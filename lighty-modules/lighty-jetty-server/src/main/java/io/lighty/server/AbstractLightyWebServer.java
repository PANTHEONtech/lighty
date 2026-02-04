/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.EnumSet;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.opendaylight.aaa.web.WebContext;
import org.opendaylight.aaa.web.WebServer;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractLightyWebServer implements WebServer {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLightyWebServer.class);
    protected static final int HTTP_SERVER_IDLE_TIMEOUT = 30000;

    private final int httpPort;
    private final ContextHandlerCollection contextHandlerCollection;
    protected final Server server;

    public AbstractLightyWebServer(final int httpPort) {
        this.httpPort = httpPort;
        checkArgument(httpPort >= 0, "httpPort must be positive");
        checkArgument(httpPort < 65536, "httpPort must < 65536");

        server = new Server();
        server.setStopAtShutdown(true);

        contextHandlerCollection = new ContextHandlerCollection();
        server.setHandler(contextHandlerCollection);
    }

    @Override
    public String getBaseURL() {
        if (httpPort == 0) {
            throw new IllegalStateException("must start() before getBaseURL()");
        }
        return "http://localhost:" + httpPort;
    }

    @PostConstruct
    public void start() throws Exception {
        server.start();
        LOG.info("Started Jetty-based HTTP web server on port {} ({}).", httpPort, hashCode());
    }

    @PreDestroy
    public void stop() throws Exception {
        LOG.info("Stopping Jetty-based web server...");
        // NB server.stop() will call stop() on all ServletContextHandler/WebAppContext
        server.stop();
        LOG.info("Stopped Jetty-based web server.");
    }

    @Override
    public synchronized Registration registerWebContext(final WebContext webContext) throws ServletException {
        ServletContextHandler handler = new ServletContextHandler(contextHandlerCollection, webContext.contextPath(),
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
                urlPattern -> handler.addFilter(filterHolder, urlPattern,
                    EnumSet.allOf(DispatcherType.class))
            );
        });

        // 4. servlets - 'bout time for 'em by now, don't you think? ;)
        webContext.servlets().forEach(servlet -> {
            ServletHolder servletHolder = new ServletHolder(servlet.name(), servlet.servlet());
            servletHolder.setInitParameters(servlet.initParams());
            servletHolder.setAsyncSupported(servlet.asyncSupported());
            // AKA <load-on-startup> 1
            servletHolder.setInitOrder(1);
            servlet.urlPatterns().forEach(
                urlPattern -> handler.addServlet(servletHolder, urlPattern)
            );
        });

        // 5. Add session handler
        handler.setSessionHandler(new SessionHandler());

        restart(handler);

        return () -> close(handler);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static void restart(final AbstractLifeCycle lifecycle) throws ServletException {
        try {
            lifecycle.start();
        } catch (ServletException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException("registerServlet() start failed", e);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void close(final ServletContextHandler handler) {
        try {
            handler.stop();
        } catch (Exception e) {
            LOG.error("close() failed", e);
        } finally {
            handler.destroy();
        }
        contextHandlerCollection.removeHandler(handler);
    }

    public Server getServer() {
        return server;
    }
}
