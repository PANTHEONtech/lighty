/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.tests.commons;

import com.google.common.io.CharStreams;
import io.lighty.modules.gnmi.connector.configuration.SecurityFactory;
import io.lighty.modules.gnmi.connector.gnmi.session.impl.GnmiSessionFactoryImpl;
import io.lighty.modules.gnmi.connector.security.Security;
import io.lighty.modules.gnmi.connector.session.SessionManagerFactory;
import io.lighty.modules.gnmi.connector.session.SessionManagerFactoryImpl;
import io.lighty.modules.gnmi.connector.session.api.SessionManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

public final class TestUtils {

    private static final String CLIENT_KEY = "/certs/client.key";
    private static final String CLIENT_CERTS = "/certs/client.crt";
    private static final String CA_CERTS = "/certs/ca.crt";
    private static final String PASSPHRASE = "";
    private static final SessionManagerFactory SESSION_MANAGER_FACTORY
            = new SessionManagerFactoryImpl(new GnmiSessionFactoryImpl());

    private TestUtils() {
        //Utility class
    }

    public static SessionManager createSessionManagerWithCerts() throws Exception {
        final KeyPair keyPair = decodePrivateKey(new StringReader(readResource(CLIENT_KEY)), PASSPHRASE);
        final Security gnmiSecurity = SecurityFactory.createGnmiSecurity(readResource(CA_CERTS),
                readResource(CLIENT_CERTS), keyPair.getPrivate());

        return SESSION_MANAGER_FACTORY.createSessionManager(gnmiSecurity);
    }

    private static String readResource(final String classPath) throws Exception {
        try (InputStream inputStream = TestUtils.class.getResourceAsStream(classPath)) {
            return CharStreams.toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        }
    }

    private static KeyPair decodePrivateKey(final Reader reader, final String passphrase) throws IOException {
        try (PEMParser keyReader = new PEMParser(reader)) {
            final var converter = new JcaPEMKeyConverter();
            var prov = java.security.Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
            prov = prov != null ? prov : new BouncyCastleProvider();
            final var decryptionProv = new JcePEMDecryptorProviderBuilder().setProvider(prov)
                .build(passphrase.toCharArray());

            final var privateKey = keyReader.readObject();
            KeyPair keyPair;
            if (privateKey instanceof PEMEncryptedKeyPair pemPrivateKey) {
                keyPair = converter.getKeyPair(pemPrivateKey.decryptKeyPair(decryptionProv));
            } else {
                keyPair = converter.getKeyPair((PEMKeyPair) privateKey);
            }
            return keyPair;
        }
    }
}
