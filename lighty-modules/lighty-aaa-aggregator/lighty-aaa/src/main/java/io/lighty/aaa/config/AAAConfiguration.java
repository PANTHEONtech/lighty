/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opendaylight.aaa.cert.api.ICertificateManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.DatastoreConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.ShiroConfiguration;

public class AAAConfiguration {
    @JsonIgnore
    private ICertificateManager certificateManager;
    @JsonIgnore
    private ShiroConfiguration shiroConf;
    @JsonIgnore
    private DatastoreConfig datastoreConf;

    private boolean enableAAA = false;
    // These are default configuration and that should be rewritten
    private String moonEndpointPath = "/moon";
    private String dbPassword = "bar";
    private String dbUsername = "foo";
    private String dbPath = "./data";
    private String password = "admin";
    private String username = "admin";

    public AAAConfiguration() {
        this.shiroConf = ShiroConfigurationConfig.getDefault();
        this.datastoreConf = DatastoreConfigurationConfig.getDefault();
    }

    public String getDbPath() {
        return dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ShiroConfiguration getShiroConf() {
        return shiroConf;
    }

    public void setShiroConf(ShiroConfiguration shiroConf) {
        this.shiroConf = shiroConf;
    }

    public DatastoreConfig getDatastoreConf() {
        return datastoreConf;
    }

    public void setDatastoreConf(DatastoreConfig datastoreConf) {
        this.datastoreConf = datastoreConf;
    }

    public ICertificateManager getCertificateManager() {
        return certificateManager;
    }

    public void setCertificateManager(ICertificateManager certificateManager) {
        this.certificateManager = certificateManager;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getMoonEndpointPath() {
        return moonEndpointPath;
    }

    public void setMoonEndpointPath(String moonEndpointPath) {
        this.moonEndpointPath = moonEndpointPath;
    }

    public boolean isEnableAAA() {
        return enableAAA;
    }

    public void setEnableAAA(boolean enableAAA) {
        this.enableAAA = enableAAA;
    }
}
