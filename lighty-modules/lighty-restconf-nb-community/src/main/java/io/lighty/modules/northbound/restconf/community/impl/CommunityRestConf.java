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
import io.lighty.server.AbstractLightyWebServer;
import io.lighty.server.LightyJettyServerProvider;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import javax.servlet.ServletException;
import org.opendaylight.aaa.filterchain.configuration.impl.CustomFilterAdapterConfigurationImpl;
import org.opendaylight.aaa.web.WebContext;
import org.opendaylight.aaa.web.WebContextSecurer;
import org.opendaylight.aaa.web.servlet.jersey2.JerseyServletSupport;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
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
    private final DOMNotificationService domNotificationService;
    private final DOMMountPointService domMountPointService;
    private final DOMActionService domActionService;
    private final DOMSchemaService domSchemaService;
    private final InetAddress inetAddress;
    private final String restconfServletContextPath;
    private final int httpPort;
    private AbstractLightyWebServer jettyServer;
    private LightyJettyServerProvider lightyServerBuilder;
    private JaxRsEndpoint jaxRsEndpoint;
    private WebContextSecurer webContextSecurer;

    public CommunityRestConf(final DOMDataBroker domDataBroker, final DOMRpcService domRpcService,
            final DOMNotificationService domNotificationService,
            final DOMActionService domActionService,
            final DOMMountPointService domMountPointService,
            final DOMSchemaService domSchemaService,
            final InetAddress inetAddress,
            final int httpPort,
            final String restconfServletContextPath,
            final LightyJettyServerProvider serverBuilder,
            final WebContextSecurer webContextSecurer) {
        this.domDataBroker = domDataBroker;
        this.domRpcService = domRpcService;
        this.domNotificationService = domNotificationService;
        this.domActionService = domActionService;
        this.domMountPointService = domMountPointService;
        this.lightyServerBuilder = serverBuilder;
        this.domSchemaService = domSchemaService;
        this.inetAddress = inetAddress;
        this.httpPort = httpPort;
        this.restconfServletContextPath = restconfServletContextPath;
        this.webContextSecurer = (webContextSecurer == null) ? new LightyWebContextSecurer() : webContextSecurer;
    }

    public CommunityRestConf(final DOMDataBroker domDataBroker, final DOMRpcService domRpcService,
            final DOMNotificationService domNotificationService, final DOMActionService domActionService,
            final DOMMountPointService domMountPointService, final DOMSchemaService domSchemaService,
            final InetAddress inetAddress, final int httpPort,
            final String restconfServletContextPath, final WebContextSecurer webContextSecurer) {
        this(domDataBroker, domRpcService, domNotificationService, domActionService,
                domMountPointService, domSchemaService, inetAddress, httpPort,
                restconfServletContextPath,null, webContextSecurer);
    }

    @Override
    protected boolean initProcedure() throws ServletException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final JaxRsEndpointConfiguration streamsConfiguration =
            RestConfConfigUtils.getStreamsConfiguration(restconfServletContextPath);

        LOG.info("Starting RestconfApplication with configuration {}", streamsConfiguration);

        if (lightyServerBuilder == null) {
            lightyServerBuilder = new LightyJettyServerProvider(new InetSocketAddress(inetAddress, httpPort));
        }

        final MdsalDatabindProvider databindProvider = new MdsalDatabindProvider(domSchemaService);
        final var server = new MdsalRestconfServer(databindProvider, domDataBroker, domRpcService,
            domActionService, domMountPointService);

        this.jettyServer = this.lightyServerBuilder.getServer();
        this.jaxRsEndpoint = new JaxRsEndpoint(
            jettyServer,
            this.webContextSecurer,
            new JerseyServletSupport(),
            new CustomFilterAdapterConfigurationImpl(),
            server,
            new MdsalRestconfStreamRegistry(domDataBroker, domNotificationService, domSchemaService,
                new JaxRsLocationProvider(), databindProvider),
            JaxRsEndpoint.props(streamsConfiguration)
        );

        LOG.info("Lighty RestConf started in {}", stopwatch.stop());
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
        if (this.jettyServer != null) {
            try {
                this.jettyServer.stop();
                LOG.info("Jetty stopped");
            } catch (final Exception e) {
                LOG.error("{} failed to stop!", this.jettyServer.getClass(), e);
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
            this.jettyServer.start();
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
            //do nothing since shiro is not used
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
