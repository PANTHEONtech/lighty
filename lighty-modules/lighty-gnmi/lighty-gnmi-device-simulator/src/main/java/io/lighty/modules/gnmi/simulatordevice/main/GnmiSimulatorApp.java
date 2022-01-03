/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.main;

import com.beust.jcommander.JCommander;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDeviceBuilder;
import io.lighty.modules.gnmi.simulatordevice.utils.GnmiSimulatorConfUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GnmiSimulatorApp {

    private SimulatedGnmiDevice device;

    public static void main(String[] args) throws IOException, ConfigurationException {
        final GnmiSimulatorApp gnmiSimulatorApp = new GnmiSimulatorApp();
        gnmiSimulatorApp.start(true, args);
    }

    public void start(final boolean registerShutdownHook, final String[] args)
            throws IOException, ConfigurationException {

        // Parse args
        final Arguments arguments = new Arguments();
        JCommander.newBuilder()
            .addObject(arguments)
            .build()
            .parse(args);

        final GnmiSimulatorConfiguration gnmiSimulatorConfiguration;

        if (arguments.getConfigPath() == null) {
            gnmiSimulatorConfiguration = GnmiSimulatorConfUtils.loadDefaultGnmiSimulatorConfiguration();
        } else {
            gnmiSimulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(Files.newInputStream(Path.of(arguments.getConfigPath())));
        }

        device = new SimulatedGnmiDeviceBuilder().from(gnmiSimulatorConfiguration).build();
        device.start();
        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        }
    }

    public void shutdown() {
        if (device != null) {
            device.stop();
            device = null;
        }
    }
}
