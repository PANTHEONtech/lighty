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
import org.opendaylight.netconf.sal.connect.netconf.DeviceActionFactoryImpl;
import org.opendaylight.netconf.sal.connect.netconf.schema.mapping.DefaultBaseNetconfSchemas;
import org.opendaylight.netconf.topology.singleton.impl.NetconfTopologyManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.topology.singleton.config.rev170419.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.topology.singleton.config.rev170419.ConfigBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfClusteredTopologyPlugin extends AbstractTopologyPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfClusteredTopologyPlugin.class);

    private final LightyServices lightyServices;
    private final String topologyId;
    private final NetconfClientDispatcher clientDispatcher;
    private final Integer writeTxIdleTimeout;
    private final AAAEncryptionService encryptionService;

    private NetconfTopologyManager topology;

    public NetconfClusteredTopologyPlugin(final LightyServices lightyServices, final String topologyId,
            final NetconfClientDispatcher clientDispatcher, final Integer writeTxIdleTimeout,
            final ExecutorService executorService, final AAAEncryptionService encryptionService) {
        super(executorService, lightyServices.getDOMMountPointService());
        this.lightyServices = lightyServices;
        this.topologyId = topologyId;
        this.clientDispatcher = clientDispatcher;
        this.writeTxIdleTimeout = writeTxIdleTimeout;
        this.encryptionService = encryptionService;
    }

    @Override
    protected boolean initProcedure() {
        final Config config = new ConfigBuilder()
                .setWriteTransactionIdleTimeout(Uint16.valueOf(writeTxIdleTimeout))
                .build();
        final DefaultBaseNetconfSchemas defaultBaseNetconfSchemas;
        try {
            defaultBaseNetconfSchemas = new DefaultBaseNetconfSchemas(lightyServices.getYangParserFactory());
        } catch (YangParserException e) {
            LOG.error("Failed to create DefaultBaseNetconfSchema, cause: ", e);
            return false;
        }
        final SchemaResourceManager schemaResourceManager
                = new DefaultSchemaResourceManager(lightyServices.getYangParserFactory());
        topology = new NetconfTopologyManager(defaultBaseNetconfSchemas,
                lightyServices.getBindingDataBroker(),
                lightyServices.getDOMRpcProviderService(), lightyServices.getDOMActionProviderService(),
                lightyServices.getClusterSingletonServiceProvider(), lightyServices.getScheduledThreadPool(),
                lightyServices.getThreadPool(), lightyServices.getActorSystemProvider(),
                lightyServices.getEventExecutor(), clientDispatcher, topologyId, config,
                lightyServices.getDOMMountPointService(), encryptionService, lightyServices.getRpcProviderService(),
                new DeviceActionFactoryImpl(), schemaResourceManager);
        topology.init();
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        if (topology != null) {
            topology.close();
        }
        return true;
    }

    @Override
    public boolean isClustered() {
        return true;
    }
}
