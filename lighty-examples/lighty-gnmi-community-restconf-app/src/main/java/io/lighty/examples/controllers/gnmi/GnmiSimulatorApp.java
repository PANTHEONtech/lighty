/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.examples.controllers.gnmi;

import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDeviceBuilder;
import java.io.IOException;
import java.util.Arrays;

public class GnmiSimulatorApp {
    private static final String DEVICE_ADDRESS = "127.0.0.1";
    private static final int DEVICE_PORT = 3333;
    private static final String INITIAL_DATA = "/simulator/initialJsonData.json";
    private static final String CERT_PATH = "/certificates/server.crt";
    private static final String CERT_KEY = "/certificates/server-pkcs8.key";
    private static final String YANG_FOLDER = "/yangs";
    private static final String USERNAME = "Admin";
    private static final String PASSWORD = "Admin";

    private final String folderWithResources;
    private SimulatedGnmiDevice device;

    public GnmiSimulatorApp(final String folderWithResources) {
        this.folderWithResources = folderWithResources;
    }

    public static void main(String[] args) throws IOException {
        final String folderWithResources = Arrays.stream(args).findFirst().orElse("");
        final GnmiSimulatorApp gnmiSimulatorApp = new GnmiSimulatorApp(folderWithResources);
        gnmiSimulatorApp.start(true);
    }

    public void start(final boolean registerShutdownHook) throws IOException {
        device = new SimulatedGnmiDeviceBuilder()
                .setHost(DEVICE_ADDRESS)
                .setPort(DEVICE_PORT)
                .setInitialConfigDataPath(folderWithResources + INITIAL_DATA)
                .setInitialStateDataPath(folderWithResources + INITIAL_DATA)
                .setYangsPath(folderWithResources + YANG_FOLDER)
                .setUsernamePasswordAuth(USERNAME, PASSWORD)
                .setCertificatePath(folderWithResources + CERT_PATH)
                .setKeyPath(folderWithResources + CERT_KEY)
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
