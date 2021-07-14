/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.examples.controllers.gnmi;

import io.lighty.applications.rcgnmi.app.RCgNMIApp;
import java.io.IOException;

public final class Main {

    private static GnmiSimulatorApp simulator;
    private static RCgNMIApp gNMIApp;

    public static void main(String[] args) throws IOException {

//        simulator = new GnmiSimulatorApp(Main.class.getClassLoader().getResource("simulator").getPath());
        simulator = new GnmiSimulatorApp("simulator");
        simulator.start(true);

        gNMIApp = new RCgNMIApp();
        gNMIApp.start(new String[]{
                "-c",
                Main.class.getClassLoader().getResource("example_config.json").getPath()});

        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown));
    }

    private static void shutdown() {
        if (gNMIApp != null) {
            gNMIApp.stop();
        }
        if (simulator != null) {
            simulator.shutdown();
        }
    }

    private Main() {
        throw new UnsupportedOperationException();
    }
}
