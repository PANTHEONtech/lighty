/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.config;

public final class GnmiSimulatorConfiguration {
    private String targetAddress = "0.0.0.0";
    private int targetPort = 10161;
    private String initialConfigDataPath;
    private String initialStateDataPath;
    private int maxConnections = 50;
    private String certPath;
    private String certKeyPath;
    private String yangsPath;
    private String username;
    private String password;
    private boolean usePlaintext = false;

    public String getTargetAddress() {
        return targetAddress;
    }

    public void setTargetAddress(String targetAddress) {
        this.targetAddress = targetAddress;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public String getInitialConfigDataPath() {
        return initialConfigDataPath;
    }

    public void setInitialConfigDataPath(String initialConfigDataPath) {
        this.initialConfigDataPath = initialConfigDataPath;
    }

    public String getInitialStateDataPath() {
        return initialStateDataPath;
    }

    public void setInitialStateDataPath(String initialStateDataPath) {
        this.initialStateDataPath = initialStateDataPath;
    }

    public String getCertPath() {
        return certPath;
    }

    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    public String getCertKeyPath() {
        return certKeyPath;
    }

    public void setCertKeyPath(String certKeyPath) {
        this.certKeyPath = certKeyPath;
    }

    public String getYangsPath() {
        return yangsPath;
    }

    public void setYangsPath(String yangsPath) {
        this.yangsPath = yangsPath;
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

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public boolean isUsePlaintext() {
        return usePlaintext;
    }

    public void setUsePlaintext(boolean usePlaintext) {
        this.usePlaintext = usePlaintext;
    }
}
