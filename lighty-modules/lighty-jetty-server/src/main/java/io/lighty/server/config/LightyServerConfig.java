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
    private boolean useHttp2 = false;
    private boolean needClientAuth = false;
    private boolean enableOpenApi = false;
    private int callhomePort = 4334;

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

    public boolean isEnableOpenApi() {
        return enableOpenApi;
    }

    public void setEnableOpenApi(boolean enableOpenApi) {
        this.enableOpenApi = enableOpenApi;
    }

    public boolean isUseHttp2() {
        return useHttp2;
    }

    public void setUseHttp2(boolean useHttp2) {
        this.useHttp2 = useHttp2;
    }

    public int getCallhomePort() {
        return callhomePort;
    }

    public void setCallhomePort(int callhomePort) {
        this.callhomePort = callhomePort;
    }
}
