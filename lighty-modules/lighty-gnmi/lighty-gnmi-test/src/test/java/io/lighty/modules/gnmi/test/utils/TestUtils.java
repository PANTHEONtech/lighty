/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.test.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.lighty.modules.gnmi.connector.configuration.SecurityFactory;
import io.lighty.modules.gnmi.connector.gnmi.session.impl.GnmiSessionFactoryImpl;
import io.lighty.modules.gnmi.connector.security.Security;
import io.lighty.modules.gnmi.connector.session.SessionManagerFactory;
import io.lighty.modules.gnmi.connector.session.SessionManagerFactoryImpl;
import io.lighty.modules.gnmi.connector.session.api.SessionManager;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import org.apache.commons.io.IOUtils;

public final class TestUtils {

    private TestUtils() {
        //Utility class
    }

    private static final String CLIENT_KEY = "/testUtilsCerts/client_pkcs8.key";
    private static final String CLIENT_CERTS = "/testUtilsCerts/client.crt";
    private static final String CA_CERTS = "/testUtilsCerts/ca.crt";
    private static final SessionManagerFactory SESSION_MANAGER_FACTORY
            = new SessionManagerFactoryImpl(new GnmiSessionFactoryImpl());

    public static SessionManager createSessionManagerWithCerts()
            throws URISyntaxException, InvalidKeySpecException, CertificateException, NoSuchAlgorithmException,
            IOException {
        final Security gnmiSecurity = SecurityFactory.createGnmiSecurity(
                Paths.get(TestUtils.class.getResource(CA_CERTS).toURI()),
                Paths.get(TestUtils.class.getResource(CLIENT_CERTS).toURI()),
                Paths.get(TestUtils.class.getResource(CLIENT_KEY).toURI())
        );

        return SESSION_MANAGER_FACTORY.createSessionManager(gnmiSecurity);
    }

    public static boolean jsonMatch(final String first, final String second) {
        final JsonParser parser = new JsonParser();
        final JsonElement jsonA = parser.parse(first);
        final JsonElement jsonB = parser.parse(second);
        return jsonA.equals(jsonB);
    }

    public static String readFile(final String filePath) throws IOException {
        return IOUtils.toString(Path.of(filePath).toUri(), Charset.defaultCharset());
    }
}
