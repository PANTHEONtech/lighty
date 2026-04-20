/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.core.controller.impl.util.DatastoreConfigurationUtils;
import io.lighty.core.controller.impl.util.FileToDatastoreUtils;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;

class ConfigLoadingTest {

    @Test
    void loadControllerConfigurationNoDsContexts() throws IOException, ConfigurationException {
        InputStream inputStream = this.getClass().getResourceAsStream("/testLightyControllerConfig-noDsContexts.json");
        final ControllerConfiguration configuration = ControllerConfigUtils.getConfiguration(inputStream);

        assertNotNull(configuration);
        assertNotNull(configuration.getConfigDatastoreContext());
        assertNotNull(configuration.getOperDatastoreContext());

        DatastoreContext configDatastoreContext = configuration.getConfigDatastoreContext();
        assertEquals(Duration.of(10, ChronoUnit.MINUTES),
            configDatastoreContext.getShardTransactionIdleTimeout());
        assertEquals(5000, configDatastoreContext.getOperationTimeoutInMillis());
        assertEquals(30, configDatastoreContext.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(1, configDatastoreContext.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(20000, configDatastoreContext.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(12, configDatastoreContext.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(Duration.of(500, ChronoUnit.MILLIS),
            configDatastoreContext.getShardRaftConfig().getHeartBeatInterval());
        assertEquals(50000, configDatastoreContext.getShardTransactionCommitQueueCapacity());
        assertEquals(Duration.of(300000, ChronoUnit.MILLIS),
            configDatastoreContext.getShardInitializationTimeout());
        assertEquals(Duration.of(30000, ChronoUnit.MILLIS),
            configDatastoreContext.getShardLeaderElectionTimeout());
        assertTrue(configDatastoreContext.isPersistent());
        assertEquals(1000, configDatastoreContext.getShardBatchedModificationCount());
        assertEquals(120000, configDatastoreContext.getShardCommitQueueExpiryTimeoutInMillis());
        assertFalse(configDatastoreContext.isTransactionDebugContextEnabled());
        assertEquals(491520, configDatastoreContext.getMaximumMessageSliceSize());
        assertEquals(134217728, configDatastoreContext.getFileBackedStreamingThreshold());
        assertEquals(10, configDatastoreContext.getShardRaftConfig().getSyncIndexThreshold());
        assertEquals(30000000000L, configDatastoreContext.getBackendAlivenessTimerInterval());
        assertEquals(120000000000L, configDatastoreContext.getRequestTimeout());
        assertEquals(900000000000L, configDatastoreContext.getNoProgressTimeout());

        DatastoreContext operDatastoreContext = configuration.getOperDatastoreContext();
        assertEquals(Duration.of(10, ChronoUnit.MINUTES),
            operDatastoreContext.getShardTransactionIdleTimeout());
        assertEquals(5000, operDatastoreContext.getOperationTimeoutInMillis());
        assertEquals(30, operDatastoreContext.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(1, operDatastoreContext.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(20000, operDatastoreContext.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(12, operDatastoreContext.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(Duration.of(500, ChronoUnit.MILLIS),
            operDatastoreContext.getShardRaftConfig().getHeartBeatInterval());
        assertEquals(50000, operDatastoreContext.getShardTransactionCommitQueueCapacity());
        assertEquals(Duration.of(300000, ChronoUnit.MILLIS),
            operDatastoreContext.getShardInitializationTimeout());
        assertEquals(Duration.of(30000, ChronoUnit.MILLIS),
            operDatastoreContext.getShardLeaderElectionTimeout());
        assertTrue(!operDatastoreContext.isPersistent());
        assertEquals(1000, operDatastoreContext.getShardBatchedModificationCount());
        assertEquals(120000, operDatastoreContext.getShardCommitQueueExpiryTimeoutInMillis());
        assertTrue(!operDatastoreContext.isTransactionDebugContextEnabled());
        assertEquals(491520, operDatastoreContext.getMaximumMessageSliceSize());
        assertEquals(134217728, operDatastoreContext.getFileBackedStreamingThreshold());
        assertEquals(10, operDatastoreContext.getShardRaftConfig().getSyncIndexThreshold());
        assertEquals(30000000000L, operDatastoreContext.getBackendAlivenessTimerInterval());
        assertEquals(120000000000L, operDatastoreContext.getRequestTimeout());
        assertEquals(900000000000L, operDatastoreContext.getNoProgressTimeout());

        assertNull(configuration.getInitialConfigData());
    }

    @Test
    void loadMissingInitConfigParam() {
        InputStream inputStream = this.getClass()
                .getResourceAsStream("/testLightyControllerConfig-missingInitParam.json");
        assertThrows(ConfigurationException.class, () -> ControllerConfigUtils.getConfiguration(inputStream));
    }

    @Test
    void loadWrongInitParam() {
        InputStream inputStream = this.getClass()
                .getResourceAsStream("/testLightyControllerConfig-wrongInitParam.json");
        assertThrows(ConfigurationException.class, () -> ControllerConfigUtils.getConfiguration(inputStream));
    }

    @Test
    void loadOkDataInitConfig() throws ConfigurationException {
        InputStream inputStream = this.getClass()
                .getResourceAsStream("/testLightyControllerConfig-okDataInit.json");
        final ControllerConfiguration configuration = ControllerConfigUtils.getConfiguration(inputStream);

        assertNotNull(configuration);
        assertEquals("test-path", configuration.getInitialConfigData().getPathToInitDataFile());
        assertEquals(FileToDatastoreUtils.ImportFileFormat.JSON, configuration.getInitialConfigData().getFormat());
    }

    @Test
    void loadControllerConfiguration() throws IOException, ConfigurationException {
        InputStream inputStream = this.getClass().getResourceAsStream("/testLightyControllerConfig.json");
        final ControllerConfiguration configuration = ControllerConfigUtils.getConfiguration(inputStream);
        assertNotNull(configuration);
    }

    @Test
    void loadMissingConfiguration() {
        InputStream inputStream = this.getClass().getResourceAsStream("/testMissingConfig.json");
        assertThrows(NullPointerException.class, () -> ControllerConfigUtils.getConfiguration(inputStream));
    }

    @Test
    void loadEmptyConfiguration() {
        InputStream inputStream = this.getClass().getResourceAsStream("/testEmptyConfig.json");
        assertThrows(ConfigurationException.class, () -> ControllerConfigUtils.getConfiguration(inputStream));
    }

    @Test
    void loadEmptyJsonConfiguration() throws ConfigurationException {
        InputStream inputStream = this.getClass().getResourceAsStream("/testEmptyJsonConfig.json");
        ControllerConfiguration configuration = ControllerConfigUtils.getConfiguration(inputStream);
        assertNotNull(configuration);
    }

    @Test
    void loadNonJsonConfiguration() {
        InputStream inputStream = this.getClass().getResourceAsStream("/testNonJsonConfig.json");
        assertThrows(ConfigurationException.class, () -> ControllerConfigUtils.getConfiguration(inputStream));
    }

    @Test
    void loadDummyConfiguration() {
        InputStream inputStream = this.getClass().getResourceAsStream("/testBadConfig.json");
        assertThrows(ConfigurationException.class, () -> ControllerConfigUtils.getConfiguration(inputStream));
    }

    @Test
    void loadConfigDatastoreCtxTest() throws IOException {
        DatastoreContext dataStoreContext = loadDatastoreContext(
            DatastoreConfigurationUtils.DATASTORECTX_CONFIG_ROOT_ELEMENT_NAME, LogicalDatastoreType.CONFIGURATION);

        assertNotNull(dataStoreContext);
        assertEquals(Duration.of(100, ChronoUnit.MILLIS), dataStoreContext.getShardTransactionIdleTimeout());
        assertEquals(50000, dataStoreContext.getOperationTimeoutInMillis());
        assertEquals(300, dataStoreContext.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(10, dataStoreContext.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(200000, dataStoreContext.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(15, dataStoreContext.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(Duration.of(5000, ChronoUnit.MILLIS),
            dataStoreContext.getShardRaftConfig().getHeartBeatInterval());
        assertEquals(500000, dataStoreContext.getShardTransactionCommitQueueCapacity());
        assertEquals(Duration.of(3000000, ChronoUnit.MILLIS), dataStoreContext.getShardInitializationTimeout());
        assertEquals(Duration.of(300000, ChronoUnit.MILLIS), dataStoreContext.getShardLeaderElectionTimeout());
        assertFalse(dataStoreContext.isPersistent());
        assertEquals(10000, dataStoreContext.getShardBatchedModificationCount());
        assertEquals(1200000, dataStoreContext.getShardCommitQueueExpiryTimeoutInMillis());
        assertTrue(dataStoreContext.isTransactionDebugContextEnabled());
        assertEquals(2048001, dataStoreContext.getMaximumMessageSliceSize());
        assertEquals(135266304, dataStoreContext.getFileBackedStreamingThreshold());
        assertEquals(11, dataStoreContext.getShardRaftConfig().getSyncIndexThreshold());
        assertEquals(31000000000L, dataStoreContext.getBackendAlivenessTimerInterval());
        assertEquals(121000000000L, dataStoreContext.getRequestTimeout());
        assertEquals(901000000000L, dataStoreContext.getNoProgressTimeout());
    }

    @Test
    void loadOperationalDatastoreCtxTest() throws IOException {
        DatastoreContext dataStoreContext = loadDatastoreContext(
            DatastoreConfigurationUtils.DATASTORECTX_OPERATIONAL_ROOT_ELEMENT_NAME, LogicalDatastoreType.OPERATIONAL);

        assertNotNull(dataStoreContext);
        assertEquals(Duration.of(-100, ChronoUnit.MILLIS), dataStoreContext.getShardTransactionIdleTimeout());
        assertEquals(-50000, dataStoreContext.getOperationTimeoutInMillis());
        assertEquals(-300, dataStoreContext.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(-10, dataStoreContext.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(-200000, dataStoreContext.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(18, dataStoreContext.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(Duration.of(-5000, ChronoUnit.MILLIS),
            dataStoreContext.getShardRaftConfig().getHeartBeatInterval());
        assertEquals(-500000, dataStoreContext.getShardTransactionCommitQueueCapacity());
        assertEquals(Duration.of(-3000000, ChronoUnit.MILLIS),
            dataStoreContext.getShardInitializationTimeout());
        assertEquals(Duration.of(-300000, ChronoUnit.MILLIS), dataStoreContext.getShardLeaderElectionTimeout());
        assertTrue(dataStoreContext.isPersistent());
        assertEquals(-10000, dataStoreContext.getShardBatchedModificationCount());
        assertEquals(-1200000, dataStoreContext.getShardCommitQueueExpiryTimeoutInMillis());
        assertFalse(dataStoreContext.isTransactionDebugContextEnabled());
        assertEquals(2048002, dataStoreContext.getMaximumMessageSliceSize());
        assertEquals(136314880, dataStoreContext.getFileBackedStreamingThreshold());
        assertEquals(12, dataStoreContext.getShardRaftConfig().getSyncIndexThreshold());
        assertEquals(32000000000L, dataStoreContext.getBackendAlivenessTimerInterval());
        assertEquals(122000000000L, dataStoreContext.getRequestTimeout());
        assertEquals(902000000000L, dataStoreContext.getNoProgressTimeout());
    }

    @Test
    void loadControllerConfigurationTest() throws Exception {
        Set<String> expectedModuleNames = new HashSet<>();
        expectedModuleNames.add("network-topology");

        InputStream inputStream = this.getClass().getResourceAsStream("/testLightyControllerConfig-example.json");
        final ControllerConfiguration configuration = ControllerConfigUtils.getConfiguration(inputStream);
        Set<YangModuleInfo> models = configuration.getSchemaServiceConfig().getModels();
        assertEquals(3, models.size());

        for (String expectedModuleName : expectedModuleNames) {
            long expectedModuleCount = models.stream()
                .filter(m -> m.getName().getLocalName().equals(expectedModuleName))
                .count();
            assertTrue(expectedModuleCount > 0);
        }
    }

    @Test
    void loadConfigurationsAndCompareTest() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("/testLightyControllerConfig-example.json");
        final ControllerConfiguration configuration = ControllerConfigUtils.getConfiguration(inputStream);

        inputStream = this.getClass().getResourceAsStream("/testLightyControllerConfig.json");
        final ControllerConfiguration configuration2 = ControllerConfigUtils.getConfiguration(inputStream);

        assertNotEquals(configuration, configuration2);
    }

    private DatastoreContext loadDatastoreContext(final String contextName,
        final LogicalDatastoreType logicalDatastoreType) throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream("/testLightyControllerConfig.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode configNode = mapper.readTree(inputStream);
        inputStream.close();

        assertTrue(configNode.has(ControllerConfigUtils.CONTROLLER_CONFIG_ROOT_ELEMENT_NAME));
        JsonNode controllerNode = configNode.path(ControllerConfigUtils.CONTROLLER_CONFIG_ROOT_ELEMENT_NAME);
        assertNotNull(controllerNode);
        JsonNode dataStoreCtxNode = controllerNode.path(contextName);
        assertNotNull(dataStoreCtxNode);

        return DatastoreConfigurationUtils.createDatastoreContext(dataStoreCtxNode, logicalDatastoreType);
    }
}