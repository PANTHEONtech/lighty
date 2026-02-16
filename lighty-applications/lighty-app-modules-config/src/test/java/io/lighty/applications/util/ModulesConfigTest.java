/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import io.lighty.core.controller.impl.config.ConfigurationException;
import org.junit.jupiter.api.Test;

class ModulesConfigTest {

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

    @Test
    void configEqualsTest() {
        final ModulesConfig firstConfig = new ModulesConfig();
        firstConfig.setModuleTimeoutSeconds(30);
        final ModulesConfig secondConfig = new ModulesConfig();
        secondConfig.setModuleTimeoutSeconds(60);

        assertFalse(firstConfig.equals(secondConfig));
        secondConfig.setModuleTimeoutSeconds(30);
        assertTrue(firstConfig.equals(secondConfig));
    }

    @Test
    void configHashCodeTest() {
        final ModulesConfig firstConfig = new ModulesConfig();
        firstConfig.setModuleTimeoutSeconds(30);
        final ModulesConfig secondConfig = new ModulesConfig();
        secondConfig.setModuleTimeoutSeconds(60);

        assertNotEquals(firstConfig.hashCode(), secondConfig.hashCode());
        secondConfig.setModuleTimeoutSeconds(30);
        assertEquals(firstConfig.hashCode(), secondConfig.hashCode());
    }
}
