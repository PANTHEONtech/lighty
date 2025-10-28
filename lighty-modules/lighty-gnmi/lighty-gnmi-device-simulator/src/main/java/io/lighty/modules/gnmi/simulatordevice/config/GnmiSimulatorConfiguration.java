/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;
import gnmi.Gnmi;
import io.netty.channel.EventLoopGroup;
import java.util.EnumSet;
import java.util.Set;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
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
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Gson gson;
    private EnumSet<Gnmi.Encoding> supportedEncodings;
    private Set<YangModuleInfo> yangModulesInfo;

    public void setYangModulesInfo(Set<YangModuleInfo> yangModulesInfo) {
        this.yangModulesInfo = yangModulesInfo;
    }

    public Set<YangModuleInfo> getYangModulesInfo() {
        return yangModulesInfo;
    }

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

    public EventLoopGroup getBossGroup() {
        return bossGroup;
    }

    public void setBossGroup(EventLoopGroup bossGroup) {
        this.bossGroup = bossGroup;
    }

    public EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

    public void setWorkerGroup(EventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
    }

    public Gson getGson() {
        return gson;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    public EnumSet<Gnmi.Encoding> getSupportedEncodings() {
        return supportedEncodings;
    }

    public void setSupportedEncodings(EnumSet<Gnmi.Encoding> supportedEncodings) {
        this.supportedEncodings = supportedEncodings;
    }
}
