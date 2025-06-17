/*
   Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.

   This program and the accompanying materials are made available under the
   terms of the Eclipse Public License v1.0 which accompanies this distribution,
   and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server;

import io.lighty.server.config.SecurityConfig;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http2LightyServerProvider extends LightyJettyServerProvider {
    private static final Logger LOG = LoggerFactory.getLogger(Http2LightyServerProvider.class);

    public Http2LightyServerProvider(final InetSocketAddress inetSocketAddress, final SecurityConfig config) {
        super(inetSocketAddress);
        final var jettyServer = server.getServer();

        // Clear connectors added by superclass
        for (Connector connector : jettyServer.getConnectors()) {
            try {
                connector.shutdown().get(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.warn("Failed to stop existing connector", e);
            }
            jettyServer.removeConnector(connector);
        }

        // HTTPS Configuration
        final var httpsConfig = new HttpConfiguration();
        httpsConfig.setSecureScheme(HttpScheme.HTTPS.asString());
        httpsConfig.setSecurePort(this.inetSocketAddress.getPort());
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
        final var sslConnector = new ServerConnector(
            jettyServer, ssl, alpn, h2, new HttpConnectionFactory(httpsConfig));
        sslConnector.setPort(this.inetSocketAddress.getPort());
        jettyServer.addConnector(sslConnector);
    }

    @Override
    public LightyJettyWebServer getServer() {
        return server;
    }
}