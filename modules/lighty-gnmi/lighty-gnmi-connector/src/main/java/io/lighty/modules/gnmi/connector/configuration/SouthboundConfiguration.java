/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.configuration;

import io.lighty.modules.gnmi.connector.security.Security;

/**
 * This class contains configuration of {@link Security}.
 */
public class SouthboundConfiguration {

    private String caCertificatePaths;
    private String clientCertificatesChainPath;
    private String privateKeyPath;

    public String getCaCertificatePaths() {
        return caCertificatePaths;
    }

    public void setCaCertificatePaths(String caCertificatePaths) {
        this.caCertificatePaths = caCertificatePaths;
    }

    public String getClientCertificatesChainPath() {
        return clientCertificatesChainPath;
    }

    public void setClientCertificatesChainPath(String clientCertificatesChainPath) {
        this.clientCertificatesChainPath = clientCertificatesChainPath;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }
}
