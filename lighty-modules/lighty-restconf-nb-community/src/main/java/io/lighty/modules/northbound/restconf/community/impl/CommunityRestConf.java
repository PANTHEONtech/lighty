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
import io.lighty.modules.northbound.restconf.community.impl.config.JsonRestConfServiceType;
import io.lighty.modules.northbound.restconf.community.impl.root.resource.discovery.RootFoundApplication;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.server.LightyServerBuilder;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
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
import org.opendaylight.netconf.sal.rest.impl.RestconfApplication;
import org.opendaylight.netconf.sal.restconf.impl.BrokerFacade;
import org.opendaylight.netconf.sal.restconf.impl.ControllerContext;
import org.opendaylight.netconf.sal.restconf.impl.RestconfImpl;
import org.opendaylight.netconf.sal.restconf.impl.RestconfProviderImpl;
import org.opendaylight.netconf.sal.restconf.impl.StatisticsRestconfServiceWrapper;
import org.opendaylight.restconf.nb.rfc8040.handlers.ActionServiceHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.DOMDataBrokerHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.DOMMountPointServiceHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.NotificationServiceHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.RpcServiceHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.SchemaContextHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.TransactionChainHandler;
import org.opendaylight.restconf.nb.rfc8040.services.wrapper.ServicesWrapper;
import org.opendaylight.restconf.nb.rfc8040.streams.Configuration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommunityRestConf extends AbstractLightyModule {

    private static final Logger LOG = LoggerFactory.getLogger(CommunityRestConf.class);

    private final DOMDataBroker domDataBroker;
    private final DOMRpcService domRpcService;
    private final DOMNotificationService domNotificationService;
    private final DOMMountPointService domMountPointService;
    private final DOMActionService domActionService;
    private final PortNumber webSocketPort;
    private final JsonRestConfServiceType jsonRestconfServiceType;
    private final DOMSchemaService domSchemaService;
    private final int httpPort;
    private final InetAddress inetAddress;
    private final String restconfServletContextPath;
    private RestconfImpl restconfImpl;
    private BrokerFacade brokerFacade;
    private ControllerContext controllerContext;
    private RestconfProviderImpl restconfProvider;
    private Server jettyServer;
    private LightyServerBuilder lightyServerBuilder;

    public CommunityRestConf(final DOMDataBroker domDataBroker, final DOMSchemaService schemaService,
            final DOMRpcService domRpcService, final DOMActionService domActionService,
            final DOMNotificationService domNotificationService, final DOMMountPointService domMountPointService,
            final int webSocketPort, final JsonRestConfServiceType jsonRestconfServiceType,
            final DOMSchemaService domSchemaService, final InetAddress inetAddress, final int httpPort,
            final String restconfServletContextPath, final ExecutorService executorService,
            final LightyServerBuilder serverBuilder) {
        this.domDataBroker = domDataBroker;
        this.domRpcService = domRpcService;
        this.domActionService = domActionService;
        this.domNotificationService = domNotificationService;
        this.domMountPointService = domMountPointService;
        this.lightyServerBuilder = serverBuilder;
        this.webSocketPort = new PortNumber(Uint16.valueOf(webSocketPort));
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

    public CommunityRestConf(final DOMDataBroker domDataBroker, final DOMSchemaService schemaService,
            final DOMRpcService domRpcService, final DOMActionService domActionService,
            final DOMNotificationService domNotificationService, final DOMMountPointService domMountPointService,
            final int webSocketPort, final JsonRestConfServiceType jsonRestconfServiceType,
            final DOMSchemaService domSchemaService, final InetAddress inetAddress, final int httpPort,
            final String restconfServletContextPath, final ExecutorService executorService) {
        this(domDataBroker, schemaService, domRpcService, domActionService, domNotificationService,
            domMountPointService, webSocketPort, jsonRestconfServiceType, domSchemaService, inetAddress, httpPort,
            restconfServletContextPath, executorService, null);
    }

    @Override
    protected boolean initProcedure() {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.info("Starting RestConfProvider websocket port: {}", this.webSocketPort);
        this.controllerContext = new ControllerContext(this.domSchemaService,
                this.domMountPointService, this.domSchemaService);
        this.brokerFacade = new BrokerFacade(this.domRpcService, this.domDataBroker,
                this.domNotificationService, controllerContext);
        this.restconfImpl = new RestconfImpl(this.brokerFacade, controllerContext);
        final StatisticsRestconfServiceWrapper stats = new StatisticsRestconfServiceWrapper(this.restconfImpl);
        this.restconfProvider = new RestconfProviderImpl(stats, IpAddressBuilder.getDefaultInstance(this.inetAddress
                .getHostAddress()), this.webSocketPort);
        this.restconfProvider.start();

        LOG.info("Starting RestConnectorProvider");
        final TransactionChainHandler transactionChainHandler = new TransactionChainHandler(this.domDataBroker);
        final SchemaContextHandler schemaCtxHandler = new SchemaContextHandler(transactionChainHandler,
                this.domSchemaService);
        schemaCtxHandler.init();
        final DOMMountPointServiceHandler domMountPointServiceHandler = new DOMMountPointServiceHandler(
                this.domMountPointService);
        final DOMDataBrokerHandler domDataBrokerHandler = new DOMDataBrokerHandler(this.domDataBroker);
        final RpcServiceHandler rpcServiceHandler = new RpcServiceHandler(this.domRpcService);
        final ActionServiceHandler actionServiceHandler = new ActionServiceHandler(this.domActionService);
        final NotificationServiceHandler notificationServiceHandler = new NotificationServiceHandler(
                this.domNotificationService);
        final Configuration streamsConfiguration = RestConfConfigUtils.getStreamsConfiguration();
        final ServicesWrapper servicesWrapper = ServicesWrapper.newInstance(schemaCtxHandler,
                domMountPointServiceHandler, transactionChainHandler, domDataBrokerHandler, rpcServiceHandler,
                actionServiceHandler, notificationServiceHandler, this.domSchemaService, streamsConfiguration);

        ServletHolder jaxrs = null;

        LOG.info("Starting jsonRestconfService {}", this.jsonRestconfServiceType.name());
        switch (this.jsonRestconfServiceType) {
            case DRAFT_02:
                final Application restconfApplication = new RestconfApplication(controllerContext, stats);
                final ServletContainer servletContainer = new ServletContainer(ResourceConfig
                    .forApplication(restconfApplication));
                jaxrs = new ServletHolder(servletContainer);
                break;
            case DRAFT_18:
                final Application restconfApplication8040 =
                    new org.opendaylight.restconf.nb.rfc8040.RestconfApplication(schemaCtxHandler,
                    domMountPointServiceHandler, servicesWrapper);
                final ServletContainer servletContainer8040 = new ServletContainer(ResourceConfig
                    .forApplication(restconfApplication8040));
                jaxrs = new ServletHolder(servletContainer8040);
                break;
            default:
                throw new UnsupportedOperationException("unsupported restconf service type: "
                    + this.jsonRestconfServiceType.name());
        }
        LOG.info("RestConf init complete, starting Jetty");
        LOG.info("http address:port {}:{}, url prefix: {}", this.inetAddress.toString(), this.httpPort,
                this.restconfServletContextPath);

        try {
            final InetSocketAddress inetSocketAddress = new InetSocketAddress(this.inetAddress, this.httpPort);
            final ContextHandlerCollection contexts = new ContextHandlerCollection();
            final ServletContextHandler mainHandler =
                    new ServletContextHandler(contexts, this.restconfServletContextPath, true, false);
            mainHandler.addServlet(jaxrs, "/*");

            // FIXME resource registering should be supported also for other resources, not only for RESTCONF 8040
            switch (this.jsonRestconfServiceType) {
                case DRAFT_18:
                    final ServletContextHandler rrdHandler =
                            new ServletContextHandler(contexts, "/.well-known", true, false);
                    final RootFoundApplication rootDiscoveryApp = new RootFoundApplication(restconfServletContextPath);
                    rrdHandler.addServlet(new ServletHolder(new ServletContainer(ResourceConfig
                            .forApplication(rootDiscoveryApp))), "/*");
                    break;
                default:
                    LOG.info("Resource Discovery skipped for RESTCONF service type {}", jsonRestconfServiceType);
            }

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
        if (this.restconfProvider != null) {
            this.restconfProvider.close();
            LOG.info("RestconfProvider closed");
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

    public RestconfImpl getRestconfImpl() {
        return restconfImpl;
    }

    public BrokerFacade getBrokerFacade() {
        return brokerFacade;
    }

    public ControllerContext getControllerContext() {
        return controllerContext;
    }

}
