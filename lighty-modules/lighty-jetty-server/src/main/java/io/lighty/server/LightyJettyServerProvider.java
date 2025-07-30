/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server;

import io.lighty.server.config.LightyServerConfig;
import java.net.InetSocketAddress;

/**
 * Provides user jetty server.
 */
public class LightyJettyServerProvider {

    private final AbstractLightyWebServer server;

    /**
     * Init new jetty server on specific port and address wrapped into {@link InetSocketAddress}.
     *
     * @param inetSocketAddress - port and address of server
     */
    public LightyJettyServerProvider(final InetSocketAddress inetSocketAddress) {
        this.server = new HttpLightyJettyWebServer(inetSocketAddress);
    }

    /**
     * Create server with desired security.
     *
     * @param config - get config option for the security (http/https/http2)
     */
    public LightyJettyServerProvider(final LightyServerConfig config, final InetSocketAddress inetSocketAddress) {
        if (config.isUseHttp2()) {
            this.server = new Http2LightyJettyWebServer(inetSocketAddress, config.getSecurityConfig());
        } else if (config.isUseHttps()) {
            this.server = new HttpsLightyJettyWebServer(inetSocketAddress, config.getSecurityConfig());
        } else {
            this.server = new HttpLightyJettyWebServer(inetSocketAddress);
        }
    }

    /**
     * Returns jetty server.
     */
    public AbstractLightyWebServer getServer() {
        return this.server;
    }
}
