/*
 * Copyright (c) 2019 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server;

import static org.glassfish.jersey.internal.guava.Preconditions.checkArgument;

import java.net.InetSocketAddress;
import java.util.EnumSet;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.opendaylight.aaa.web.WebContext;
import org.opendaylight.aaa.web.WebContextRegistration;
import org.opendaylight.aaa.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows user to build jetty server with WebServer interface support.
 */
public class LightyWebServerAdapter extends LightyServerBuilder implements WebServer {
    private static final Logger LOG = LoggerFactory.getLogger(LightyWebServerAdapter.class);
    private static final int HTTP_SERVER_IDLE_TIMEOUT = 30000;

    private int httpPort;
    private ServerConnector http;
    private ContextHandlerCollection contextHandlerCollection;

    public LightyWebServerAdapter(final InetSocketAddress inetSocketAddress, final int httpPort) {
        super(inetSocketAddress);
        this.httpPort = httpPort;
        serverInit(httpPort);
    }

    public LightyWebServerAdapter(final Server server, final int httpPort) {
        super(server);
        serverInit(httpPort);
    }

    private void serverInit(final int port) {
        checkArgument(port >= 0, "httpPort must be positive");
        checkArgument(port < 65536, "httpPort must < 65536");
        this.contextHandlerCollection = new ContextHandlerCollection();
        if (this.server == null) {
            this.server = new Server(this.inetSocketAddress);
        }
        this.contexts.forEach((contextHandler) -> {
            if (true) {
                addFilters(contextHandler);
            }
            contextHandlerCollection.addHandler(contextHandler);
        });
        this.http = new ServerConnector(server);
        this.http.setHost("localhost");
        this.http.setPort(port);
        this.http.setIdleTimeout(HTTP_SERVER_IDLE_TIMEOUT);
        this.server.addConnector(http);
    }

    @Override
    public Server build() {
        return this.server;
    }

    @Override
    public WebContextRegistration registerWebContext(final WebContext webContext) {
        String contextPathWithSlashPrefix = webContext.contextPath().startsWith("/")
                ? webContext.contextPath() : "/" + webContext.contextPath();
        ServletContextHandler handler = new ServletContextHandler(contextHandlerCollection, contextPathWithSlashPrefix,
                webContext.supportsSessions() ? ServletContextHandler.SESSIONS : ServletContextHandler.NO_SESSIONS);

        // The order in which we do things here must be the same as
        // the equivalent in org.opendaylight.aaa.web.osgi.PaxWebServer

        // 1. Context parameters - because listeners, filters and servlets could need them
        webContext.contextParams().entrySet().forEach(entry -> handler.setAttribute(entry.getKey(), entry.getValue()));
        // also handler.getServletContext().setAttribute(name, value), both seem work

        // 2. Listeners - because they could set up things that filters and servlets need
        webContext.listeners().forEach(listener -> handler.addEventListener(listener));

        // 3. Filters - because subsequent servlets should already be covered by the filters
        webContext.filters().forEach(filter -> {
            FilterHolder filterHolder = new FilterHolder(filter.filter());
            filterHolder.setInitParameters(filter.initParams());
            filter.urlPatterns().forEach(
                urlPattern -> handler.addFilter(filterHolder, urlPattern, EnumSet.allOf(DispatcherType.class))
            );
        });
        webContext.servlets().forEach(servlet -> {
            ServletHolder servletHolder = new ServletHolder(servlet.name(), servlet.servlet());
            servletHolder.setInitParameters(servlet.initParams());
            servletHolder.setInitOrder(1); // AKA <load-on-startup> 1
            servlet.urlPatterns().forEach(urlPattern -> handler.addServlet(servletHolder, urlPattern));
        });
        addFilters(handler);
        this.contextHandlerCollection.addHandler(handler);
        return () -> close(handler);
    }

    @Override
    public String getBaseURL() {
        if (httpPort == 0) {
            throw new IllegalStateException("must start() before getBaseURL()");
        }
        return "http://localhost:" + httpPort;
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private void close(final ServletContextHandler handler) {
        try {
            handler.stop();
            handler.destroy();
        } catch (Exception e) {
            LOG.error("close() failed", e);
        }
        contextHandlerCollection.removeHandler(handler);
    }

    @PostConstruct
    @SuppressWarnings("checkstyle:IllegalThrows") // Jetty WebAppContext.getUnavailableException() throws Throwable
    public void start() throws Throwable {
        server.start();
        this.httpPort = http.getLocalPort();
        LOG.info("Started Jetty-based HTTP web server on port {} ({}).", httpPort, hashCode());
    }

    @PreDestroy
    public void stop() throws Exception {
        LOG.info("Stopping Jetty-based web server...");
        // NB server.stop() will call stop() on all ServletContextHandler/WebAppContext
        server.stop();
        LOG.info("Stopped Jetty-based web server.");
    }
}
