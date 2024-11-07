/*
   Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.

   This program and the accompanying materials are made available under the
   terms of the Eclipse Public License v1.0 which accompanies this distribution,
   and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server;

import io.lighty.server.config.SecurityConfig;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.opendaylight.aaa.web.jetty.JettyWebServer;

public class Http2LightyServerBuilder extends LightyServerBuilder {
    private final SecurityConfig securityConfig;

    public Http2LightyServerBuilder(final InetSocketAddress inetSocketAddress, final SecurityConfig config) {
        super(inetSocketAddress);
        this.securityConfig = config;
    }

    @Override
    public JettyWebServer build() {
        if (super.server == null) {
            super.server = new JettyWebServer(this.inetSocketAddress.getPort());
        }
        super.build();

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
        final var ssl = securityConfig.getSslConnectionFactory(alpn.getProtocol());

        // HTTP/2 Connector
        final var sslConnector = new ServerConnector(
            jettyServer, ssl, alpn, h2, new HttpConnectionFactory(httpsConfig));
        sslConnector.setPort(this.inetSocketAddress.getPort());
        jettyServer.addConnector(sslConnector);

        return super.server;
    }
}