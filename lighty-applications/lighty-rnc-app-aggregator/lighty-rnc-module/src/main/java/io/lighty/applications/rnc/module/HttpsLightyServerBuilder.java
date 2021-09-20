/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module;

import io.lighty.applications.rnc.module.config.SecurityConfig;
import io.lighty.server.LightyServerBuilder;
import java.net.InetSocketAddress;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;

public class HttpsLightyServerBuilder extends LightyServerBuilder {
    private final SecurityConfig securityConfig;

    public HttpsLightyServerBuilder(InetSocketAddress inetSocketAddress, SecurityConfig securityConfig) {
        super(inetSocketAddress);
        this.server = new Server();
        this.securityConfig = securityConfig;
    }

    @Override
    public Server build() {
        final Server server = super.build();
        final SslConnectionFactory ssl = securityConfig.getSslConnectionFactory(HttpVersion.HTTP_1_1.asString());
        final ServerConnector sslConnector = new ServerConnector(server,
                ssl, httpConfiguration(this.inetSocketAddress));
        sslConnector.setPort(this.inetSocketAddress.getPort());

        server.addConnector(sslConnector);
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
