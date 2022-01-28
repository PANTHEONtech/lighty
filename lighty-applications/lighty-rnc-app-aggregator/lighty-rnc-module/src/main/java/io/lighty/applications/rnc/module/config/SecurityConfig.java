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
    private final KeyStore trustKeyStore;
    private final String password;
    private final String trustPassword;
    private final SslContextFactory sslContextFactory;
    private final boolean isNeedClientAuth;

    public SecurityConfig(final KeyStore keyStore, final String password, final KeyStore trustKeyStore, final String trustPassword, final boolean isNeedClientAuth) {
        this.keyStore = keyStore;
        this.password = password;
        this.trustKeyStore = trustKeyStore;
        this.trustPassword = trustPassword;
        this.isNeedClientAuth = isNeedClientAuth;
        sslContextFactory = new SslContextFactory.Server();
        initFactoryCtx();
    }

    private void initFactoryCtx() {
        sslContextFactory.setTrustStore(trustKeyStore);
        sslContextFactory.setTrustStorePassword(trustPassword);
        sslContextFactory.setKeyStore(keyStore);
        sslContextFactory.setKeyStorePassword(password);
        sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
        if(isNeedClientAuth == true)
            sslContextFactory.setNeedClientAuth(true);
    }

    public SslConnectionFactory getSslConnectionFactory(final String protocol) {
        return new SslConnectionFactory(sslContextFactory, protocol);
    }
}
