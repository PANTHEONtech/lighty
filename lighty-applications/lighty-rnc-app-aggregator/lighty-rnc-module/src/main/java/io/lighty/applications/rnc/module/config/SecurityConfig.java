/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.config;

import java.security.KeyStore;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class SecurityConfig {
    private final KeyStore keyStore;
    private final String password;
    private final SslContextFactory sslContextFactory;

    public SecurityConfig(final KeyStore keyStore, final String password) {
        this.keyStore = keyStore;
        this.password = password;
        sslContextFactory = new SslContextFactory.Server();
        initFactoryCtx();
    }

    private void initFactoryCtx() {
        sslContextFactory.setTrustStore(keyStore);
        sslContextFactory.setTrustStorePassword(password);
        sslContextFactory.setKeyStore(keyStore);
        sslContextFactory.setKeyStorePassword(password);
        sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
    }

    public SslConnectionFactory getSslConnectionFactory(final String protocol) {
        return new SslConnectionFactory(sslContextFactory, protocol);
    }
}
