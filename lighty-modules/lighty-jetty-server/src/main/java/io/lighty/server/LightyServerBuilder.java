/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import org.opendaylight.aaa.web.WebContext;
import org.opendaylight.aaa.web.jetty.JettyWebServer;

/**
 * Allows user to build jetty server.
 */
public class LightyServerBuilder {

    protected final InetSocketAddress inetSocketAddress;
    protected final List<WebContext> contexts;
    protected JettyWebServer server;

    /**
     * Init new jetty server on specific port and address wrapped into {@link InetSocketAddress}.
     *
     * @param inetSocketAddress - port and address of server
     */
    public LightyServerBuilder(final InetSocketAddress inetSocketAddress) {
        this.inetSocketAddress = inetSocketAddress;
        this.contexts = new ArrayList<>();
    }

    /**
     * Init jetty server with existing ones.
     *
     * @param server - jetty server
     */
    public LightyServerBuilder(final JettyWebServer server) {
        this(new InetSocketAddress(0));
        this.server = server;
    }

    /**
     * Add specific handler for server to handle incoming HTTP requests.
     *
     * @param handler - specific handler
     * @return instance of {@link LightyServerBuilder}
     */
    public LightyServerBuilder addContextHandler(final WebContext handler) {
        this.contexts.add(handler);
        return this;
    }

    /**
     * Build jetty server with specific settings (filters, init params, event listeners, handlers).
     *
     * @return instance of jetty server
     */
    public JettyWebServer build() {
        if (this.server == null) {
            this.server = new JettyWebServer(this.inetSocketAddress.getPort());
        }
        this.contexts.forEach((contextHandler) -> {
            try {
                this.server.registerWebContext(contextHandler);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
        });
        return this.server;
    }
}
