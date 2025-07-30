/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server;

import io.lighty.server.config.SecurityConfig;
import java.net.InetSocketAddress;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;

public final class Http2LightyJettyWebServer extends AbstractLightyWebServer {

    public Http2LightyJettyWebServer(final SecurityConfig config) {
        // automatically choose free port
        this(new InetSocketAddress("localhost", 0), config);
    }

    public Http2LightyJettyWebServer(final InetSocketAddress address, final SecurityConfig config) {
        super(address.getPort());

        // HTTPS Configuration
        final var httpsConfig = new HttpConfiguration();
        httpsConfig.setSecureScheme(HttpScheme.HTTPS.asString());
        httpsConfig.setSecurePort(address.getPort());
        httpsConfig.setSendXPoweredBy(true);
        httpsConfig.setSendServerVersion(true);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        // HTTP/2 Connection Factory
        final var h2 = new HTTP2ServerConnectionFactory(httpsConfig);
        final var alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol(h2.getProtocol());

        // SSL Connection Factory
        final var ssl = config.getSslConnectionFactory(alpn.getProtocol());

        // HTTP/2 Connector
        final ServerConnector http = new ServerConnector(
            server, ssl, alpn, h2, new HttpConnectionFactory(httpsConfig));
        http.setHost(address.getHostName());
        http.setPort(address.getPort());
        http.setIdleTimeout(HTTP_SERVER_IDLE_TIMEOUT);

        server.addConnector(http);
    }

}
