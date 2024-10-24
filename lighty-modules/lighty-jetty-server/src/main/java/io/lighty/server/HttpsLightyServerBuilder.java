/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server;

import io.lighty.server.config.SecurityConfig;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.opendaylight.aaa.web.jetty.JettyWebServer;

public class HttpsLightyServerBuilder extends LightyServerBuilder {
    private final SecurityConfig securityConfig;

    public HttpsLightyServerBuilder(final InetSocketAddress inetSocketAddress, final SecurityConfig securityConfig) {
        super(inetSocketAddress);
        this.securityConfig = securityConfig;
    }

    @Override
    public JettyWebServer build() {
        if (super.server == null) {
            super.server = new JettyWebServer(this.inetSocketAddress.getPort());
        }
        final SslConnectionFactory ssl = securityConfig.getSslConnectionFactory(HttpVersion.HTTP_1_1.asString());

        Server jettyServer;
        try {
            // Use AccessController.doPrivileged to allow access to the private field
            Field serverField = AccessController.doPrivileged((PrivilegedAction<Field>) () -> {
                try {
                    Field field = JettyWebServer.class.getDeclaredField("server");
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Field not found", e);
                }
            });

            jettyServer = (Server) serverField.get(server);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set handler on JettyWebServer", e);
        }

        final ServerConnector sslConnector = new ServerConnector(jettyServer,
                ssl, httpConfiguration(this.inetSocketAddress));
        sslConnector.setPort(this.inetSocketAddress.getPort());

        jettyServer.addConnector(sslConnector);
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
