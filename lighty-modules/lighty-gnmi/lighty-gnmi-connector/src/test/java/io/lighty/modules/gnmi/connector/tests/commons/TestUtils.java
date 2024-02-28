/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.tests.commons;

import com.google.common.io.CharStreams;
import io.lighty.aaa.util.AAAConfigUtils;
import io.lighty.modules.gnmi.connector.configuration.SecurityFactory;
import io.lighty.modules.gnmi.connector.gnmi.session.impl.GnmiSessionFactoryImpl;
import io.lighty.modules.gnmi.connector.security.Security;
import io.lighty.modules.gnmi.connector.session.SessionManagerFactory;
import io.lighty.modules.gnmi.connector.session.SessionManagerFactoryImpl;
import io.lighty.modules.gnmi.connector.session.api.SessionManager;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

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
        final KeyPair keyPair = AAAConfigUtils.decodePrivateKey(new StringReader(readResource(CLIENT_KEY)), PASSPHRASE);
        final Security gnmiSecurity = SecurityFactory.createGnmiSecurity(readResource(CA_CERTS),
                readResource(CLIENT_CERTS), keyPair.getPrivate());

        return SESSION_MANAGER_FACTORY.createSessionManager(gnmiSecurity);
    }

    private static String readResource(final String classPath) throws Exception {
        try (InputStream inputStream = TestUtils.class.getResourceAsStream(classPath)) {
            return CharStreams.toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        }
    }
}
