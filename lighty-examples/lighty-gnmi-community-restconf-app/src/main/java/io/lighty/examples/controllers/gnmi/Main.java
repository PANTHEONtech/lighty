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
    private static final String TEST_CONFIG = "/src/test/resources/example_config.json";
    private static final String PATH_TO_ASSEMBLY_RESOURCE = "/src/main/assembly/resources";
    private static final String MODULE_NAME = "lighty-gnmi-community-restconf-app";
    private static final String ASSEMBLY_RESOURCES
            = "/lighty-examples/lighty-gnmi-community-restconf-app/src/main/assembly/resources";
    private static final String LIGHTY = "lighty";
    private static final String USER_DIR = "user.dir";

    public static void main(String[] args) throws IOException {
        String currentFolder = System.getProperty(USER_DIR);
        // Find current location of resources
        final String gnmiConfig;
        if (currentFolder.length() > 6 && currentFolder.endsWith(LIGHTY)) {
            // Example stared by IDEA
            currentFolder += ASSEMBLY_RESOURCES;
            // Specific gNMI configuration for IDEA run
            gnmiConfig = currentFolder + GNMI_CONFIGURATION;
        } else if (currentFolder.endsWith(MODULE_NAME)) {
            // Example run by tests
            // Specific gNMI config for tests
            gnmiConfig = currentFolder + TEST_CONFIG;
            currentFolder += PATH_TO_ASSEMBLY_RESOURCE;
        }
        else {
            // Example stared from jar file
            gnmiConfig = currentFolder + GNMI_CONFIGURATION;
        }

        GnmiSimulatorApp.main(new String[]{currentFolder});
        RCgNMIApp.main(new String[]{"-c", gnmiConfig});
    }

    private Main() {
        throw new UnsupportedOperationException();
    }
}
