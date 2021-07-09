/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.config.util;

import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertEquals;

import io.lighty.applications.rnc.module.config.RncAAAConfiguration;
import io.lighty.core.controller.impl.config.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.testng.annotations.Test;

public class AAAConfigUtilsTest {

    @Test(expectedExceptions = ConfigurationException.class)
    public void testNotAllowedToCreateAAAConfigUtils() throws ConfigurationException {
        final InputStream configStream = mock(InputStream.class);
        AAAConfigUtils.getAAAConfiguration(configStream);
    }

    @Test
    public void testGetAAAConfigurationEmptyConfigFile() throws URISyntaxException, IOException,
            ConfigurationException {
        Path configPath = Paths.get(this.getClass().getResource("/config_empty.json").toURI());
        final RncAAAConfiguration rncAaaConfiguration =
                AAAConfigUtils.getAAAConfiguration(Files.newInputStream(configPath));

        assertEquals(rncAaaConfiguration.getMoonEndpointPath(), "/moon");
        assertEquals(rncAaaConfiguration.getDbPassword(), "bar");
        assertEquals(rncAaaConfiguration.getDbUsername(), "foo");
    }
}
