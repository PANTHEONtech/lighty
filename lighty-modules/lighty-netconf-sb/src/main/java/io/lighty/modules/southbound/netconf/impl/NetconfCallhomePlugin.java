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
import org.opendaylight.netconf.client.mdsal.DeviceActionFactoryImpl;
import org.opendaylight.netconf.client.mdsal.api.SchemaResourceManager;
import org.opendaylight.netconf.client.mdsal.impl.DefaultBaseNetconfSchemaProvider;
import org.opendaylight.netconf.client.mdsal.impl.DefaultSchemaResourceManager;
import org.opendaylight.netconf.common.NetconfTimer;
import org.opendaylight.netconf.common.di.DefaultNetconfTimer;
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
    private final CallHomeMountService dispatcher;

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
        final CallHomeMountService.Configuration configuration = new Configuration(adress, port);

        this.dispatcher =
            new CallHomeMountService(topologyId, timer,
                new NetconfTopologySchemaAssembler(1),
                manager, defaultBaseNetconfSchemas, lightyServices.getBindingDataBroker(),
                lightyServices.getDOMMountPointService(), new DeviceActionFactoryImpl(), configuration);
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
            this.dispatcher.close();
            this.provider.close();
        } catch (final Exception e) {
            LOG.error("{} failed to close!", this.provider.getClass(), e);
            return false;
        }
        return true;
    }

    public static class Configuration implements CallHomeMountService.Configuration {

        private final String host;
        private final int port;

        public Configuration(final String host, final int port) {
            this.port = port;
            this.host = host;
        }

        @Override
        public String host() {
            return host;
        }

        @Override
        public int ssh$_$port() {
            return port;
        }

        @Override
        public int tls$_$port() {
            return port;
        }

        @Override
        public int connection$_$timeout$_$millis() {
            return 10_000;
        }

        @Override
        public int max$_$connections() {
            return 64;
        }

        @Override
        public int keep$_$alive$_$delay() {
            return 120;
        }

        @Override
        public int request$_$timeout$_$millis() {
            return 60000;
        }

        @Override
        public int min$_$backoff$_$millis() {
            return 2000;
        }

        @Override
        public int max$_$backoff$_$millis() {
            return 1800000;
        }

        @Override
        public double backoff$_$multiplier() {
            return 1.5;
        }

        @Override
        public double backoff$_$jitter() {
            return 0.1;
        }

        @Override
        public int concurrent$_$rpc$_$limit() {
            return 0;
        }

        @Override
        public int max$_$connection$_$attempts() {
            return 0;
        }

        @Override
        public boolean schemaless() {
            return false;
        }

        @Override
        public int actor$_$response$_$wait$_$time() {
            return 5;
        }

        @Override
        public boolean lock$_$datastore() {
            return true;
        }

        @Override
        public boolean reconnect$_$on$_$changed$_$schema() {
            return false;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }
    }

}
