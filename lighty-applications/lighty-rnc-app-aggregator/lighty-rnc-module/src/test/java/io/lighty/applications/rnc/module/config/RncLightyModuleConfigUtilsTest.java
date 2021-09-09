/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.config;


import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.DatastoreConfigurationUtils;
import io.lighty.modules.northbound.restconf.community.impl.config.JsonRestConfServiceType;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.testng.annotations.Test;

public class RncLightyModuleConfigUtilsTest {

    @Test
    public void testLoadConfigFromFile() throws ConfigurationException, URISyntaxException {
        Path configPath = Paths.get(this.getClass().getResource("/config.json").toURI());
        RncLightyModuleConfiguration config = RncLightyModuleConfigUtils.loadConfigFromFile(configPath);
        final RncRestConfConfiguration restconfConfig = config.getRestconfConfig();

        assertEquals(restconfConfig.getInetAddress().getCanonicalHostName(), "0.0.0.1");
        assertEquals(restconfConfig.getWebSocketPort(), 8181);
        assertEquals(restconfConfig.getHttpPort(), 8181);
        assertEquals(restconfConfig.getRestconfServletContextPath(), "/rests");
        assertEquals(restconfConfig.getJsonRestconfServiceType(), JsonRestConfServiceType.DRAFT_02);
        assertFalse(restconfConfig.isUseHttps());
        assertEquals(restconfConfig.getKeyStoreFilePath(), "src/test/resources/keystore/KeyStore.jks");
        checkDefaultKeystoreConfig(restconfConfig);

        checkDefaultAAAconfig(config);

        final ControllerConfiguration controllerConfig = config.getControllerConfig();

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

        final NetconfConfiguration netconfConfig = config.getNetconfConfig();

        assertEquals(netconfConfig.getTopologyId(), "topology-netconf-test");
        assertEquals(netconfConfig.getWriteTxTimeout(), 0);
        assertFalse(netconfConfig.isClusterEnabled());
    }

    @Test
    public void testLoadDefaultConfig() throws ConfigurationException {
        RncLightyModuleConfiguration config = RncLightyModuleConfigUtils.loadDefaultConfig();
        final RncRestConfConfiguration restconfConfig = config.getRestconfConfig();

        assertEquals(restconfConfig.getInetAddress().getCanonicalHostName(), "0.0.0.0");
        assertEquals(restconfConfig.getWebSocketPort(), 8185);
        assertEquals(restconfConfig.getHttpPort(), 8888);
        assertEquals(restconfConfig.getRestconfServletContextPath(), "/restconf");
        assertEquals(restconfConfig.getJsonRestconfServiceType(), JsonRestConfServiceType.DRAFT_18);
        assertFalse(restconfConfig.isUseHttps());
        assertEquals(restconfConfig.getKeyStoreFilePath(), "keystore/lightyio.jks");
        checkDefaultKeystoreConfig(restconfConfig);

        checkDefaultAAAconfig(config);

        final ControllerConfiguration controllerConfig = config.getControllerConfig();

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

        final NetconfConfiguration netconfConfig = config.getNetconfConfig();
        assertEquals(netconfConfig.getTopologyId(), "topology-netconf");
        assertEquals(netconfConfig.getWriteTxTimeout(), 0);
        assertFalse(netconfConfig.isClusterEnabled());
    }

    private void checkDefaultAAAconfig(RncLightyModuleConfiguration config) {
        final RncAAAConfiguration aaaConfig = config.getAaaConfig();

        assertEquals(aaaConfig.getMoonEndpointPath(), "/moon");
        assertEquals(aaaConfig.getDbPassword(), "bar");
        assertEquals(aaaConfig.getDbUsername(), "foo");
        assertFalse(aaaConfig.isEnableAAA());
    }

    private void checkDefaultKeystoreConfig(RncRestConfConfiguration restconfConfig) {
        assertFalse(restconfConfig.getKeyStorePassword().isEmpty());
        assertEquals(restconfConfig.getKeyStoreType(), "JKS");
    }
}
