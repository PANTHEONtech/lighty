/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.provider;

import io.lighty.gnmi.southbound.device.connection.DeviceConnectionInitializer;
import io.lighty.gnmi.southbound.device.connection.DeviceConnectionManager;
import io.lighty.gnmi.southbound.device.session.security.GnmiSecurityProvider;
import io.lighty.gnmi.southbound.device.session.security.KeystoreGnmiSecurityProvider;
import io.lighty.gnmi.southbound.identifier.IdentifierUtils;
import io.lighty.gnmi.southbound.listener.GnmiNodeListener;
import io.lighty.gnmi.southbound.mountpoint.GnmiMountPointRegistrator;
import io.lighty.gnmi.southbound.mountpoint.broker.GnmiDataBrokerFactoryImpl;
import io.lighty.gnmi.southbound.schema.SchemaContextHolder;
import io.lighty.gnmi.southbound.schema.certstore.impl.CertificationStorageServiceImpl;
import io.lighty.gnmi.southbound.schema.certstore.rpc.CertificationStorageServiceRpcImpl;
import io.lighty.gnmi.southbound.schema.impl.SchemaContextHolderImpl;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoaderService;
import io.lighty.gnmi.southbound.schema.yangstore.impl.YangDataStoreServiceImpl;
import io.lighty.gnmi.southbound.schema.yangstore.rpc.YangStorageServiceRpcImpl;
import io.lighty.gnmi.southbound.schema.yangstore.service.YangDataStoreService;
import io.lighty.gnmi.southbound.timeout.TimeoutUtils;
import io.lighty.modules.gnmi.connector.gnmi.session.impl.GnmiSessionFactory;
import io.lighty.modules.gnmi.connector.gnmi.session.impl.GnmiSessionFactoryImpl;
import io.lighty.modules.gnmi.connector.session.SessionManagerFactoryImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiTopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides gNMI southbound.
 */
public class GnmiSouthboundProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiSouthboundProvider.class);

    private final List<AutoCloseable> closeables;
    private final DOMMountPointService mountPointService;
    private final DataBroker dataBroker;
    private final RpcProviderService rpcProvider;
    private final ExecutorService gnmiExecutorService;
    private final AAAEncryptionService encryptionService;
    /**
     * Optional list of YangLoaderService which loads initial yang models to datastore.
     * These models are then used to construct schema context for gNMI devices based on gNMI.CapabilityResponse.
     */
    private final List<YangLoaderService> initialYangsLoaders;
    /**
     * Optional custom yang reactor used for parsing provided yang models.
     */
    private final CrossSourceStatementReactor yangReactor;

    public GnmiSouthboundProvider(final DOMMountPointService mountService, final DataBroker dataBroker,
                                  final RpcProviderService rpcProvider, final ExecutorService gnmiExecutorService,
                                  final List<YangLoaderService> initialYangsLoaders,
                                  final AAAEncryptionService encryptionService,
                                  final @Nullable CrossSourceStatementReactor yangReactor) {
        this.mountPointService = mountService;
        this.dataBroker = dataBroker;
        this.gnmiExecutorService = gnmiExecutorService;
        this.closeables = new ArrayList<>();
        this.rpcProvider = rpcProvider;
        this.initialYangsLoaders = initialYangsLoaders;
        this.encryptionService = encryptionService;
        this.yangReactor = yangReactor;
    }

    public void init() throws ExecutionException, InterruptedException, TimeoutException, YangLoadException {
        LOG.info("gNMI init started");
        //----Load initial yang models to datastore and register yang load rpc----
        final YangDataStoreService yangDataStoreService = new YangDataStoreServiceImpl(dataBroker, gnmiExecutorService);
        final YangStorageServiceRpcImpl yangStorageServiceRpc = new YangStorageServiceRpcImpl(yangDataStoreService);
        closeables.add(rpcProvider.registerRpcImplementations(yangStorageServiceRpc.getRpcClassToInstanceMap()));

        final CertificationStorageServiceImpl certStorageService
                = new CertificationStorageServiceImpl(encryptionService, dataBroker);
        final CertificationStorageServiceRpcImpl certStorageServiceRpc
                = new CertificationStorageServiceRpcImpl(certStorageService);
        closeables.add(rpcProvider
                .registerRpcImplementations(certStorageServiceRpc.getRpcClassToInstanceMap()));

        // Load initial Yang models
        if (!initialYangsLoaders.isEmpty()) {
            LOG.info("Loading provided initial yang loaders");
            for (YangLoaderService loaderService : initialYangsLoaders) {
                loaderService.load(yangDataStoreService);
            }
        }

        //----Start and wire up core components----
        final SchemaContextHolder schemaContextHolder =
                new SchemaContextHolderImpl(yangDataStoreService,yangReactor);
        final GnmiMountPointRegistrator mountPointRegistrator = new GnmiMountPointRegistrator(mountPointService);
        closeables.add(mountPointRegistrator);

        final GnmiSecurityProvider securityProvider = new KeystoreGnmiSecurityProvider(certStorageService);
        final GnmiSessionFactory gnmiSessionFactory = new GnmiSessionFactoryImpl();
        final DeviceConnectionInitializer deviceConnectionInitializer = new DeviceConnectionInitializer(
                securityProvider, new SessionManagerFactoryImpl(gnmiSessionFactory), dataBroker, gnmiExecutorService);
        closeables.add(deviceConnectionInitializer);

        final DeviceConnectionManager deviceConnectionManager = new DeviceConnectionManager(
                mountPointRegistrator, schemaContextHolder, new GnmiDataBrokerFactoryImpl(),
                deviceConnectionInitializer, dataBroker, gnmiExecutorService);
        closeables.add(deviceConnectionManager);

        final GnmiNodeListener topologyNodeListener = new GnmiNodeListener(
                deviceConnectionManager, dataBroker, gnmiExecutorService);

        //-----Init gNMI topology------
        initGnmiTopology();
        closeables.add(dataBroker.registerDataTreeChangeListener(IdentifierUtils.GNMI_NODE_DTI, topologyNodeListener));
        LOG.info("gNMI south-bound has successfully started");
    }

    private void initGnmiTopology() throws ExecutionException, InterruptedException, TimeoutException {
        Topology topology = new TopologyBuilder().setTopologyId(new TopologyId(IdentifierUtils.GNMI_TOPOLOGY_ID))
                .setTopologyTypes(new TopologyTypesBuilder()
                        .addAugmentation(new GnmiTopologyTypesBuilder()
                                .build())
                        .build())
                .build();
        @NonNull WriteTransaction configTx = dataBroker.newWriteOnlyTransaction();
        configTx.merge(LogicalDatastoreType.CONFIGURATION, IdentifierUtils.GNMI_TOPO_IID, topology);
        configTx.commit().get(TimeoutUtils.DATASTORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        @NonNull WriteTransaction operTx = dataBroker.newWriteOnlyTransaction();
        operTx.merge(LogicalDatastoreType.OPERATIONAL, IdentifierUtils.GNMI_TOPO_IID, topology);
        operTx.commit().get(TimeoutUtils.DATASTORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws Exception {
        for (AutoCloseable closable : closeables) {
            closable.close();
        }
    }
}
