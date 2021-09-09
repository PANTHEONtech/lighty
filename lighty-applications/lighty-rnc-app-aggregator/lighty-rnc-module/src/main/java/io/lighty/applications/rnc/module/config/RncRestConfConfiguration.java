/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import java.net.InetSocketAddress;

public class RncRestConfConfiguration extends RestConfConfiguration {
    @JsonIgnore
    private SecurityConfig securityConfig;

    // These are default configuration and that should be rewritten
    @SuppressWarnings("squid:S2068")
    private String keyStorePassword = "8pgETwat";
    private String keyStoreType = "JKS";
    private String keyStoreFilePath = "keystore/lightyio.jks";
    private boolean useHttps = false;

    public RncRestConfConfiguration() {
        super();
        // by default listen on any IP address (0.0.0.0) not only on loopback
        this.setInetAddress(new InetSocketAddress(this.getHttpPort()).getAddress());
    }

    public RncRestConfConfiguration(final RncRestConfConfiguration rncRestConfConfiguration) {
        super(rncRestConfConfiguration);
        this.keyStorePassword = rncRestConfConfiguration.getKeyStorePassword();
        this.keyStoreType = rncRestConfConfiguration.getKeyStoreType();
        this.keyStoreFilePath = rncRestConfConfiguration.getKeyStoreFilePath();
        this.useHttps = rncRestConfConfiguration.isUseHttps();
        setSecurityConfig(rncRestConfConfiguration.getSecurityConfig());
    }

    public void setSecurityConfig(final SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public String getKeyStoreFilePath() {
        return keyStoreFilePath;
    }

    public void setKeyStoreFilePath(String keyStoreFilePath) {
        this.keyStoreFilePath = keyStoreFilePath;
    }

    public boolean isUseHttps() {
        return useHttps;
    }

    public void setUseHttps(boolean useHttps) {
        this.useHttps = useHttps;
    }
}
