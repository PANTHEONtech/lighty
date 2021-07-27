/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.config;

public final class GnmiSimulatorConfiguration {
    private String deviceAddress = "0.0.0.0";
    private int devicePort = 3333;
    private String initialDataConfig;
    private String initialDataState;
    private String certPath;
    private String certKey;
    private String yangFolder = getClass().getResource("/yangs").getPath();
    private String username = "admin";
    private String password = "admin";

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public int getDevicePort() {
        return devicePort;
    }

    public void setDevicePort(int devicePort) {
        this.devicePort = devicePort;
    }

    public String getInitialDataConfig() {
        return initialDataConfig;
    }

    public void setInitialDataConfig(String initialDataConfig) {
        this.initialDataConfig = initialDataConfig;
    }

    public String getInitialDataState() {
        return initialDataState;
    }

    public void setInitialDataState(String initialDataState) {
        this.initialDataState = initialDataState;
    }

    public String getCertPath() {
        return certPath;
    }

    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    public String getCertKey() {
        return certKey;
    }

    public void setCertKey(String certKey) {
        this.certKey = certKey;
    }

    public String getYangFolder() {
        return yangFolder;
    }

    public void setYangFolder(String yangFolder) {
        this.yangFolder = yangFolder;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
