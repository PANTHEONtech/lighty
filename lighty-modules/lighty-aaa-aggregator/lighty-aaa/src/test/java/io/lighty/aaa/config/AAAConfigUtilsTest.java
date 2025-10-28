/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa.config;

import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import io.lighty.aaa.util.AAAConfigUtils;
import io.lighty.core.controller.impl.config.ConfigurationException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.testng.annotations.Test;

public class AAAConfigUtilsTest {

    @Test(expectedExceptions = ConfigurationException.class)
    public void testNotAllowedToCreateAAAConfigUtils() throws Exception {
        final var configStream = mock(InputStream.class);
        AAAConfigUtils.getAAAConfiguration(configStream);
    }

    @Test
    public void testGetAAAConfigurationEmptyConfigFile() throws Exception {
        final var configPath = Paths.get(this.getClass().getResource("/configEmpty.json").toURI());
        final var rncAaaConfiguration = AAAConfigUtils.getAAAConfiguration(Files.newInputStream(configPath));

        assertFalse(rncAaaConfiguration.isEnableAAA());
        assertEquals(rncAaaConfiguration.getMoonEndpointPath(), "/moon");
        assertEquals(rncAaaConfiguration.getDbPassword(), "bar");
        assertEquals(rncAaaConfiguration.getDbUsername(), "foo");
        assertEquals(rncAaaConfiguration.getDbPath(), "./data");
        assertEquals(rncAaaConfiguration.getUsername(), "admin");
        assertEquals(rncAaaConfiguration.getPassword(), "admin");
    }

    @Test
    public void testGetAAAConfigurationCustomConfigFile() throws Exception {
        final var configPath = Paths.get(this.getClass().getResource("/aaaConfig.json").toURI());
        final var rncAaaConfiguration = AAAConfigUtils.getAAAConfiguration(Files.newInputStream(configPath));

        assertTrue(rncAaaConfiguration.isEnableAAA());
        assertEquals(rncAaaConfiguration.getMoonEndpointPath(), "/moon");
        assertEquals(rncAaaConfiguration.getDbPassword(), "Password");
        assertEquals(rncAaaConfiguration.getDbUsername(), "Username");
        assertEquals(rncAaaConfiguration.getDbPath(), "./testData");
        assertEquals(rncAaaConfiguration.getUsername(), "Admin");
        assertEquals(rncAaaConfiguration.getPassword(), "Admin");
    }
}
