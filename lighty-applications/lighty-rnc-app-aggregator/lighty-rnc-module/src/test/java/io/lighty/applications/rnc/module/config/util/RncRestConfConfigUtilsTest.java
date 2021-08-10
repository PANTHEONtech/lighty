/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.config.util;

import static org.testng.Assert.assertNotNull;

import io.lighty.applications.rnc.module.config.RncLightyModuleConfigUtils;
import io.lighty.applications.rnc.module.config.RncRestConfConfiguration;
import io.lighty.applications.rnc.module.config.SecurityConfig;
import io.lighty.core.controller.impl.config.ConfigurationException;
import org.eclipse.jetty.http.HttpVersion;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RncRestConfConfigUtilsTest {
    private static RncRestConfConfiguration configDefault;
    private static RncRestConfConfiguration configFromFile;

    @BeforeClass
    public static void init() throws ConfigurationException {
        configDefault = new RncRestConfConfiguration(RncRestConfConfigUtils.getDefaultRestConfConfiguration());
        configFromFile = new RncRestConfConfiguration(RncRestConfConfigUtils.getRestConfConfiguration(
            RncLightyModuleConfigUtils.class.getClassLoader().getResourceAsStream("config.json")));
    }

    @Test
    public void testCreateSecurityConfigDefault() throws ConfigurationException {
        final SecurityConfig security = RncRestConfConfigUtils.createSecurityConfig(configDefault);
        assertNotNull(security.getSslConnectionFactory(HttpVersion.HTTP_1_1.asString()));
    }

    @Test
    public void testCreateSecurityConfigFromFile() throws ConfigurationException {
        final SecurityConfig security = RncRestConfConfigUtils.createSecurityConfig(configFromFile);
        assertNotNull(security.getSslConnectionFactory(HttpVersion.HTTP_1_1.asString()));
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testCreateSecurityConfigFileNotFound() throws ConfigurationException {
        final RncRestConfConfiguration conf = new RncRestConfConfiguration(configDefault);
        conf.setKeyStoreFilePath("");
        RncRestConfConfigUtils.createSecurityConfig(conf);
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testCreateSecurityConfigInvalidPassword() throws ConfigurationException {
        final RncRestConfConfiguration conf = new RncRestConfConfiguration(configDefault);
        conf.setKeyStorePassword("");
        RncRestConfConfigUtils.createSecurityConfig(conf);
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testCreateSecurityConfigInvalidKeyStoreType() throws ConfigurationException {
        final RncRestConfConfiguration conf = new RncRestConfConfiguration(configDefault);
        conf.setKeyStoreType("");
        RncRestConfConfigUtils.createSecurityConfig(conf);
    }
}
