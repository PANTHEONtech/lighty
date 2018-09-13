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

import java.util.Optional;
import java.util.concurrent.ExecutorService;

import io.lighty.modules.southbound.netconf.impl.util.NetconfUtils;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
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
            final NetconfClientDispatcher clientDispatcher, ExecutorService executorService,
            final AAAEncryptionService encryptionService) {
        super(executorService);
        this.domMountPointService = lightyServices.getDOMMountPointService();
        final SchemaRepositoryProvider schemaRepositoryProvider =
                new SchemaRepositoryProviderImpl("shared-schema-repository-impl");
        topology = new NetconfTopologyImpl(topologyId, clientDispatcher,
                lightyServices.getEventExecutor(), lightyServices.getScheduledThreaPool(),
                lightyServices.getThreadPool(), schemaRepositoryProvider,
                lightyServices.getBindingDataBroker(), lightyServices.getDOMMountPointService(),
                encryptionService);
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
        return false;
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
