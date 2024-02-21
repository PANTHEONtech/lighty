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
import java.util.concurrent.TimeUnit;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.netconf.client.NetconfClientFactory;
import org.opendaylight.netconf.client.NetconfClientFactoryImpl;
import org.opendaylight.netconf.client.mdsal.DeviceActionFactoryImpl;
import org.opendaylight.netconf.client.mdsal.api.CredentialProvider;
import org.opendaylight.netconf.client.mdsal.api.SchemaResourceManager;
import org.opendaylight.netconf.client.mdsal.api.SslContextFactoryProvider;
import org.opendaylight.netconf.client.mdsal.impl.DefaultBaseNetconfSchemaProvider;
import org.opendaylight.netconf.client.mdsal.impl.DefaultCredentialProvider;
import org.opendaylight.netconf.client.mdsal.impl.DefaultSchemaResourceManager;
import org.opendaylight.netconf.client.mdsal.impl.DefaultSslContextFactoryProvider;
import org.opendaylight.netconf.common.impl.DefaultNetconfTimer;
import org.opendaylight.netconf.keystore.legacy.impl.DefaultNetconfKeystoreService;
import org.opendaylight.netconf.topology.singleton.impl.NetconfTopologyManager;
import org.opendaylight.netconf.topology.spi.NetconfClientConfigurationBuilderFactory;
import org.opendaylight.netconf.topology.spi.NetconfClientConfigurationBuilderFactoryImpl;
import org.opendaylight.netconf.topology.spi.NetconfTopologySchemaAssembler;
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
        final var timer =  new DefaultNetconfTimer();
        final var defaultBaseNetconfSchemas =
                new DefaultBaseNetconfSchemaProvider(lightyServices.getYangParserFactory());
        final SchemaResourceManager schemaResourceManager
                = new DefaultSchemaResourceManager(lightyServices.getYangParserFactory());
        final var keystoreService = new DefaultNetconfKeystoreService(lightyServices.getBindingDataBroker(),
                lightyServices.getRpcProviderService(), lightyServices.getClusterSingletonServiceProvider(),
                encryptionService);
        final CredentialProvider credentialProvider
                = new DefaultCredentialProvider(keystoreService);
        final NetconfClientFactory netconfClientFactory = new NetconfClientFactoryImpl(timer);
        final SslContextFactoryProvider factoryProvider
            = new DefaultSslContextFactoryProvider(keystoreService);
        final NetconfClientConfigurationBuilderFactory factory = new NetconfClientConfigurationBuilderFactoryImpl(
                encryptionService, credentialProvider, factoryProvider);
        topology = new NetconfTopologyManager(defaultBaseNetconfSchemas,
                lightyServices.getBindingDataBroker(),
                lightyServices.getClusterSingletonServiceProvider(), new DefaultNetconfTimer(),
                new NetconfTopologySchemaAssembler(1, 1, 0, TimeUnit.SECONDS), lightyServices.getActorSystemProvider(),
                netconfClientFactory, lightyServices.getDOMMountPointService(), encryptionService,
                lightyServices.getRpcProviderService(), new DeviceActionFactoryImpl(), schemaResourceManager, factory);
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
