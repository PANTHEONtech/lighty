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
import io.lighty.modules.northbound.restconf.community.impl.root.resource.discovery.RootFoundApplication;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.server.LightyServerBuilder;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;
import javax.ws.rs.core.Application;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.restconf.nb.jaxrs.JaxRsRestconf;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.JsonNormalizedNodeBodyWriter;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.JsonPatchStatusBodyWriter;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.XmlNormalizedNodeBodyWriter;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.XmlPatchStatusBodyWriter;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.errors.RestconfDocumentedExceptionMapper;
import org.opendaylight.restconf.nb.rfc8040.streams.StreamsConfiguration;
import org.opendaylight.restconf.server.mdsal.MdsalDatabindProvider;
import org.opendaylight.restconf.server.mdsal.MdsalRestconfServer;
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
    private final int httpPort;
    private final InetAddress inetAddress;
    private final String restconfServletContextPath;
    private Server jettyServer;
    private LightyServerBuilder lightyServerBuilder;

    public CommunityRestConf(final DOMDataBroker domDataBroker, final DOMRpcService domRpcService,
            final DOMActionService domActionService, final DOMNotificationService domNotificationService,
            final DOMMountPointService domMountPointService,
            final DOMSchemaService domSchemaService, final InetAddress inetAddress,
            final int httpPort, final String restconfServletContextPath,
            final LightyServerBuilder serverBuilder) {
        this.domDataBroker = domDataBroker;
        this.domRpcService = domRpcService;
        this.domActionService = domActionService;
        this.domNotificationService = domNotificationService;
        this.domMountPointService = domMountPointService;
        this.lightyServerBuilder = serverBuilder;
        this.domSchemaService = domSchemaService;
        this.httpPort = httpPort;
        this.inetAddress = inetAddress;
        this.restconfServletContextPath = restconfServletContextPath;
    }

    public CommunityRestConf(final DOMDataBroker domDataBroker,
            final DOMRpcService domRpcService, final DOMActionService domActionService,
            final DOMNotificationService domNotificationService, final DOMMountPointService domMountPointService,
            final DOMSchemaService domSchemaService, final InetAddress inetAddress, final int httpPort,
            final String restconfServletContextPath) {
        this(domDataBroker, domRpcService, domActionService, domNotificationService,
                domMountPointService, domSchemaService, inetAddress, httpPort,
                restconfServletContextPath, null);
    }

    @Override
    protected boolean initProcedure() {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final StreamsConfiguration streamsConfiguration = RestConfConfigUtils.getStreamsConfiguration();

        LOG.info("Starting RestconfApplication with configuration {}", streamsConfiguration);

        final MdsalDatabindProvider databindProvider = new MdsalDatabindProvider(domSchemaService);
        final var server = new MdsalRestconfServer(databindProvider, domDataBroker, domRpcService, domActionService,
                domMountPointService);

        final ServletContainer servletContainer8040 = new ServletContainer(ResourceConfig
                .forApplication(new Application() {
                    @Override
                    public Set<Class<?>> getClasses() {
                        return Set.of(
                                JsonNormalizedNodeBodyWriter.class, XmlNormalizedNodeBodyWriter.class,
                                JsonPatchStatusBodyWriter.class, XmlPatchStatusBodyWriter.class);
                    }

                    @Override
                    public Set<Object> getSingletons() {
                        return Set.of(
                                new RestconfDocumentedExceptionMapper(databindProvider),
                                new JaxRsRestconf(server));
                    }
                }));

        final ServletHolder jaxrs = new ServletHolder(servletContainer8040);

        LOG.info("RestConf init complete, starting Jetty");
        LOG.info("http address:port {}:{}, url prefix: {}", this.inetAddress.toString(), this.httpPort,
                this.restconfServletContextPath);

        try {
            final InetSocketAddress inetSocketAddress = new InetSocketAddress(this.inetAddress, this.httpPort);
            final ContextHandlerCollection contexts = new ContextHandlerCollection();
            final ServletContextHandler mainHandler =
                    new ServletContextHandler(contexts, this.restconfServletContextPath, true, false);
            mainHandler.addServlet(jaxrs, "/*");

            final ServletContextHandler rrdHandler =
                    new ServletContextHandler(contexts, "/.well-known", true, false);
            final RootFoundApplication rootDiscoveryApp = new RootFoundApplication(restconfServletContextPath);
            rrdHandler.addServlet(new ServletHolder(new ServletContainer(ResourceConfig
                    .forApplication(rootDiscoveryApp))), "/*");

            boolean startDefault = false;
            if (this.lightyServerBuilder == null) {
                this.lightyServerBuilder = new LightyServerBuilder(inetSocketAddress);
                startDefault = true;
            }
            this.lightyServerBuilder.addContextHandler(contexts);
            if (startDefault) {
                startServer();
            }
        } catch (final IllegalStateException e) {
            LOG.error("Failed to start jetty: ", e);
        }
        LOG.info("Lighty RestConf started in {}", stopwatch.stop());
        return true;
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    @Override
    protected boolean stopProcedure() {
        boolean stopFailed = false;
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
        if (this.jettyServer != null && !this.jettyServer.isStopped()) {
            return;
        }
        try {
            this.jettyServer = this.lightyServerBuilder.build();
            this.jettyServer.start();
        } catch (final Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new IllegalStateException("Failed to start jetty!", e);
        }
        LOG.info("Jetty started");
    }

}
