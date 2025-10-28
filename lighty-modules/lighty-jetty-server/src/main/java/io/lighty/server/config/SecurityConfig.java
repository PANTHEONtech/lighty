/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server.config;

import java.security.KeyStore;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory.Server;

public class SecurityConfig {
    private final KeyStore keyStore;
    private final KeyStore trustKeyStore;
    private final String ksPassword;
    private final String trustKsPassword;
    private final Server server;
    private final boolean isNeedClientAuth;

    public SecurityConfig(final KeyStore keyStore, final String ksPassword, final KeyStore trustKeyStore,
                          final String trustKsPassword, final boolean isNeedClientAuth) {
        this.keyStore = keyStore;
        this.ksPassword = ksPassword;
        this.trustKeyStore = trustKeyStore;
        this.trustKsPassword = trustKsPassword;
        this.isNeedClientAuth = isNeedClientAuth;
        server = new Server();
        initFactoryCtx();
    }

    private void initFactoryCtx() {
        server.setTrustStore(trustKeyStore);
        server.setTrustStorePassword(trustKsPassword);
        server.setKeyStore(keyStore);
        server.setKeyStorePassword(ksPassword);
        server.setCipherComparator(HTTP2Cipher.COMPARATOR);
        server.setNeedClientAuth(isNeedClientAuth);
    }

    public SslConnectionFactory getSslConnectionFactory(final String protocol) {
        return new SslConnectionFactory(server, protocol);
    }
}
