/*
 * Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.modules.southbound.netconf.impl.util.NetconfUtils;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

abstract class AbstractTopologyPlugin extends AbstractLightyModule implements NetconfSBPlugin {

    private final DOMMountPointService domMountPointService;

    AbstractTopologyPlugin(ExecutorService executorService, DOMMountPointService domMountPointService) {
        super(executorService);
        this.domMountPointService = domMountPointService;
    }

    @Override
    public Optional<NetconfBaseService> getNetconfBaseService(NodeId nodeId) {
        var mountPoint = getNetconfDOMMountPoint(nodeId);
        var schemaService = mountPoint.flatMap(t -> t.getService(DOMSchemaService.class));
        var rpcService = mountPoint.flatMap(t -> t.getService(DOMRpcService.class));

        return rpcService.map(t -> new NetconfBaseServiceImpl(nodeId, t, schemaService
                .orElseThrow().getGlobalContext()));
    }

    @Override
    public Optional<NetconfNmdaBaseService> getNetconfNmdaBaseService(NodeId nodeId) {
        var mountPoint = getNetconfDOMMountPoint(nodeId);
        var schemaService = mountPoint.flatMap(t -> t.getService(DOMSchemaService.class));
        var rpcService = mountPoint.flatMap(t -> t.getService(DOMRpcService.class));
        return rpcService.map(t -> new NetconfNmdaBaseServiceImpl(nodeId, t, schemaService
                .orElseThrow().getGlobalContext()));
    }

    private Optional<DOMMountPoint> getNetconfDOMMountPoint(NodeId nodeId) {
        var instanceIdentifier = NetconfUtils.createNetConfNodeMountPointYII(nodeId);
        return domMountPointService.getMountPoint(instanceIdentifier);
    }
}
