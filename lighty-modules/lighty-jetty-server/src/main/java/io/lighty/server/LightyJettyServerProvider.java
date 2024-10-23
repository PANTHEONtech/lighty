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
import org.opendaylight.aaa.web.WebContext;
import org.opendaylight.aaa.web.jetty.JettyWebServer;

/**
 * Provides user jetty server.
 */
public class LightyJettyServerProvider {

    protected final InetSocketAddress inetSocketAddress;
    protected final List<WebContext> contexts;
    protected JettyWebServer server;

    /**
     * Init new jetty server on specific port and address wrapped into {@link InetSocketAddress}.
     *
     * @param inetSocketAddress - port and address of server
     */
    public LightyJettyServerProvider(final InetSocketAddress inetSocketAddress) {
        this.inetSocketAddress = inetSocketAddress;
        this.contexts = new ArrayList<>();
        this.server = new JettyWebServer(inetSocketAddress.getPort());
    }

    /**
     * Init jetty server with existing ones.
     *
     * @param server - jetty server
     */
    public LightyJettyServerProvider(final JettyWebServer server) {
        this(new InetSocketAddress(0));
        this.server = server;
    }

    /**
     * Add specific handler for server to handle incoming HTTP requests.
     *
     * @param handler - specific handler
     * @return instance of {@link LightyJettyServerProvider}
     */
    public LightyJettyServerProvider addContextHandler(final WebContext handler) {
        this.contexts.add(handler);
        return this;
    }

    /**
     * Returns jetty server.
     */
    public JettyWebServer getServer() {
        return this.server;
    }
}
