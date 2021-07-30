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
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import io.lighty.modules.gnmi.simulatordevice.utils.UsernamePasswordAuth;
import io.netty.channel.EventLoopGroup;
import java.util.EnumSet;

public class SimulatedGnmiDeviceBuilder {
    private GnmiSimulatorConfiguration gnmiSimulatorConfiguration = null;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Gson gson;
    private EnumSet<Gnmi.Encoding> supportedEncodings;

    public SimulatedGnmiDeviceBuilder setBossGroup(final EventLoopGroup bossGroup) {
        this.bossGroup = bossGroup;
        return this;
    }

    public SimulatedGnmiDeviceBuilder setWorkerGroup(final EventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
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

    public SimulatedGnmiDeviceBuilder from(final GnmiSimulatorConfiguration simulatorConfiguration) {
        this.gnmiSimulatorConfiguration = simulatorConfiguration;
        return this;
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    public SimulatedGnmiDevice build() throws ConfigurationException {
        try {
            return new SimulatedGnmiDevice(bossGroup, workerGroup,
                    gnmiSimulatorConfiguration.getTargetAddress(),
                    gnmiSimulatorConfiguration.getTargetPort(),
                    gnmiSimulatorConfiguration.getMaxConnections(),
                    gnmiSimulatorConfiguration.getCertPath(),
                    gnmiSimulatorConfiguration.getCertKeyPath(),
                    gnmiSimulatorConfiguration.getYangsPath(),
                    gnmiSimulatorConfiguration.getInitialConfigDataPath(),
                    gnmiSimulatorConfiguration.getInitialStateDataPath(),
                    new UsernamePasswordAuth(gnmiSimulatorConfiguration.getUsername(),
                            gnmiSimulatorConfiguration.getPassword()),
                    gnmiSimulatorConfiguration.isUsePlaintext(),
                    gson, supportedEncodings);
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }
}