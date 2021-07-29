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
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.netconf.client.NetconfClientDispatcher;
import org.opendaylight.netconf.sal.connect.api.SchemaResourceManager;
import org.opendaylight.netconf.sal.connect.impl.DefaultSchemaResourceManager;
import org.opendaylight.netconf.sal.connect.netconf.schema.mapping.DefaultBaseNetconfSchemas;
import org.opendaylight.netconf.topology.impl.NetconfTopologyImpl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfTopologyPlugin extends AbstractLightyModule implements NetconfSBPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfTopologyPlugin.class);

    private final DOMMountPointService domMountPointService;
    private final String topologyId;
    private final NetconfClientDispatcher clientDispatcher;
    private NetconfTopologyImpl netconfTopologyImpl;
    private final AAAEncryptionService encryptionService;
    private final LightyServices lightyServices;

    NetconfTopologyPlugin(final LightyServices lightyServices, final String topologyId,
            final NetconfClientDispatcher clientDispatcher, final ExecutorService executorService,
            final AAAEncryptionService encryptionService) {
        super(executorService);
        this.domMountPointService = lightyServices.getDOMMountPointService();
        this.lightyServices = lightyServices;
        this.topologyId = topologyId;
        this.clientDispatcher = clientDispatcher;
        this.encryptionService = encryptionService;
    }

    @Override
    protected boolean initProcedure() {
        final DefaultBaseNetconfSchemas defaultBaseNetconfSchemas;
        try {
            defaultBaseNetconfSchemas = new DefaultBaseNetconfSchemas(lightyServices.getYangParserFactory());
        } catch (YangParserException e) {
            LOG.error("Failed to create DefaultBaseNetconfSchema, cause: ", e);
            return false;
        }
        final SchemaResourceManager schemaResourceManager =
                new DefaultSchemaResourceManager(lightyServices.getYangParserFactory());
        netconfTopologyImpl = new NetconfTopologyImpl(topologyId, clientDispatcher,
                lightyServices.getEventExecutor(), lightyServices.getScheduledThreadPool(),
                lightyServices.getThreadPool(), schemaResourceManager,
                lightyServices.getBindingDataBroker(), lightyServices.getDOMMountPointService(),
                encryptionService, lightyServices.getRpcProviderService(),
                defaultBaseNetconfSchemas, new LightyDeviceActionFactory());
        netconfTopologyImpl.init();
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        if (netconfTopologyImpl != null) {
            netconfTopologyImpl.close();
        }
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
            Optional<DOMSchemaService> service = domMountPoint.getService(DOMSchemaService.class);
            final Optional<DOMRpcService> domRpcServiceOptional = domMountPoint.getService(DOMRpcService.class);
            if (domRpcServiceOptional.isPresent()) {
                return Optional.of(new NetconfBaseServiceImpl(nodeId, domRpcServiceOptional.get(),
                        service.orElseThrow().getGlobalContext()));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<NetconfNmdaBaseService> getNetconfNmdaBaseService(NodeId nodeId) {
        final Optional<DOMMountPoint> domMountPointOptional = getNetconfDOMMountPoint(nodeId);
        if (domMountPointOptional.isPresent()) {
            final DOMMountPoint domMountPoint = domMountPointOptional.get();
            Optional<DOMSchemaService> service = domMountPoint.getService(DOMSchemaService.class);
            final Optional<DOMRpcService> domRpcServiceOptional = domMountPoint.getService(DOMRpcService.class);
            if (domRpcServiceOptional.isPresent()) {
                return Optional.of(new NetconfNmdaBaseServiceImpl(nodeId, domRpcServiceOptional.get(),
                        service.orElseThrow().getGlobalContext()));
            }
        }
        return Optional.empty();
    }

    private Optional<DOMMountPoint> getNetconfDOMMountPoint(NodeId nodeId) {
        final YangInstanceIdentifier yangInstanceIdentifier = NetconfUtils.createNetConfNodeMountPointYII(nodeId);
        return this.domMountPointService.getMountPoint(yangInstanceIdentifier);
    }
}
