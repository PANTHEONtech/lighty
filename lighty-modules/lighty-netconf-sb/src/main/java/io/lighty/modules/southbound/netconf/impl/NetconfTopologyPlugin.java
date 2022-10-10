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
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.netconf.client.NetconfClientDispatcher;
import org.opendaylight.netconf.sal.connect.api.SchemaResourceManager;
import org.opendaylight.netconf.sal.connect.impl.DefaultSchemaResourceManager;
import org.opendaylight.netconf.sal.connect.netconf.schema.mapping.DefaultBaseNetconfSchemas;
import org.opendaylight.netconf.topology.impl.NetconfTopologyImpl;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfTopologyPlugin extends AbstractTopologyPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfTopologyPlugin.class);

    private final String topologyId;
    private final NetconfClientDispatcher clientDispatcher;
    private NetconfTopologyImpl netconfTopologyImpl;
    private final AAAEncryptionService encryptionService;
    private final LightyServices lightyServices;

    NetconfTopologyPlugin(final LightyServices lightyServices, final String topologyId,
            final NetconfClientDispatcher clientDispatcher, final ExecutorService executorService,
            final AAAEncryptionService encryptionService) {
        super(executorService, lightyServices.getDOMMountPointService());
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
}
