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
    private final NetconfTopologyImpl topology;
    private final DOMMountPointService domMountPointService;

    NetconfTopologyPlugin(final LightyServices lightyServices, final String topologyId,
            final NetconfClientDispatcher clientDispatcher, final ExecutorService executorService,
            final AAAEncryptionService encryptionService) {
        super(executorService);
        this.domMountPointService = lightyServices.getDOMMountPointService();
        DefaultBaseNetconfSchemas defaultBaseNetconfSchemas;
        try {
            defaultBaseNetconfSchemas = new DefaultBaseNetconfSchemas(lightyServices.getYangParserFactory());
        } catch (YangParserException ex) {
            LOG.error("Cannot create DefaultBaseNetconfSchemas.", ex);
            this.topology = null;
            return;
        }
        final SchemaResourceManager schemaResourceManager =
                new DefaultSchemaResourceManager(lightyServices.getYangParserFactory());
        this.topology = new NetconfTopologyImpl(topologyId, clientDispatcher,
                lightyServices.getEventExecutor(), lightyServices.getScheduledThreadPool(),
                lightyServices.getThreadPool(), schemaResourceManager,
                lightyServices.getBindingDataBroker(), lightyServices.getDOMMountPointService(),
                encryptionService, defaultBaseNetconfSchemas, new LightyDeviceActionFactory());
    }

    @Override
    protected boolean initProcedure() {
        if (topology != null) {
            this.topology.init();
            return true;
        } else {
            LOG.error("NetconfTopologyPlugin initialization failed.");
            return false;
        }
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
                Optional<DOMSchemaService> schemaService = domMountPoint.getService(DOMSchemaService.class);
                if (schemaService.isPresent()) {
                    return Optional.of(new NetconfBaseServiceImpl(nodeId, domRpcServiceOptional.get(),
                            schemaService.get().getGlobalContext()));
                }
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
                Optional<DOMSchemaService> schemaService = domMountPoint.getService(DOMSchemaService.class);
                if (schemaService.isPresent()) {
                    return Optional.of(new NetconfNmdaBaseServiceImpl(nodeId, domRpcServiceOptional.get(),
                            schemaService.get().getGlobalContext()));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<DOMMountPoint> getNetconfDOMMountPoint(NodeId nodeId) {
        final YangInstanceIdentifier yangInstanceIdentifier = NetconfUtils.createNetConfNodeMountPointYII(nodeId);
        return this.domMountPointService.getMountPoint(yangInstanceIdentifier);
    }
}
