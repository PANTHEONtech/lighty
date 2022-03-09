/*
 * Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.util;

import static org.testng.Assert.assertEquals;

import io.lighty.core.controller.impl.config.ConfigurationException;
import org.junit.jupiter.api.Test;

public class ModulesConfigTest {

    @Test
    void loadJsonConfig() throws ConfigurationException {
        final var config = ModulesConfig.getModulesConfig(this.getClass().getClassLoader()
                .getResourceAsStream("sampleModulesConfig.json"));

        assertEquals(config.getModuleTimeoutSeconds(), 180);
    }

    @Test
    void loadMissingConfig() throws ConfigurationException {
        final var config = ModulesConfig.getModulesConfig(this.getClass().getClassLoader()
                .getResourceAsStream("missingModulesConfig.json"));

        assertEquals(config.getModuleTimeoutSeconds(), 60);
    }
}
