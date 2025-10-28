/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.main;

import com.beust.jcommander.JCommander;
import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.utils.EffectiveModelContextBuilder.EffectiveModelContextBuilderException;
import io.lighty.modules.gnmi.simulatordevice.utils.GnmiSimulatorConfUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnmiSimulatorApp {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiSimulatorApp.class);

    private SimulatedGnmiDevice device;

    public static void main(String[] args) {
        final GnmiSimulatorApp gnmiSimulatorApp = new GnmiSimulatorApp();
        gnmiSimulatorApp.start(true, args);
    }

    public void start(final boolean registerShutdownHook, final String[] args) {

        // Parse args
        final Arguments arguments = new Arguments();
        JCommander.newBuilder()
            .addObject(arguments)
            .build()
            .parse(args);
        final GnmiSimulatorConfiguration gnmiSimulatorConfiguration;

        try {
            if (arguments.getConfigPath() == null) {
                gnmiSimulatorConfiguration = GnmiSimulatorConfUtils.loadDefaultGnmiSimulatorConfiguration();
            } else {
                gnmiSimulatorConfiguration = GnmiSimulatorConfUtils
                        .loadGnmiSimulatorConfiguration(Files.newInputStream(Path.of(arguments.getConfigPath())));
            }
            device = new SimulatedGnmiDevice(gnmiSimulatorConfiguration);
            device.start();

        } catch (EffectiveModelContextBuilderException e) {
            LOG.error("Lighty gNMI application - failed during creating schema context: ", e);
            shutdown();
        } catch (IOException e) {
            LOG.error("Lighty gNMI application - failed to read configuration: ", e);
            shutdown();
        }
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
