/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.applications.rcgnmi.module;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import io.lighty.applications.util.ModulesConfig;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.DatastoreConfigurationUtils;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RcGnmiAppModuleConfigUtilsTest {

    private static final String CONFIG_PATH = "src/test/resources/config.json";
    private static final String EMPTY_CONFIG_PATH = "src/test/resources/config_empty.json";

    @Test
    public void testConfigLoadedJson() throws IOException, ConfigurationException {
        final RcGnmiAppConfiguration rcGnmiAppConfiguration = RcGnmiAppModuleConfigUtils.loadConfiguration(
                Path.of(CONFIG_PATH));
        // Assert restconf config
        final RestConfConfiguration restconfConfig = rcGnmiAppConfiguration.getRestconfConfig();
        assertEquals(restconfConfig.getInetAddress().getCanonicalHostName(), "0.0.0.1");
        assertEquals(restconfConfig.getHttpPort(), 8181);
        assertEquals(restconfConfig.getRestconfServletContextPath(), "rests");
        // Assert gnmi config
        final GnmiConfiguration gnmiConfiguration = rcGnmiAppConfiguration.getGnmiConfiguration();
        Assertions.assertEquals(5, gnmiConfiguration.getInitialYangsPaths().size());
        // Assert controller config
        final ControllerConfiguration controllerConfig = rcGnmiAppConfiguration.getControllerConfig();
        assertEquals(controllerConfig.getRestoreDirectoryPath(), "./clustered-datastore-restore-test");
        assertEquals(controllerConfig.getMaxDataBrokerFutureCallbackPoolSize(), 20);
        assertEquals(controllerConfig.getMaxDataBrokerFutureCallbackQueueSize(), 2000);
        assertTrue(controllerConfig.isMetricCaptureEnabled());
        assertEquals(controllerConfig.getMailboxCapacity(), 2000);
        assertEquals(controllerConfig.getModuleShardsConfig(), "module-shards.conf");
        assertEquals(controllerConfig.getModulesConfig(), "modules.conf");
        assertEquals(controllerConfig.getDomNotificationRouterConfig(),
                new ControllerConfiguration.DOMNotificationRouterConfig());
        assertTrue(controllerConfig.getDistributedEosProperties().isEmpty());
        assertEquals(controllerConfig.getDatastoreProperties(),
                DatastoreConfigurationUtils.getDefaultDatastoreProperties());
        final ModulesConfig modulesConfig = rcGnmiAppConfiguration.getModulesConfig();
        assertEquals(180, modulesConfig.getModuleTimeoutSeconds());
    }

    @Test
    public void testDefaultConfigLoadedEmptyJson() throws IOException, ConfigurationException {
        final RcGnmiAppConfiguration rcGnmiAppConfiguration = RcGnmiAppModuleConfigUtils.loadConfiguration(
                Path.of(EMPTY_CONFIG_PATH));
        // Assert restconf config
        final RestConfConfiguration restconfConfig = rcGnmiAppConfiguration.getRestconfConfig();
        assertEquals(restconfConfig.getInetAddress().getCanonicalHostName(), "0.0.0.0");
        assertEquals(restconfConfig.getHttpPort(), 8888);
        assertEquals(restconfConfig.getRestconfServletContextPath(), "restconf");
        // Assert gnmi config
        final GnmiConfiguration gnmiConfiguration = rcGnmiAppConfiguration.getGnmiConfiguration();
        Assertions.assertTrue(gnmiConfiguration.getInitialYangsPaths().isEmpty());
        // Assert controller config
        final ControllerConfiguration controllerConfig = rcGnmiAppConfiguration.getControllerConfig();
        assertEquals(controllerConfig.getRestoreDirectoryPath(), "./clustered-datastore-restore");
        assertEquals(controllerConfig.getMaxDataBrokerFutureCallbackPoolSize(), 10);
        assertEquals(controllerConfig.getMaxDataBrokerFutureCallbackQueueSize(), 1000);
        assertFalse(controllerConfig.isMetricCaptureEnabled());
        assertEquals(controllerConfig.getMailboxCapacity(), 1000);
        assertEquals(controllerConfig.getModuleShardsConfig(), "configuration/initial/module-shards.conf");
        assertEquals(controllerConfig.getModulesConfig(), "configuration/initial/modules.conf");
        assertEquals(controllerConfig.getDomNotificationRouterConfig(),
                new ControllerConfiguration.DOMNotificationRouterConfig());
        assertTrue(controllerConfig.getDistributedEosProperties().isEmpty());
        assertEquals(controllerConfig.getDatastoreProperties(),
                DatastoreConfigurationUtils.getDefaultDatastoreProperties());
        final ModulesConfig modulesConfig = rcGnmiAppConfiguration.getModulesConfig();
        assertEquals(60, modulesConfig.getModuleTimeoutSeconds());
    }


    @Test
    public void testConfigLoadedDefaultConfig() throws ConfigurationException {
        final RcGnmiAppConfiguration rcGnmiAppConfiguration = RcGnmiAppModuleConfigUtils.loadDefaultConfig();
        // Assert restconf config
        final RestConfConfiguration restconfConfig = rcGnmiAppConfiguration.getRestconfConfig();
        assertEquals(restconfConfig.getInetAddress().getCanonicalHostName(), "0.0.0.0");
        assertEquals(restconfConfig.getHttpPort(), 8888);
        assertEquals(restconfConfig.getRestconfServletContextPath(), "restconf");
        // Assert gnmi config
        final GnmiConfiguration gnmiConfiguration = rcGnmiAppConfiguration.getGnmiConfiguration();
        Assertions.assertTrue(gnmiConfiguration.getInitialYangsPaths().isEmpty());
        // Assert controller config
        final ControllerConfiguration controllerConfig = rcGnmiAppConfiguration.getControllerConfig();
        assertEquals(controllerConfig.getRestoreDirectoryPath(), "./clustered-datastore-restore");
        assertEquals(controllerConfig.getMaxDataBrokerFutureCallbackPoolSize(), 10);
        assertEquals(controllerConfig.getMaxDataBrokerFutureCallbackQueueSize(), 1000);
        assertFalse(controllerConfig.isMetricCaptureEnabled());
        assertEquals(controllerConfig.getMailboxCapacity(), 1000);
        assertEquals(controllerConfig.getModuleShardsConfig(), "configuration/initial/module-shards.conf");
        assertEquals(controllerConfig.getModulesConfig(), "configuration/initial/modules.conf");
        assertEquals(controllerConfig.getDomNotificationRouterConfig(),
                new ControllerConfiguration.DOMNotificationRouterConfig());
        assertTrue(controllerConfig.getDistributedEosProperties().isEmpty());
        assertEquals(controllerConfig.getDatastoreProperties(),
                DatastoreConfigurationUtils.getDefaultDatastoreProperties());
        final ModulesConfig modulesConfig = rcGnmiAppConfiguration.getModulesConfig();
        assertEquals(60, modulesConfig.getModuleTimeoutSeconds());
    }

}
