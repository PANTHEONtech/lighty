/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.impl;

import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice.SimulatedGnmiDeviceConnectionInfoHolder;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice.SimulatedGnmiDeviceGroupHolder;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice.SimulatedGnmiDevicePathsHolder;
import io.lighty.modules.gnmi.simulatordevice.utils.UsernamePasswordAuth;

public class SimulatedGnmiDeviceBuilder {
    private GnmiSimulatorConfiguration gnmiSimulatorConfiguration = null;

    public SimulatedGnmiDeviceBuilder from(final GnmiSimulatorConfiguration simulatorConfiguration) {
        this.gnmiSimulatorConfiguration = simulatorConfiguration;
        return this;
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    public SimulatedGnmiDevice build() throws ConfigurationException {
        try {
            return new SimulatedGnmiDevice(
                    new SimulatedGnmiDeviceGroupHolder(gnmiSimulatorConfiguration.getBossGroup(),
                            gnmiSimulatorConfiguration.getWorkerGroup()),
                    new SimulatedGnmiDevicePathsHolder(gnmiSimulatorConfiguration.getCertPath(),
                            gnmiSimulatorConfiguration.getCertKeyPath(),
                            gnmiSimulatorConfiguration.getYangsPath(),
                            gnmiSimulatorConfiguration.getInitialConfigDataPath(),
                            gnmiSimulatorConfiguration.getInitialStateDataPath()),
                    new SimulatedGnmiDeviceConnectionInfoHolder(gnmiSimulatorConfiguration.getTargetAddress(),
                            gnmiSimulatorConfiguration.getTargetPort(),
                            gnmiSimulatorConfiguration.getMaxConnections()),
                    new UsernamePasswordAuth(gnmiSimulatorConfiguration.getUsername(),
                            gnmiSimulatorConfiguration.getPassword()),
                    gnmiSimulatorConfiguration.isUsePlaintext(),
                    gnmiSimulatorConfiguration.getGson(),
                    gnmiSimulatorConfiguration.getSupportedEncodings(),
                    gnmiSimulatorConfiguration.getYangModulesInfo());
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }
}