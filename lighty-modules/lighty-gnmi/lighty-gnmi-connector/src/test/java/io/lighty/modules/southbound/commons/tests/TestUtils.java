/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.southbound.commons.tests;

import io.lighty.modules.southbound.configuration.SecurityFactory;
import io.lighty.modules.southbound.gnmi.session.impl.GnmiSessionFactoryImpl;
import io.lighty.modules.southbound.security.Security;
import io.lighty.modules.southbound.session.SessionManagerFactory;
import io.lighty.modules.southbound.session.SessionManagerFactoryImpl;
import io.lighty.modules.southbound.session.api.SessionManager;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

public final class TestUtils {

    private static final String CLIENT_KEY = "/certs/client_pkcs8.key";
    private static final String CLIENT_CERTS = "/certs/client.crt";
    private static final String CA_CERTS = "/certs/ca.crt";
    private static final SessionManagerFactory SESSION_MANAGER_FACTORY
            = new SessionManagerFactoryImpl(new GnmiSessionFactoryImpl());

    private TestUtils() {
        throw new UnsupportedOperationException("Utility classes should not be instantiated!");
    }

    public static SessionManager createSessionManagerWithCerts()
            throws URISyntaxException, InvalidKeySpecException, CertificateException, NoSuchAlgorithmException,
            IOException {
        final Security security = SecurityFactory.createGnmiSecurity(
                Paths.get(TestUtils.class.getResource(CA_CERTS).toURI()),
                Paths.get(TestUtils.class.getResource(CLIENT_CERTS).toURI()),
                Paths.get(TestUtils.class.getResource(CLIENT_KEY).toURI())
        );

        return SESSION_MANAGER_FACTORY.createSessionManager(security);
    }
}
