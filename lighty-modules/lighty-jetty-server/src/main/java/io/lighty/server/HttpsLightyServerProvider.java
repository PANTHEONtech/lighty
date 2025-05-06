/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server;

import io.lighty.server.config.SecurityConfig;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpsLightyServerProvider extends LightyJettyServerProvider {
    private static final Logger LOG = LoggerFactory.getLogger(HttpsLightyServerProvider.class);

    public HttpsLightyServerProvider(final InetSocketAddress inetSocketAddress,
        final SecurityConfig securityConfig) {
        super(inetSocketAddress); // Let the superclass handle server creation
        final SslConnectionFactory ssl = securityConfig.getSslConnectionFactory(HttpVersion.HTTP_1_1.asString());

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

        // Add only the HTTPS connector
        final ServerConnector sslConnector = new ServerConnector(jettyServer,
            ssl, httpConfiguration(this.inetSocketAddress));
        sslConnector.setPort(this.inetSocketAddress.getPort());

        jettyServer.addConnector(sslConnector);
    }

    @Override
    public LightyJettyWebServer getServer() {
        return server;
    }

    private HttpConnectionFactory httpConfiguration(final InetSocketAddress inetSocketAddress) {
        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecurePort(inetSocketAddress.getPort());
        httpConfig.setSendXPoweredBy(true);

        final HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());
        return new HttpConnectionFactory(httpsConfig);
    }
}

