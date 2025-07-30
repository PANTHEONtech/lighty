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
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;

public final class HttpsLightyJettyWebServer extends AbstractLightyWebServer {

    public HttpsLightyJettyWebServer(final SecurityConfig securityConfig) {
        // automatically choose free port
        this(new InetSocketAddress("localhost", 0), securityConfig);
    }

    public HttpsLightyJettyWebServer(final InetSocketAddress address, final SecurityConfig securityConfig) {
        super(address.getPort());
        final SslConnectionFactory ssl = securityConfig.getSslConnectionFactory(HttpVersion.HTTP_1_1.asString());

        // add the HTTPS connector
        final ServerConnector http = new ServerConnector(server, ssl, createHttpsConnectionFactory(address));
        http.setPort(address.getPort());
        http.setHost(address.getHostName());
        http.setIdleTimeout(HTTP_SERVER_IDLE_TIMEOUT);

        server.addConnector(http);
    }

    private HttpConnectionFactory createHttpsConnectionFactory(final InetSocketAddress inetSocketAddress) {
        final HttpConfiguration baseConfig = new HttpConfiguration();
        baseConfig.setSecurePort(inetSocketAddress.getPort());
        baseConfig.setSendXPoweredBy(true);

        final HttpConfiguration httpsConfig = new HttpConfiguration(baseConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        return new HttpConnectionFactory(httpsConfig);
    }
}
