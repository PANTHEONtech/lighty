/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;
import java.util.concurrent.ExecutorService;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.netconf.callhome.mount.CallHomeMountDispatcher;
import org.opendaylight.netconf.callhome.mount.IetfZeroTouchCallHomeServerProvider;
import org.opendaylight.netconf.client.mdsal.api.CredentialProvider;
import org.opendaylight.netconf.client.mdsal.api.SchemaResourceManager;
import org.opendaylight.netconf.client.mdsal.api.SslHandlerFactoryProvider;
import org.opendaylight.netconf.client.mdsal.impl.DefaultBaseNetconfSchemaProvider;
import org.opendaylight.netconf.client.mdsal.impl.DefaultCredentialProvider;
import org.opendaylight.netconf.client.mdsal.impl.DefaultSchemaResourceManager;
import org.opendaylight.netconf.client.mdsal.impl.DefaultSslHandlerFactoryProvider;
import org.opendaylight.netconf.topology.spi.DefaultNetconfClientConfigurationBuilderFactory;
import org.opendaylight.netconf.topology.spi.NetconfClientConfigurationBuilderFactory;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;
import org.slf4j.LoggerFactory;

public class NetconfCallhomePlugin extends AbstractLightyModule {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(NetconfCallhomePlugin.class);

    private final IetfZeroTouchCallHomeServerProvider provider;

    public NetconfCallhomePlugin(final LightyServices lightyServices, final String topologyId,
            final ExecutorService executorService, final AAAEncryptionService encryptionService) {
        super(executorService);
        final DefaultBaseNetconfSchemaProvider defaultBaseNetconfSchemas = new
                DefaultBaseNetconfSchemaProvider(lightyServices.getYangParserFactory());
        final SchemaResourceManager schemaResourceManager =
                new DefaultSchemaResourceManager(lightyServices.getYangParserFactory());
        final CredentialProvider credentialProvider =
            new DefaultCredentialProvider(lightyServices.getBindingDataBroker());
        final SslHandlerFactoryProvider factoryProvider =
            new DefaultSslHandlerFactoryProvider(lightyServices.getBindingDataBroker());
        final NetconfClientConfigurationBuilderFactory factory = new DefaultNetconfClientConfigurationBuilderFactory(
            encryptionService, credentialProvider, factoryProvider);
        final CallHomeMountDispatcher dispatcher =
                new CallHomeMountDispatcher(topologyId, lightyServices.getEventExecutor(),
                        lightyServices.getScheduledThreadPool(), lightyServices.getThreadPool(),
                        schemaResourceManager, defaultBaseNetconfSchemas, lightyServices.getBindingDataBroker(),
                        lightyServices.getDOMMountPointService(), factory);
        this.provider = new IetfZeroTouchCallHomeServerProvider(lightyServices.getBindingDataBroker(), dispatcher);
    }

    @Override
    protected boolean initProcedure() {
        return true;
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    @Override
    protected boolean stopProcedure() {
        try {
            this.provider.close();
        } catch (final Exception e) {
            LOG.error("{} failed to close!", this.provider.getClass(), e);
            return false;
        }
        return true;
    }

}
