/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.applications.rcgnmi.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.applications.util.ModulesConfig;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.DatastoreConfigurationUtils;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RcGnmiAppModuleConfigUtilsTest {

    private static final String CONFIG_PATH = "src/test/resources/config.json";
    private static final String EMPTY_CONFIG_PATH = "src/test/resources/config_empty.json";

    @Test
    void testConfigLoadedJson() throws IOException, ConfigurationException {
        final RcGnmiAppConfiguration rcGnmiAppConfiguration = RcGnmiAppModuleConfigUtils.loadConfiguration(
                Path.of(CONFIG_PATH));
        // Assert restconf config
        final RestConfConfiguration restconfConfig = rcGnmiAppConfiguration.getRestconfConfig();
        assertEquals("0.0.0.1", restconfConfig.getInetAddress().getCanonicalHostName());
        assertEquals(8181, restconfConfig.getHttpPort());
        assertEquals("rests", restconfConfig.getRestconfServletContextPath());
        // Assert gnmi config
        final ControllerConfiguration controllerConfig = rcGnmiAppConfiguration.getControllerConfig();
        assertEquals("./clustered-datastore-restore-test", controllerConfig.getRestoreDirectoryPath());
        assertEquals(20, controllerConfig.getMaxDataBrokerFutureCallbackPoolSize());
        assertEquals(2000, controllerConfig.getMaxDataBrokerFutureCallbackQueueSize());
        assertTrue(controllerConfig.isMetricCaptureEnabled());
        assertEquals(2000, controllerConfig.getMailboxCapacity());
        assertEquals("module-shards.conf", controllerConfig.getModuleShardsConfig());
        assertEquals("modules.conf", controllerConfig.getModulesConfig());
        assertEquals(new ControllerConfiguration.DOMNotificationRouterConfig(),
            controllerConfig.getDomNotificationRouterConfig());
        assertTrue(controllerConfig.getDistributedEosProperties().isEmpty());
        assertEquals(DatastoreConfigurationUtils.getDefaultDatastoreProperties(),
            controllerConfig.getDatastoreProperties());
        final ModulesConfig modulesConfig = rcGnmiAppConfiguration.getModulesConfig();
        assertEquals(180, modulesConfig.getModuleTimeoutSeconds());
    }

    @Test
    void testDefaultConfigLoadedEmptyJson() throws IOException, ConfigurationException {
        final RcGnmiAppConfiguration rcGnmiAppConfiguration = RcGnmiAppModuleConfigUtils.loadConfiguration(
                Path.of(EMPTY_CONFIG_PATH));
        // Assert restconf config
        final RestConfConfiguration restconfConfig = rcGnmiAppConfiguration.getRestconfConfig();
        assertEquals("0.0.0.0", restconfConfig.getInetAddress().getCanonicalHostName());
        assertEquals(8888, restconfConfig.getHttpPort());
        assertEquals("restconf", restconfConfig.getRestconfServletContextPath());
        // Assert controller config
        final ControllerConfiguration controllerConfig = rcGnmiAppConfiguration.getControllerConfig();
        assertEquals("./clustered-datastore-restore", controllerConfig.getRestoreDirectoryPath());
        assertEquals(10, controllerConfig.getMaxDataBrokerFutureCallbackPoolSize());
        assertEquals(1000, controllerConfig.getMaxDataBrokerFutureCallbackQueueSize());
        assertFalse(controllerConfig.isMetricCaptureEnabled());
        assertEquals(1000, controllerConfig.getMailboxCapacity());
        assertEquals("configuration/initial/module-shards.conf", controllerConfig.getModuleShardsConfig());
        assertEquals("configuration/initial/modules.conf", controllerConfig.getModulesConfig());
        assertEquals(new ControllerConfiguration.DOMNotificationRouterConfig(),
            controllerConfig.getDomNotificationRouterConfig());
        assertTrue(controllerConfig.getDistributedEosProperties().isEmpty());
        assertEquals(DatastoreConfigurationUtils.getDefaultDatastoreProperties(),
            controllerConfig.getDatastoreProperties());
        final ModulesConfig modulesConfig = rcGnmiAppConfiguration.getModulesConfig();
        assertEquals(60, modulesConfig.getModuleTimeoutSeconds());
    }


    @Test
    void testConfigLoadedDefaultConfig() throws ConfigurationException {
        final RcGnmiAppConfiguration rcGnmiAppConfiguration = RcGnmiAppModuleConfigUtils.loadDefaultConfig();
        // Assert restconf config
        final RestConfConfiguration restconfConfig = rcGnmiAppConfiguration.getRestconfConfig();
        assertEquals("0.0.0.0", restconfConfig.getInetAddress().getCanonicalHostName());
        assertEquals(8888, restconfConfig.getHttpPort());
        assertEquals("restconf", restconfConfig.getRestconfServletContextPath());
        // Assert controller config
        final ControllerConfiguration controllerConfig = rcGnmiAppConfiguration.getControllerConfig();
        assertEquals("./clustered-datastore-restore", controllerConfig.getRestoreDirectoryPath());
        assertEquals(10, controllerConfig.getMaxDataBrokerFutureCallbackPoolSize());
        assertEquals(1000, controllerConfig.getMaxDataBrokerFutureCallbackQueueSize());
        assertFalse(controllerConfig.isMetricCaptureEnabled());
        assertEquals(1000, controllerConfig.getMailboxCapacity());
        assertEquals("configuration/initial/module-shards.conf", controllerConfig.getModuleShardsConfig());
        assertEquals("configuration/initial/modules.conf", controllerConfig.getModulesConfig());
        assertEquals(new ControllerConfiguration.DOMNotificationRouterConfig(),
            controllerConfig.getDomNotificationRouterConfig());
        assertTrue(controllerConfig.getDistributedEosProperties().isEmpty());
        assertEquals(DatastoreConfigurationUtils.getDefaultDatastoreProperties(),
            controllerConfig.getDatastoreProperties());
        final ModulesConfig modulesConfig = rcGnmiAppConfiguration.getModulesConfig();
        assertEquals(60, modulesConfig.getModuleTimeoutSeconds());
    }

}
