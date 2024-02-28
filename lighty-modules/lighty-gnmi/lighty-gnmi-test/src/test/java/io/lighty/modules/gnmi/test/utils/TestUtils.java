/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.test.utils;

import com.google.common.io.CharStreams;
import io.lighty.aaa.util.AAAConfigUtils;
import io.lighty.gnmi.southbound.device.session.security.SessionSecurityException;
import io.lighty.modules.gnmi.connector.configuration.SecurityFactory;
import io.lighty.modules.gnmi.connector.gnmi.session.impl.GnmiSessionFactoryImpl;
import io.lighty.modules.gnmi.connector.security.Security;
import io.lighty.modules.gnmi.connector.session.SessionManagerFactory;
import io.lighty.modules.gnmi.connector.session.SessionManagerFactoryImpl;
import io.lighty.modules.gnmi.connector.session.api.SessionManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import org.apache.commons.io.IOUtils;

public final class TestUtils {

    private TestUtils() {
        //Utility class
    }

    private static final String CLIENT_KEY = "/certs/client.key";
    private static final String CLIENT_CERTS = "/certs/client.crt";
    private static final String CA_CERTS = "/certs/ca.crt";
    private static final String PASSPHRASE = "";
    private static final SessionManagerFactory SESSION_MANAGER_FACTORY
            = new SessionManagerFactoryImpl(new GnmiSessionFactoryImpl());

    public static SessionManager createSessionManagerWithCerts() throws IOException, SessionSecurityException,
            CertificateException {
        final KeyPair keyPair = getKeyPair(readResource(CLIENT_KEY));
        final Security gnmiSecurity = SecurityFactory.createGnmiSecurity(readResource(CA_CERTS),
                readResource(CLIENT_CERTS), keyPair.getPrivate());

        return SESSION_MANAGER_FACTORY.createSessionManager(gnmiSecurity);
    }

    private static KeyPair getKeyPair(final String clientKey) throws SessionSecurityException {
        try {
            return AAAConfigUtils.decodePrivateKey(new StringReader(clientKey), PASSPHRASE);
        } catch (IOException e) {
            throw new SessionSecurityException("Error while creating KeyPair from private key and passphrase", e);
        }
    }

    private static String readResource(final String classPath) throws IOException {
        String result;
        try (InputStream inputStream = TestUtils.class.getResourceAsStream(classPath)) {
            result = CharStreams.toString(new InputStreamReader(
                    inputStream, StandardCharsets.UTF_8));
        }
        return result;
    }

    public static String readFile(final String filePath) throws IOException {
        return IOUtils.toString(Path.of(filePath).toUri(), StandardCharsets.UTF_8);
    }
}
