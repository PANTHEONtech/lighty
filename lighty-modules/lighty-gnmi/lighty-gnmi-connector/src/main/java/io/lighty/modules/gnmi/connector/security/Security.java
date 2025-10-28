/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.security;

import com.google.common.base.Preconditions;
import io.grpc.netty.GrpcSslContexts;
import io.lighty.modules.gnmi.connector.session.api.SessionManager;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import javax.net.ssl.SSLException;

/**
 * Contains security (keys, certificates) for {@link SessionManager}.
 */
public class Security {

    private final X509Certificate[] caCertificates;
    private final X509Certificate[] clientCertificatesChain;
    private final PrivateKey privateKey;

    private SslContext sslContext;

    public Security(final Collection<X509Certificate> caCertificates,
                    final Collection<X509Certificate> clientCertificatesChain,
                    final PrivateKey privateKey) {
        Preconditions.checkArgument(caCertificates != null && !caCertificates.isEmpty(),
                "CA certificate are missing!");
        Preconditions.checkArgument(clientCertificatesChain != null && !clientCertificatesChain.isEmpty(),
                "Client certificate chain is missing!");
        Preconditions.checkArgument(privateKey != null, "Path to private key is missing!");

        this.caCertificates = caCertificates.toArray(new X509Certificate[0]);
        this.clientCertificatesChain = clientCertificatesChain.toArray(new X509Certificate[0]);
        this.privateKey = privateKey;
    }

    /**
     * Use this constructor when establishing insecure ssl communication without certs.
     */
    public Security() {
        this.caCertificates = null;
        this.clientCertificatesChain = null;
        this.privateKey = null;
    }

    public SslContext getSslContext() throws SSLException {
        if (this.sslContext != null) {
            return this.sslContext;
        }
        final SslContextBuilder contextBuilder = GrpcSslContexts.forClient();
        if (this.caCertificates != null && this.clientCertificatesChain != null) {
            contextBuilder.trustManager(this.caCertificates);
            contextBuilder.keyManager(privateKey, this.clientCertificatesChain);
        }
        else {
            contextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        }
        this.sslContext = contextBuilder.build();
        return this.sslContext;
    }
}
