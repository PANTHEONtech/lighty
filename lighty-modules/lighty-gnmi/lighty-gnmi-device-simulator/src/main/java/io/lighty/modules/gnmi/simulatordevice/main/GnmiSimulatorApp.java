/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.main;

import com.beust.jcommander.JCommander;
import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDeviceBuilder;
import io.lighty.modules.gnmi.simulatordevice.utils.GnmiSimulatorConfUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnmiSimulatorApp {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiSimulatorApp.class);

    private GnmiSimulatorConfiguration gnmiSimulatorConfiguration;
    private SimulatedGnmiDevice device;

    public static void main(String[] args) throws IOException {
        BasicConfigurator.configure();
        final GnmiSimulatorApp gnmiSimulatorApp = new GnmiSimulatorApp();
        gnmiSimulatorApp.start(true, args);
    }

    public void start(final boolean registerShutdownHook, final String[] args) throws IOException {

        // Parse args
        final Arguments arguments = new Arguments();
        JCommander.newBuilder()
            .addObject(arguments)
            .build()
            .parse(args);

        if (arguments.getLoggerPath() != null) {
            LOG.debug("Argument for custom logging settings path is present: {} ", arguments.getLoggerPath());
            PropertyConfigurator.configure(arguments.getLoggerPath());
            LOG.info("Custom logger properties loaded successfully");
        }

        if (arguments.getConfigPath() == null) {
            gnmiSimulatorConfiguration = GnmiSimulatorConfUtils.loadDefaultGnmiSimulatorConfiguration();
        } else {
            gnmiSimulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(Files.newInputStream(Path.of(arguments.getConfigPath())));
        }

        device = new SimulatedGnmiDeviceBuilder()
            .setHost(gnmiSimulatorConfiguration.getDeviceAddress())
            .setPort(gnmiSimulatorConfiguration.getDevicePort())
            .setInitialConfigDataPath(gnmiSimulatorConfiguration.getInitialDataConfig())
            .setInitialStateDataPath(gnmiSimulatorConfiguration.getInitialDataState())
            .setYangsPath(gnmiSimulatorConfiguration.getYangFolder())
            .setUsernamePasswordAuth(gnmiSimulatorConfiguration.getUsername(),gnmiSimulatorConfiguration.getPassword())
            .setCertificatePath(gnmiSimulatorConfiguration.getCertPath())
            .setKeyPath(gnmiSimulatorConfiguration.getCertKey())
            .build();
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
