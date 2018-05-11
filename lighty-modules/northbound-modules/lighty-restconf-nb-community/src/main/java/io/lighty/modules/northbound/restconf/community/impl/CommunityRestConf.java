/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.modules.northbound.restconf.community.impl;

import com.sun.jersey.spi.container.servlet.ServletContainer;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.modules.northbound.restconf.community.impl.config.JsonRestConfServiceType;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.netconf.sal.restconf.impl.RestconfProviderImpl;
import org.opendaylight.restconf.nb.rfc8040.RestConnectorProvider;
import org.opendaylight.restconf.nb.rfc8040.services.wrapper.ServicesWrapperImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.slf4j.LoggerFactory;

public class CommunityRestConf extends AbstractLightyModule {

    private static final String JAVAX_WS_RS_APPLICATION = "javax.ws.rs.Application";
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CommunityRestConf.class);

    private final DOMDataBroker domDataBroker;
    private final SchemaService schemaService;
    private final DOMRpcService domRpcService;
    private final DOMNotificationService domNotificationService;
    private final DOMMountPointService domMountPointService;
    private final PortNumber webSocketPort;
    private final JsonRestConfServiceType jsonRestconfServiceType;

    private RestconfProviderImpl restconfProvider;
    private RestConnectorProvider restConnectorProvider;
    private final DOMSchemaService domSchemaService;
    private final int httpPort;
    private final InetAddress inetAddress;
    private final String restconfServletContextPath;
    private Server jettyServer;

    public CommunityRestConf(final DOMDataBroker domDataBroker, final SchemaService schemaService,
            final DOMRpcService domRpcService, final DOMNotificationService domNotificationService,
            final DOMMountPointService domMountPointService, final int webSocketPort,
            final JsonRestConfServiceType jsonRestconfServiceType, final DOMSchemaService domSchemaService,
            final InetAddress inetAddress, final int httpPort, final String restconfServletContextPath,
            final ExecutorService executorService) {
        this.domDataBroker = domDataBroker;
        this.schemaService = schemaService;
        this.domRpcService = domRpcService;
        this.domNotificationService = domNotificationService;
        this.domMountPointService = domMountPointService;
        this.webSocketPort = new PortNumber(webSocketPort);
        this.jsonRestconfServiceType = jsonRestconfServiceType;
        this.domSchemaService = domSchemaService;
        this.httpPort = httpPort;
        this.inetAddress = inetAddress;
        this.restconfServletContextPath = restconfServletContextPath;

        /* In standard Oxygen ODL Restconf blueprints there are exposed two additional services:
           - jsonRestconfServiceDraft02 (for bierman implementation)
           - jsonRestconfService (for RFC8040 implementation)
           Those services are not expected to be required thus they are not here.
         */
    }

    @Override
    protected boolean initProcedure() {
        final long startTime = System.nanoTime();
        LOG.info("Starting RestConfProvider websocket port: {}", webSocketPort);
        restconfProvider =
                new RestconfProviderImpl(domDataBroker, schemaService, domRpcService, domNotificationService,
                        domMountPointService, domSchemaService,
                        IpAddressBuilder.getDefaultInstance(inetAddress.getHostAddress()), webSocketPort);
        restconfProvider.start();

        LOG.info("Starting RestConnectorProvider");
        restConnectorProvider =
                new RestConnectorProvider<>(domDataBroker, schemaService, domRpcService,
                        domNotificationService, domMountPointService, ServicesWrapperImpl.getInstance());
        restConnectorProvider.start();

        final ServletHolder jaxrs = new ServletHolder(ServletContainer.class);

        LOG.info("Starting jsonRestconfService {}", jsonRestconfServiceType.name());
        switch (jsonRestconfServiceType) {
            case DRAFT_02:
                jaxrs.setInitParameter(JAVAX_WS_RS_APPLICATION,
                        "org.opendaylight.netconf.sal.rest.impl.RestconfApplication");
                break;
            case DRAFT_18:
                jaxrs.setInitParameter(JAVAX_WS_RS_APPLICATION, "org.opendaylight.restconf.nb.rfc8040.RestconfApplication");
                break;
            default:
                throw new UnsupportedOperationException("unsupported restconf service type: " + jsonRestconfServiceType.name());
        }
        LOG.info("RestConf init complete, starting Jetty");
        LOG.info("http address:port {}:{}, url prefix: {}", inetAddress.toString(), httpPort,
                restconfServletContextPath);

        //Start jetty server here and wire up
        try {
            final InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, httpPort);
            jettyServer = new Server(inetSocketAddress);
            final ContextHandlerCollection contexts = new ContextHandlerCollection();
            jettyServer.setHandler(contexts);
            final ServletContextHandler mainHandler =
                    new ServletContextHandler(contexts, restconfServletContextPath, true, false);
            mainHandler.addServlet(jaxrs, "/*");
            jettyServer.start();
        } catch (final Exception e) {
            LOG.error("Failed to start jetty: ", e);
        }
        LOG.info("Jetty started");
        final float delay = (System.nanoTime() - startTime) / 1_000_000f;
        LOG.info("Lighty RestConf started in {}ms", delay);
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        boolean stopFailed = false;
        if (restConnectorProvider != null) {
            try {
                restConnectorProvider.close();
                LOG.info("RestConnectorProvider closed");
            } catch (Exception e) {
                LOG.error("{} failed to close!", restConnectorProvider.getClass(), e);
                stopFailed = true;
            }
        }
        if (restconfProvider != null) {
            restconfProvider.close();
            LOG.info("RestconfProvider closed");
        }
        if (jettyServer != null) {
            try {
                jettyServer.stop();
                LOG.info("Jetty stopped");
            } catch (Exception e) {
                LOG.error("{} failed to stop!", jettyServer.getClass(), e);
                stopFailed = true;
            }
        }
        return !stopFailed;
    }

}
