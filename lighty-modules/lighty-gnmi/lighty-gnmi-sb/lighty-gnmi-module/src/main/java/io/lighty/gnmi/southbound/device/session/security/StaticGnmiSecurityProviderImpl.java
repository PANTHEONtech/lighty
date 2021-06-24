/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.device.session.security;


import io.lighty.modules.southbound.configuration.SecurityFactory;
import io.lighty.modules.southbound.security.Security;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiNode;

/**
 * Provides default pre-generated client certificates for our gNMI simulated device.
 */
public class StaticGnmiSecurityProviderImpl implements GnmiSecurityProvider {


    private static final String STATIC_CLIENT_KEY_PATH = "/sim_certs/client_pkcs8.key";
    private static final String STATIC_CLIENT_CERTS_PATH = "/sim_certs/client.crt";
    private static final String STATIC_CA_CERTS_PATH = "/sim_certs/ca.crt";

    public StaticGnmiSecurityProviderImpl() {

    }

    @Override
    public Security getSecurity(GnmiNode gnmiNode) throws SessionSecurityException {
        return defaultSecurity();
    }

    public Security defaultSecurity() throws SessionSecurityException {
        try {
            return SecurityFactory.createGnmiSecurity(
                    Paths.get(StaticGnmiSecurityProviderImpl.class.getResource(STATIC_CA_CERTS_PATH).toURI()),
                    Paths.get(StaticGnmiSecurityProviderImpl.class.getResource(STATIC_CLIENT_CERTS_PATH).toURI()),
                    Paths.get(StaticGnmiSecurityProviderImpl.class.getResource(STATIC_CLIENT_KEY_PATH).toURI()));

        } catch (IOException | CertificateException | NoSuchAlgorithmException
                | URISyntaxException | InvalidKeySpecException e) {
            throw new SessionSecurityException("Error while creating default security", e);
        }
    }

}
