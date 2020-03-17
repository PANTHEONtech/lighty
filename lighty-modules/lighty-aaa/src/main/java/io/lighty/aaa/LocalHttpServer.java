/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa;

import io.lighty.server.LightyServerBuilder;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Servlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

final class LocalHttpServer {
    private final LightyServerBuilder server;
    private final Map<String, Handler> handlers;

    LocalHttpServer(final LightyServerBuilder server) {
        this.server = server;
        this.handlers = new HashMap<>();
    }

    @SuppressWarnings("rawtypes")
    public void registerServlet(final String alias, final Servlet servlet, final Dictionary initparams) {
        final ServletHolder servletHolder = new ServletHolder(servlet);
        final ContextHandlerCollection contexts = new ContextHandlerCollection();
        final ServletContextHandler mainHandler = new ServletContextHandler(contexts, alias, true, false);
        mainHandler.addServlet(servletHolder, "/*");
        this.server.addContextHandler(contexts);
        this.handlers.put(alias, contexts);
    }

    public void unregister(final String alias) {
        this.handlers.get(alias).destroy();
    }
}
