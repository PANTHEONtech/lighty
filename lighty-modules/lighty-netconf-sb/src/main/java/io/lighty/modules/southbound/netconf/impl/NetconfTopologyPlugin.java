/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.modules.southbound.netconf.impl.util.NetconfUtils;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.netconf.client.NetconfClientDispatcher;
import org.opendaylight.netconf.topology.api.SchemaRepositoryProvider;
import org.opendaylight.netconf.topology.impl.NetconfTopologyImpl;
import org.opendaylight.netconf.topology.impl.SchemaRepositoryProviderImpl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class NetconfTopologyPlugin extends AbstractLightyModule implements NetconfSBPlugin {

    private final NetconfTopologyImpl topology;
    private final DOMMountPointService domMountPointService;

    NetconfTopologyPlugin(final LightyServices lightyServices, final String topologyId,
            final NetconfClientDispatcher clientDispatcher, final ExecutorService executorService,
            final AAAEncryptionService encryptionService) {
        super(executorService);
        this.domMountPointService = lightyServices.getDOMMountPointService();
        final SchemaRepositoryProvider schemaRepositoryProvider =
                new SchemaRepositoryProviderImpl("shared-schema-repository-impl");
        this.topology = new NetconfTopologyImpl(topologyId, clientDispatcher,
                lightyServices.getEventExecutor(), lightyServices.getScheduledThreaPool(),
                lightyServices.getThreadPool(), schemaRepositoryProvider,
                lightyServices.getBindingDataBroker(), lightyServices.getDOMMountPointService(),
                encryptionService, new LightyDeviceActionFactory());
    }

    @Override
    protected boolean initProcedure() {
        this.topology.init();
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        return true;
    }

    @Override
    public boolean isClustered() {
        return false;
    }

    @Override
    public Optional<NetconfBaseService> getNetconfBaseService(final NodeId nodeId) {
        final Optional<DOMMountPoint> domMountPointOptional = getNetconfDOMMountPoint(nodeId);
        if (domMountPointOptional.isPresent()) {
            final DOMMountPoint domMountPoint = domMountPointOptional.get();
            final Optional<DOMRpcService> domRpcServiceOptional = domMountPoint.getService(DOMRpcService.class);
            if (domRpcServiceOptional.isPresent()) {
                return Optional.of(new NetconfBaseServiceImpl(nodeId, domRpcServiceOptional.get(), domMountPoint.getSchemaContext()));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<NetconfNmdaBaseService> getNetconfNmdaBaseService(NodeId nodeId) {
        final Optional<DOMMountPoint> domMountPointOptional = getNetconfDOMMountPoint(nodeId);
        if (domMountPointOptional.isPresent()) {
            final DOMMountPoint domMountPoint = domMountPointOptional.get();
            final Optional<DOMRpcService> domRpcServiceOptional = domMountPoint.getService(DOMRpcService.class);
            if (domRpcServiceOptional.isPresent()) {
                return Optional.of(new NetconfNmdaBaseServiceImpl(nodeId, domRpcServiceOptional.get(), domMountPoint.getSchemaContext()));
            }
        }
        return Optional.empty();
    }

    private Optional<DOMMountPoint> getNetconfDOMMountPoint(NodeId nodeId) {
        final YangInstanceIdentifier yangInstanceIdentifier = NetconfUtils.createNetConfNodeMountPointYII(nodeId);
        return this.domMountPointService.getMountPoint(yangInstanceIdentifier);
    }
}
