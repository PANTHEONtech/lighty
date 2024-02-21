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
import org.opendaylight.netconf.client.mdsal.DeviceActionFactoryImpl;
import org.opendaylight.netconf.client.mdsal.api.CredentialProvider;
import org.opendaylight.netconf.client.mdsal.api.SchemaResourceManager;
import org.opendaylight.netconf.client.mdsal.api.SslHandlerFactoryProvider;
import org.opendaylight.netconf.client.mdsal.impl.DefaultBaseNetconfSchemas;
import org.opendaylight.netconf.client.mdsal.impl.DefaultCredentialProvider;
import org.opendaylight.netconf.client.mdsal.impl.DefaultSchemaResourceManager;
import org.opendaylight.netconf.client.mdsal.impl.DefaultSslHandlerFactoryProvider;
import org.opendaylight.netconf.topology.singleton.impl.NetconfTopologyManager;
import org.opendaylight.netconf.topology.spi.DefaultNetconfClientConfigurationBuilderFactory;
import org.opendaylight.netconf.topology.spi.NetconfClientConfigurationBuilderFactory;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfClusteredTopologyPlugin extends AbstractTopologyPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfClusteredTopologyPlugin.class);

    private final LightyServices lightyServices;
    private final AAAEncryptionService encryptionService;

    private NetconfTopologyManager topology;

    public NetconfClusteredTopologyPlugin(final LightyServices lightyServices,
            final ExecutorService executorService, final AAAEncryptionService encryptionService) {
        super(executorService, lightyServices.getDOMMountPointService());
        this.lightyServices = lightyServices;
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
        final SchemaResourceManager schemaResourceManager
                = new DefaultSchemaResourceManager(lightyServices.getYangParserFactory());
        final CredentialProvider credentialProvider
                = new DefaultCredentialProvider(lightyServices.getBindingDataBroker());
        final SslHandlerFactoryProvider factoryProvider
            = new DefaultSslHandlerFactoryProvider(lightyServices.getBindingDataBroker());
        final NetconfClientConfigurationBuilderFactory factory = new DefaultNetconfClientConfigurationBuilderFactory(
            encryptionService, credentialProvider, factoryProvider);
        topology = new NetconfTopologyManager(defaultBaseNetconfSchemas,
                lightyServices.getBindingDataBroker(),
                lightyServices.getClusterSingletonServiceProvider(), lightyServices.getScheduledThreadPool(),
                lightyServices.getThreadPool(), lightyServices.getActorSystemProvider(),
                lightyServices.getEventExecutor(), clientDispatcher,
                lightyServices.getDOMMountPointService(), encryptionService, lightyServices.getRpcProviderService(),
                new DeviceActionFactoryImpl(), schemaResourceManager, factory);
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
