/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server.config;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class LightyServerConfig {
    @JsonIgnore
    private SecurityConfig securityConfig;
    private String keyStorePassword = "8pgETwat";
    private String keyStoreType = "JKS";
    private String keyStoreFilePath = "keystore/lightyio.jks";
    private String trustKeyStorePassword = "8pgETwat";
    private String trustKeyStoreFilePath = "keystore/lightyio.jks";
    private boolean useHttps = false;
    private boolean needClientAuth = false;
    private boolean enableSwagger = false;

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

    public String getTrustKeyStorePassword() {
        return trustKeyStorePassword;
    }

    public void setTrustKeyStorePassword(String trustKeyStorePassword) {
        this.trustKeyStorePassword = trustKeyStorePassword;
    }

    public String getTrustKeyStoreFilePath() {
        return trustKeyStoreFilePath;
    }

    public void setTrustKeyStoreFilePath(String trustKeyStoreFilePath) {
        this.trustKeyStoreFilePath = trustKeyStoreFilePath;
    }

    public boolean isUseHttps() {
        return useHttps;
    }

    public void setUseHttps(boolean useHttps) {
        this.useHttps = useHttps;
    }

    public boolean isNeedClientAuth() {
        return needClientAuth;
    }

    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }

    public boolean isEnableSwagger() {
        return enableSwagger;
    }

    public void setEnableSwagger(boolean enableSwagger) {
        this.enableSwagger = enableSwagger;
    }
}
