/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl;

import io.lighty.core.controller.api.LightyServices;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.netconf.client.NetconfClientFactory;
import org.opendaylight.netconf.client.NetconfClientFactoryImpl;
import org.opendaylight.netconf.client.mdsal.api.BaseNetconfSchemaProvider;
import org.opendaylight.netconf.client.mdsal.api.CredentialProvider;
import org.opendaylight.netconf.client.mdsal.api.SchemaResourceManager;
import org.opendaylight.netconf.client.mdsal.api.SslContextFactoryProvider;
import org.opendaylight.netconf.client.mdsal.impl.DefaultBaseNetconfSchemaProvider;
import org.opendaylight.netconf.client.mdsal.impl.DefaultCredentialProvider;
import org.opendaylight.netconf.client.mdsal.impl.DefaultSchemaResourceManager;
import org.opendaylight.netconf.client.mdsal.impl.DefaultSslContextFactoryProvider;
import org.opendaylight.netconf.common.impl.DefaultNetconfTimer;
import org.opendaylight.netconf.keystore.legacy.NetconfKeystoreService;
import org.opendaylight.netconf.keystore.legacy.impl.DefaultNetconfKeystoreService;
import org.opendaylight.netconf.topology.impl.NetconfTopologyImpl;
import org.opendaylight.netconf.topology.spi.NetconfClientConfigurationBuilderFactory;
import org.opendaylight.netconf.topology.spi.NetconfClientConfigurationBuilderFactoryImpl;
import org.opendaylight.netconf.topology.spi.NetconfTopologySchemaAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfTopologyPlugin extends AbstractTopologyPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfTopologyPlugin.class);

    private final String topologyId;
    private NetconfTopologyImpl netconfTopologyImpl;
    private final AAAEncryptionService encryptionService;
    private final LightyServices lightyServices;
    private final NetconfTopologySchemaAssembler assembler;
    private final DefaultNetconfTimer timer;

    NetconfTopologyPlugin(final LightyServices lightyServices, final String topologyId,
            final ExecutorService executorService, final AAAEncryptionService encryptionService) {
        super(executorService, lightyServices.getDOMMountPointService());
        this.lightyServices = lightyServices;
        this.topologyId = topologyId;
        this.encryptionService = encryptionService;
        assembler = new NetconfTopologySchemaAssembler(1, 1, 10, TimeUnit.SECONDS);
        timer = new DefaultNetconfTimer();
    }

    @Override
    protected boolean initProcedure() {
        final BaseNetconfSchemaProvider defaultBaseNetconfSchemas;
        defaultBaseNetconfSchemas = new DefaultBaseNetconfSchemaProvider(lightyServices.getYangParserFactory());
        final NetconfKeystoreService service = new DefaultNetconfKeystoreService(
                lightyServices.getBindingDataBroker(), lightyServices.getRpcProviderService(),
                lightyServices.getClusterSingletonServiceProvider(), encryptionService);
        final NetconfClientFactory netconfFactory = new NetconfClientFactoryImpl(timer);
        final CredentialProvider credentialProvider = new DefaultCredentialProvider(service);
        final SslContextFactoryProvider factoryProvider = new DefaultSslContextFactoryProvider(service);
        final NetconfClientConfigurationBuilderFactory factory = new NetconfClientConfigurationBuilderFactoryImpl(
            encryptionService, credentialProvider, factoryProvider);
        final SchemaResourceManager schemaResourceManager =
                new DefaultSchemaResourceManager(lightyServices.getYangParserFactory());
        netconfTopologyImpl = new NetconfTopologyImpl(topologyId, netconfFactory, timer, assembler,
                schemaResourceManager, lightyServices.getBindingDataBroker(), lightyServices.getDOMMountPointService(),
                encryptionService, factory, lightyServices.getRpcProviderService(),
                defaultBaseNetconfSchemas, new LightyDeviceActionFactory());
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        boolean success = true;
        success &= closeResource(netconfTopologyImpl);
        success &= closeResource(assembler);
        success &= closeResource(timer);
        return success;
    }

    @Override
    public boolean isClustered() {
        return false;
    }

    @SuppressWarnings({"checkstyle:illegalCatch"})
    private static boolean closeResource(final AutoCloseable resource) {
        if (resource == null) {
            return true;
        }
        try {
            resource.close();
            return true;
        } catch (Exception e) {
            LOG.error("{} failed to close!", resource.getClass().getName(), e);
            return false;
        }
    }
}
