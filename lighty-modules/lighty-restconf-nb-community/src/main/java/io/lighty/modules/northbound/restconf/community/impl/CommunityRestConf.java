/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.restconf.community.impl;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.server.LightyServerBuilder;
import javax.servlet.ServletException;
import org.opendaylight.aaa.filterchain.configuration.impl.CustomFilterAdapterConfigurationImpl;
import org.opendaylight.aaa.web.WebContext;
import org.opendaylight.aaa.web.WebContextSecurer;
import org.opendaylight.aaa.web.jetty.JettyWebServer;
import org.opendaylight.aaa.web.servlet.jersey2.JerseyServletSupport;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.restconf.server.jaxrs.JaxRsEndpoint;
import org.opendaylight.restconf.server.jaxrs.JaxRsEndpointConfiguration;
import org.opendaylight.restconf.server.jaxrs.JaxRsLocationProvider;
import org.opendaylight.restconf.server.mdsal.MdsalDatabindProvider;
import org.opendaylight.restconf.server.mdsal.MdsalRestconfServer;
import org.opendaylight.restconf.server.mdsal.MdsalRestconfStreamRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommunityRestConf extends AbstractLightyModule {
    private static final Logger LOG = LoggerFactory.getLogger(CommunityRestConf.class);

    private final DOMDataBroker domDataBroker;
    private final DOMRpcService domRpcService;
    private final DOMMountPointService domMountPointService;
    private final DOMActionService domActionService;
    private final DOMSchemaService domSchemaService;
    private final int httpPort;
    private LightyServerBuilder lightyServerBuilder;
    private JaxRsEndpoint jaxRsEndpoint;
    private JettyWebServer server;

    public CommunityRestConf(final DOMDataBroker domDataBroker, final DOMRpcService domRpcService,
            final DOMActionService domActionService,
            final DOMMountPointService domMountPointService,
            final DOMSchemaService domSchemaService,
            final int httpPort,
            final LightyServerBuilder serverBuilder) {
        this.domDataBroker = domDataBroker;
        this.domRpcService = domRpcService;
        this.domActionService = domActionService;
        this.domMountPointService = domMountPointService;
        this.lightyServerBuilder = serverBuilder;
        this.domSchemaService = domSchemaService;
        this.httpPort = httpPort;
    }

    public CommunityRestConf(final DOMDataBroker domDataBroker,
            final DOMRpcService domRpcService, final DOMActionService domActionService,
        final DOMMountPointService domMountPointService,
            final DOMSchemaService domSchemaService, final int httpPort) {
        this(domDataBroker, domRpcService, domActionService,
                domMountPointService, domSchemaService, httpPort, null);
    }

    @Override
    protected boolean initProcedure() throws ServletException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final JaxRsEndpointConfiguration streamsConfiguration = RestConfConfigUtils.getStreamsConfiguration();

        LOG.info("Starting RestconfApplication with configuration {}", streamsConfiguration);

        final MdsalDatabindProvider databindProvider = new MdsalDatabindProvider(domSchemaService);
        final var mdsalserver = new MdsalRestconfServer(databindProvider, domDataBroker, domRpcService, domActionService,
            domMountPointService);

        server = new JettyWebServer(httpPort);
        this.jaxRsEndpoint = new JaxRsEndpoint(server, new LightyWebContextSecurer(),
            new JerseyServletSupport(), new CustomFilterAdapterConfigurationImpl(), mdsalserver,
            new MdsalRestconfStreamRegistry(new JaxRsLocationProvider(), domDataBroker),
            JaxRsEndpoint.props(streamsConfiguration));
        return true;
    }


    @SuppressWarnings("checkstyle:illegalCatch")
    @Override
    protected boolean stopProcedure() {
        boolean stopFailed = false;
        if (this.jaxRsEndpoint != null) {
            try {
                this.jaxRsEndpoint.close();
                LOG.info("jaxRsEndpoint stopped");
            } catch (final Exception e) {
                LOG.error("{} failed to stop!", this.jaxRsEndpoint.getClass(), e);
                stopFailed = true;
            }
        }
        if (this.lightyServerBuilder != null) {
            this.lightyServerBuilder = null;
        }
        return !stopFailed;
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    public void startServer() {
        try {
            this.server.start();
        } catch (final Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new IllegalStateException("Failed to start jetty!", e);
        }
        LOG.info("Jetty started");
    }

    public JaxRsEndpoint getJaxRsEndpoint() {
        return this.jaxRsEndpoint;
    }

    public static class LightyWebContextSecurer implements WebContextSecurer {
        @Override
        public void requireAuthentication(WebContext.Builder webContextBuilder,
            boolean asyncSupported, String... urlPatterns) {

        }

        @Override
        public void requireAuthentication(WebContext.Builder webContextBuilder, String... urlPatterns) {
            WebContextSecurer.super.requireAuthentication(webContextBuilder, urlPatterns);
        }

        @Override
        public void requireAuthentication(WebContext.Builder webContextBuilder) {
            WebContextSecurer.super.requireAuthentication(webContextBuilder);
        }
    }

}
