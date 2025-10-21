/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.lightymodule;

import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import io.lighty.gnmi.southbound.lightymodule.util.GnmiConfigUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GnmiConfigUtilsTest {

    private static final String CONFIG_PATH = "src/test/resources/lightyconfigs/config.json";
    private static final String EMPTY_CONFIG_PATH = "src/test/resources/lightyconfigs/config_empty.json";

    @Test
    public void testConfigLoadedJson() throws IOException, ConfigurationException {
        final GnmiConfiguration gnmiConfiguration = GnmiConfigUtils.getGnmiConfiguration(
                Files.newInputStream(Path.of(CONFIG_PATH)));

        Assertions.assertEquals(5, gnmiConfiguration.getInitialYangsPaths().size());
    }

    @Test
    public void testConfigLoadedDefaultConfig() throws IOException, ConfigurationException {
        final GnmiConfiguration gnmiConfiguration = GnmiConfigUtils.getGnmiConfiguration(
                Files.newInputStream(Path.of(EMPTY_CONFIG_PATH)));

        Assertions.assertTrue(gnmiConfiguration.getInitialYangsPaths().isEmpty());
    }

}
