/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.config;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import io.lighty.applications.util.ModulesConfig;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.DatastoreConfigurationUtils;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.testng.annotations.Test;

class RncLightyModuleConfigUtilsTest {

    @Test
    void testLoadConfigFromFile() throws ConfigurationException, URISyntaxException {
        final var configPath = Paths.get(this.getClass().getResource("/config.json").toURI());
        var rncConfig = RncLightyModuleConfigUtils.loadConfigFromFile(configPath);

        // Test Restconf configuration
        final var restconfConfig = rncConfig.getRestconfConfig();
        assertEquals(restconfConfig.getInetAddress().getCanonicalHostName(), "0.0.0.1");
        assertEquals(restconfConfig.getHttpPort(), 8181);
        assertEquals(restconfConfig.getRestconfServletContextPath(), "rests");

        // Test Server configuration
        final var serverConfig = rncConfig.getServerConfig();
        assertFalse(serverConfig.isUseHttps());
        assertTrue(serverConfig.isUseHttp2());
        assertEquals(serverConfig.getKeyStoreFilePath(), "src/test/resources/keystore/KeyStore.jks");
        assertFalse(serverConfig.getKeyStorePassword().isEmpty());
        assertEquals(serverConfig.getKeyStoreType(), "JKS");

        // Test AAA configuration
        final var aaaConfig = rncConfig.getAaaConfig();
        assertEquals(aaaConfig.getMoonEndpointPath(), "/moon");
        assertEquals(aaaConfig.getDbPassword(), "bar");
        assertEquals(aaaConfig.getDbUsername(), "foo");
        assertFalse(aaaConfig.isEnableAAA());

        // Test Controller configuration
        final var controllerConfig = rncConfig.getControllerConfig();
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

        // Test Netconf configuration
        final var netconfConfig = rncConfig.getNetconfConfig();
        assertEquals(netconfConfig.getTopologyId(), "topology-netconf-test");
        assertEquals(netconfConfig.getWriteTxTimeout(), 0);
        assertFalse(netconfConfig.isClusterEnabled());

        final ModulesConfig moduleConfig = rncConfig.getModuleConfig();
        assertEquals(moduleConfig.getModuleTimeoutSeconds(), 180);
    }

    @Test
    void testLoadDefaultConfig() throws ConfigurationException {
        final var rncConfig = RncLightyModuleConfigUtils.loadDefaultConfig();

        // Test Restconf configuration
        final var restconfConfig = rncConfig.getRestconfConfig();
        assertEquals(restconfConfig.getInetAddress().getCanonicalHostName(), "localhost");
        assertEquals(restconfConfig.getHttpPort(), 8888);
        assertEquals(restconfConfig.getRestconfServletContextPath(), "restconf");

        // Test Server configuration
        final var serverConfig = rncConfig.getServerConfig();
        assertFalse(serverConfig.isUseHttps());
        assertEquals(serverConfig.getKeyStoreFilePath(), "keystore/lightyio.jks");
        assertFalse(serverConfig.getKeyStorePassword().isEmpty());
        assertEquals(serverConfig.getKeyStoreType(), "JKS");

        // Test AAA configuration
        final var aaaConfig = rncConfig.getAaaConfig();
        assertEquals(aaaConfig.getMoonEndpointPath(), "/moon");
        assertEquals(aaaConfig.getDbPassword(), "bar");
        assertEquals(aaaConfig.getDbUsername(), "foo");
        assertFalse(aaaConfig.isEnableAAA());

        // Test Controller configuration
        final var controllerConfig = rncConfig.getControllerConfig();
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

        // Test Netconf configuration
        final var netconfConfig = rncConfig.getNetconfConfig();
        assertEquals(netconfConfig.getTopologyId(), "topology-netconf");
        assertEquals(netconfConfig.getWriteTxTimeout(), 0);
        assertFalse(netconfConfig.isClusterEnabled());

        final ModulesConfig moduleConfig = rncConfig.getModuleConfig();
        assertEquals(moduleConfig.getModuleTimeoutSeconds(), 60);
    }
}
