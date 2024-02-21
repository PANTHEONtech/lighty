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
import java.lang.annotation.Annotation;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.opendaylight.netconf.client.mdsal.DeviceActionFactoryImpl;
import org.opendaylight.netconf.client.mdsal.api.SchemaResourceManager;
import org.opendaylight.netconf.client.mdsal.impl.DefaultBaseNetconfSchemaProvider;
import org.opendaylight.netconf.client.mdsal.impl.DefaultSchemaResourceManager;
import org.opendaylight.netconf.common.NetconfTimer;
import org.opendaylight.netconf.common.impl.DefaultNetconfTimer;
import org.opendaylight.netconf.topology.callhome.CallHomeMountService;
import org.opendaylight.netconf.topology.callhome.CallHomeMountSshAuthProvider;
import org.opendaylight.netconf.topology.callhome.CallHomeMountStatusReporter;
import org.opendaylight.netconf.topology.callhome.CallHomeSshAuthProvider;
import org.opendaylight.netconf.topology.callhome.IetfZeroTouchCallHomeServerProvider;
import org.opendaylight.netconf.topology.spi.NetconfTopologySchemaAssembler;
import org.slf4j.LoggerFactory;

public class NetconfCallhomePlugin extends AbstractLightyModule {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(NetconfCallhomePlugin.class);

    private final IetfZeroTouchCallHomeServerProvider provider;

    public NetconfCallhomePlugin(final LightyServices lightyServices, final String topologyId,
            final ExecutorService executorService, final String adress, final int port) {
        super(executorService);
        final DefaultBaseNetconfSchemaProvider defaultBaseNetconfSchemas = new
                DefaultBaseNetconfSchemaProvider(lightyServices.getYangParserFactory());
        final SchemaResourceManager manager = new DefaultSchemaResourceManager(lightyServices.getYangParserFactory());
        final var mountStatusReporter = new CallHomeMountStatusReporter(
                lightyServices.getBindingDataBroker());
        final CallHomeSshAuthProvider authProvider = new CallHomeMountSshAuthProvider(
                lightyServices.getBindingDataBroker(), mountStatusReporter);
        final var recorder = new CallHomeMountStatusReporter(lightyServices.getBindingDataBroker());
        final NetconfTimer timer = new DefaultNetconfTimer();
        IetfZeroTouchCallHomeServerProvider.Configuration configuration = new Configuration(adress, 4334);

        final CallHomeMountService dispatcher =
                new CallHomeMountService(topologyId, timer,
                        new NetconfTopologySchemaAssembler(1, 1, 10, TimeUnit.SECONDS),
                        manager, defaultBaseNetconfSchemas, lightyServices.getBindingDataBroker(),
                        lightyServices.getDOMMountPointService(), new DeviceActionFactoryImpl());
        this.provider = new IetfZeroTouchCallHomeServerProvider(timer, dispatcher, authProvider, recorder,
                configuration);
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

    public static class Configuration implements IetfZeroTouchCallHomeServerProvider.Configuration {
        private final String host;
        private final int port;

        public Configuration(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public String host() {
            return this.host;
        }

        @Override
        public int port() {
            return this.port;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }
    }

}
