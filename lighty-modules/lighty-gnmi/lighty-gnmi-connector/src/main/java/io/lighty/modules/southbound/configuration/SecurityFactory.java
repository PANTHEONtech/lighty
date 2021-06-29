/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.southbound.configuration;

import io.lighty.modules.southbound.security.Security;
import io.lighty.modules.southbound.session.api.SessionManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;

/**
 * This factory class provides creation of {@link Security} instances. These instances are used as security
 * configuration for {@link SessionManager}.
 */
public final class SecurityFactory {

    private SecurityFactory() {
        throw new UnsupportedOperationException("Instance of this class should not be created!");
    }

    public static Security createGnmiSecurity(final SouthboundConfiguration configuration)
            throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        return createGnmiSecurity(
                Paths.get(configuration.getCaCertificatePaths()),
                Paths.get(configuration.getClientCertificatesChainPath()),
                Paths.get(configuration.getPrivateKeyPath()));
    }

    public static Security createGnmiSecurity(final Path caCertificatePath, final Path clientCertificatePath,
                                              final Path privateKeyPath)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, CertificateException {
        return new Security(
                loadCertsFromFile(caCertificatePath),
                loadCertsFromFile(clientCertificatePath),
                loadPrivateKeyFromFile(privateKeyPath));
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


    private static Collection<X509Certificate> loadCertsFromFile(final Path certsFilePath) throws IOException,
            CertificateException {
        try (InputStream certsInputStream = Files.newInputStream(certsFilePath)) {
            return getX509Certificates(certsInputStream);
        }
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

    private static PrivateKey loadPrivateKeyFromFile(final Path keyFile) throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException {
        final byte[] keyBytes = Files.readAllBytes(keyFile);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}
