/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.applications.util.ModulesConfig;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.DatastoreConfigurationUtils;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class RncLightyModuleConfigUtilsTest {

    @Test
    void testLoadConfigFromFile() throws ConfigurationException, URISyntaxException {
        final var configPath = Paths.get(this.getClass().getResource("/config.json").toURI());
        var rncConfig = RncLightyModuleConfigUtils.loadConfigFromFile(configPath);

        // Test Restconf configuration
        final var restconfConfig = rncConfig.getRestconfConfig();
        assertEquals("0.0.0.1", restconfConfig.getInetAddress().getCanonicalHostName());
        assertEquals(8181, restconfConfig.getHttpPort());
        assertEquals("rests", restconfConfig.getRestconfServletContextPath());

        // Test Server configuration
        final var serverConfig = rncConfig.getServerConfig();
        assertFalse(serverConfig.isUseHttps());
        assertTrue(serverConfig.isUseHttp2());
        assertEquals("src/test/resources/keystore/KeyStore.jks", serverConfig.getKeyStoreFilePath());
        assertFalse(serverConfig.getKeyStorePassword().isEmpty());
        assertEquals("JKS", serverConfig.getKeyStoreType());

        // Test AAA configuration
        final var aaaConfig = rncConfig.getAaaConfig();
        assertEquals("/moon", aaaConfig.getMoonEndpointPath());
        assertEquals("bar", aaaConfig.getDbPassword());
        assertEquals("foo", aaaConfig.getDbUsername());
        assertFalse(aaaConfig.isEnableAAA());

        // Test Controller configuration
        final var controllerConfig = rncConfig.getControllerConfig();
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

        // Test Netconf configuration
        final var netconfConfig = rncConfig.getNetconfConfig();
        assertEquals("topology-netconf-test", netconfConfig.getTopologyId());
        assertEquals(0, netconfConfig.getWriteTxTimeout());
        assertFalse(netconfConfig.isClusterEnabled());

        final ModulesConfig moduleConfig = rncConfig.getModuleConfig();
        assertEquals(180, moduleConfig.getModuleTimeoutSeconds());
    }

    @Test
    void testLoadDefaultConfig() throws ConfigurationException {
        final var rncConfig = RncLightyModuleConfigUtils.loadDefaultConfig();

        // Test Restconf configuration
        final var restconfConfig = rncConfig.getRestconfConfig();
        assertEquals("localhost", restconfConfig.getInetAddress().getCanonicalHostName());
        assertEquals(8888, restconfConfig.getHttpPort());
        assertEquals("restconf", restconfConfig.getRestconfServletContextPath());

        // Test Server configuration
        final var serverConfig = rncConfig.getServerConfig();
        assertFalse(serverConfig.isUseHttps());
        assertEquals("keystore/lightyio.jks", serverConfig.getKeyStoreFilePath());
        assertFalse(serverConfig.getKeyStorePassword().isEmpty());
        assertEquals("JKS", serverConfig.getKeyStoreType());

        // Test AAA configuration
        final var aaaConfig = rncConfig.getAaaConfig();
        assertEquals("/moon", aaaConfig.getMoonEndpointPath());
        assertEquals("bar", aaaConfig.getDbPassword());
        assertEquals("foo", aaaConfig.getDbUsername());
        assertFalse(aaaConfig.isEnableAAA());

        // Test Controller configuration
        final var controllerConfig = rncConfig.getControllerConfig();
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

        // Test Netconf configuration
        final var netconfConfig = rncConfig.getNetconfConfig();
        assertEquals("topology-netconf", netconfConfig.getTopologyId());
        assertEquals(0, netconfConfig.getWriteTxTimeout());
        assertFalse(netconfConfig.isClusterEnabled());

        final ModulesConfig moduleConfig = rncConfig.getModuleConfig();
        assertEquals(60, moduleConfig.getModuleTimeoutSeconds());
    }
}
