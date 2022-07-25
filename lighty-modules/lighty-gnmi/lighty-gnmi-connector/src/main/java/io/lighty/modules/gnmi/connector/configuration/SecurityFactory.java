/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.configuration;

import io.lighty.modules.gnmi.connector.security.Security;
import io.lighty.modules.gnmi.connector.session.api.SessionManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * This factory class provides creation of {@link Security} instances. These instances are used as security
 * configuration for {@link SessionManager}.
 */
public final class SecurityFactory {

    private SecurityFactory() {
        // Hidden on purpose
    }

    public static Security createGnmiSecurity(final String caCertificate, final String clientCertificate,
                                              final PrivateKey privateKey) throws CertificateException {
        return new Security(
                loadCertificates(caCertificate),
                loadCertificates(clientCertificate),
                privateKey);
    }

    public static Security createInsecureGnmiSecurity() {
        return new Security();
    }

    private static Collection<X509Certificate> loadCertificates(final String certificate) throws CertificateException {
        return getX509Certificates(new ByteArrayInputStream(certificate.getBytes(Charset.defaultCharset())));
    }

    @SuppressWarnings("unchecked")
    private static Collection<X509Certificate> getX509Certificates(final InputStream certsInputStream)
            throws CertificateException {
        return (Collection<X509Certificate>) CertificateFactory.getInstance("X.509")
                .generateCertificates(certsInputStream);
    }
}
