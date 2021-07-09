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
    private static final String GNMI_CONFIGURATION = "/example_config.json";
    private static final String ASSEMBLY_RESOURCES
            = "/lighty-examples/lighty-gnmi-community-restconf-app/src/main/assembly/resources";
    private static final String LIGHTY = "lighty";
    private static final String USER_DIR = "user.dir";

    public static void main(String[] args) throws IOException {
        String currentFolder = System.getProperty(USER_DIR);
        // Find current location of resources
        final String gnmiConfig;
        if (currentFolder.length() > 6 && currentFolder.endsWith(LIGHTY)) {
            currentFolder += ASSEMBLY_RESOURCES;
            // Use different gNMI configuration when is example start in IDEA
            gnmiConfig = ASSEMBLY_RESOURCES + GNMI_CONFIGURATION;
        } else {
            gnmiConfig = currentFolder + GNMI_CONFIGURATION;
        }

        GnmiSimulatorApp.main(new String[]{currentFolder});
        RCgNMIApp.main(new String[]{"-c", gnmiConfig});
    }

    private Main() {
        throw new UnsupportedOperationException();
    }
}
