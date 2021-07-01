/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.impl;


import com.google.gson.Gson;
import gnmi.Gnmi;
import io.lighty.modules.gnmi.simulatordevice.utils.UsernamePasswordAuth;
import io.netty.channel.EventLoopGroup;
import java.util.EnumSet;

public class SimulatedGnmiDeviceBuilder {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private String host = "0.0.0.0";
    private int port = 10161;
    private int maxConnections = 50;
    private String certificatePath;
    private String keyPath;
    private String yangsPath;
    private String initialConfigDataPath;
    private String initialStateDataPath;
    private UsernamePasswordAuth usernamePasswordAuth;
    private boolean plaintext = false;
    private Gson gson;
    private EnumSet<Gnmi.Encoding> supportedEncodings;

    public SimulatedGnmiDeviceBuilder setInitialConfigDataPath(final String initialConfigDataPath) {
        this.initialConfigDataPath = initialConfigDataPath;
        return this;
    }

    public SimulatedGnmiDeviceBuilder setInitialStateDataPath(final String initialStateDataPath) {
        this.initialStateDataPath = initialStateDataPath;
        return this;
    }

    public SimulatedGnmiDeviceBuilder setBossGroup(final EventLoopGroup bossGroup) {
        this.bossGroup = bossGroup;
        return this;
    }

    public SimulatedGnmiDeviceBuilder setWorkerGroup(final EventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
        return this;
    }

    public SimulatedGnmiDeviceBuilder setYangsPath(final String pathToYangs) {
        this.yangsPath = pathToYangs;
        return this;
    }

    public SimulatedGnmiDeviceBuilder setHost(final String host) {
        this.host = host;
        return this;
    }

    public SimulatedGnmiDeviceBuilder setPort(final int port) {
        this.port = port;
        return this;
    }

    public SimulatedGnmiDeviceBuilder setMaxConnections(final int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public SimulatedGnmiDeviceBuilder setCertificatePath(final String certificatePath) {
        this.certificatePath = certificatePath;
        return this;
    }

    public SimulatedGnmiDeviceBuilder setKeyPath(final String keyPath) {
        this.keyPath = keyPath;
        return this;
    }

    public SimulatedGnmiDeviceBuilder setUsernamePasswordAuth(final String username, final String password) {
        this.usernamePasswordAuth = new UsernamePasswordAuth(username, password);
        return this;
    }

    public SimulatedGnmiDeviceBuilder usePlaintext() {
        this.plaintext = true;
        return this;
    }

    public SimulatedGnmiDeviceBuilder setGsonInstance(final Gson customGson) {
        this.gson = customGson;
        return this;
    }

    public SimulatedGnmiDeviceBuilder setSupportedEncodings(final EnumSet<Gnmi.Encoding> encodings) {
        this.supportedEncodings = encodings;
        return this;
    }


    public SimulatedGnmiDevice build() {
        return new SimulatedGnmiDevice(bossGroup, workerGroup, host, port, maxConnections, certificatePath, keyPath,
                yangsPath, initialConfigDataPath, initialStateDataPath, usernamePasswordAuth, plaintext, gson,
                supportedEncodings);
    }
}