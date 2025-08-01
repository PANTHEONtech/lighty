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
import org.opendaylight.netconf.common.di.DefaultNetconfTimer;
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

    NetconfTopologyPlugin(final LightyServices lightyServices, final String topologyId,
            final ExecutorService executorService, final AAAEncryptionService encryptionService) {
        super(executorService, lightyServices.getDOMMountPointService());
        this.lightyServices = lightyServices;
        this.topologyId = topologyId;
        this.encryptionService = encryptionService;
    }

    @Override
    protected boolean initProcedure() {
        final BaseNetconfSchemaProvider defaultBaseNetconfSchemas;
        defaultBaseNetconfSchemas = new DefaultBaseNetconfSchemaProvider(lightyServices.getYangParserFactory());
        final NetconfKeystoreService service = new DefaultNetconfKeystoreService(
                lightyServices.getBindingDataBroker(), lightyServices.getRpcProviderService(),
                lightyServices.getClusterSingletonServiceProvider(), encryptionService);
        final NetconfClientFactory netconfFactory = new NetconfClientFactoryImpl(new DefaultNetconfTimer());
        final CredentialProvider credentialProvider = new DefaultCredentialProvider(service);
        final SslContextFactoryProvider factoryProvider = new DefaultSslContextFactoryProvider(service);
        final NetconfClientConfigurationBuilderFactory factory = new NetconfClientConfigurationBuilderFactoryImpl(
            encryptionService, credentialProvider, factoryProvider);
        final NetconfTopologySchemaAssembler assembler = new NetconfTopologySchemaAssembler(1);
        final SchemaResourceManager schemaResourceManager =
                new DefaultSchemaResourceManager(lightyServices.getYangParserFactory());
        netconfTopologyImpl = new NetconfTopologyImpl(topologyId, netconfFactory, new DefaultNetconfTimer(), assembler,
                schemaResourceManager, lightyServices.getBindingDataBroker(), lightyServices.getDOMMountPointService(),
                encryptionService, factory, lightyServices.getRpcProviderService(),
                defaultBaseNetconfSchemas, new LightyDeviceActionFactory());
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
