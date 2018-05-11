/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.modules.southbound.netconf.impl;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

import io.lighty.modules.southbound.netconf.impl.util.NetconfUtils;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.netconf.client.NetconfClientDispatcher;
import org.opendaylight.netconf.topology.singleton.impl.NetconfTopologyManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.topology.singleton.config.rev170419.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.topology.singleton.config.rev170419.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class NetconfClusteredTopologyPlugin extends AbstractLightyModule implements NetconfSBPlugin {

    private final NetconfTopologyManager topology;
    private final DOMMountPointService domMountPointService;

    public NetconfClusteredTopologyPlugin(final LightyServices lightyServices, final String topologyId,
            final NetconfClientDispatcher clientDispatcher, final Integer writeTxIdleTimeout,
            final ExecutorService executorService, final AAAEncryptionService encryptionService) {
        super(executorService);
        this.domMountPointService = lightyServices.getDOMMountPointService();
        final Config config = new ConfigBuilder()
                .setWriteTransactionIdleTimeout(writeTxIdleTimeout)
                .build();
        this.topology = new NetconfTopologyManager(lightyServices.getBindingDataBroker(),
                lightyServices.getRpcProviderRegistry(), lightyServices.getClusterSingletonServiceProvider(),
                lightyServices.getScheduledThreaPool(), lightyServices.getThreadPool(),
                lightyServices.getActorSystemProvider(), lightyServices.getEventExecutor(), clientDispatcher,
                topologyId, config, lightyServices.getDOMMountPointService(), encryptionService);
    }

    @Override
    protected boolean initProcedure() {
        topology.init();
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        return true;
    }

    @Override
    public boolean isClustered() {
        return true;
    }

    @Override
    public Optional<NetconfBaseService> getNetconfBaseService(NodeId nodeId) {
        YangInstanceIdentifier yangInstanceIdentifier = NetconfUtils.createNetConfNodeMountPointYII(nodeId);
        com.google.common.base.Optional<DOMMountPoint> mountPoint = domMountPointService.getMountPoint(yangInstanceIdentifier);
        if (mountPoint.isPresent()) {
            DOMMountPoint domMountPoint = mountPoint.get();
            com.google.common.base.Optional<DOMRpcService> optionalDOMMountPoint = domMountPoint.getService(DOMRpcService.class);
            if (optionalDOMMountPoint.isPresent()) {
                return Optional.of(new NetconfBaseServiceImpl(nodeId, optionalDOMMountPoint.get(), domMountPoint.getSchemaContext()));
            }
        }
        return Optional.empty();
    }

}
