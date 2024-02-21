/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.config;

import akka.util.Timeout;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.core.controller.impl.util.DatastoreConfigurationUtils;
import io.lighty.core.controller.impl.util.FileToDatastoreUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.testng.Assert;
import org.testng.annotations.Test;
import scala.concurrent.duration.Duration;

public class ConfigLoadingTest {

    @Test
    public void loadControllerConfigurationNoDsContexts() throws IOException, ConfigurationException {
        InputStream inputStream = this.getClass().getResourceAsStream("/testLightyControllerConfig-noDsContexts.json");
        final ControllerConfiguration configuration = ControllerConfigUtils.getConfiguration(inputStream);
        Assert.assertNotNull(configuration);
        Assert.assertNotNull(configuration.getConfigDatastoreContext());
        Assert.assertNotNull(configuration.getOperDatastoreContext());
        DatastoreContext configDatastoreContext = configuration.getConfigDatastoreContext();
        Assert.assertEquals(configDatastoreContext.getShardTransactionIdleTimeout(),
            Duration.create(10, TimeUnit.MINUTES));
        Assert.assertEquals(configDatastoreContext.getOperationTimeoutInMillis(), 5000);
        Assert.assertEquals(configDatastoreContext.getShardTransactionCommitTimeoutInSeconds(), 30);
        Assert.assertEquals(configDatastoreContext.getShardRaftConfig().getJournalRecoveryLogBatchSize(), 1);
        Assert.assertEquals(configDatastoreContext.getShardRaftConfig().getSnapshotBatchCount(), 20000);
        Assert.assertEquals(configDatastoreContext.getShardRaftConfig().getSnapshotDataThresholdPercentage(), 12);
        Assert.assertEquals(configDatastoreContext.getShardRaftConfig().getHeartBeatInterval(),
            Duration.create(500, TimeUnit.MILLISECONDS));
        Assert.assertEquals(configDatastoreContext.getShardTransactionCommitQueueCapacity(), 50000);
        Assert.assertEquals(configDatastoreContext.getShardInitializationTimeout(),
            new Timeout(300000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(configDatastoreContext.getShardLeaderElectionTimeout(),
            new Timeout(30000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(configDatastoreContext.isPersistent(), true);
        Assert.assertEquals(configDatastoreContext.getShardBatchedModificationCount(), 1000);
        Assert.assertEquals(configDatastoreContext.getShardCommitQueueExpiryTimeoutInMillis(), 120000);
        Assert.assertEquals(configDatastoreContext.isTransactionDebugContextEnabled(), false);
        Assert.assertEquals(configDatastoreContext.getMaximumMessageSliceSize(), 491520);
        Assert.assertEquals(configDatastoreContext.getFileBackedStreamingThreshold(), 134217728);
        Assert.assertEquals(configDatastoreContext.getShardRaftConfig().getSyncIndexThreshold(), 10);
        Assert.assertEquals(configDatastoreContext.getBackendAlivenessTimerInterval(), 30000000000L);
        Assert.assertEquals(configDatastoreContext.getRequestTimeout(), 120000000000L);
        Assert.assertEquals(configDatastoreContext.getNoProgressTimeout(), 900000000000L);

        DatastoreContext operDatastoreContext = configuration.getOperDatastoreContext();
        Assert.assertEquals(operDatastoreContext.getShardTransactionIdleTimeout(),
            Duration.create(10, TimeUnit.MINUTES));
        Assert.assertEquals(operDatastoreContext.getOperationTimeoutInMillis(), 5000);
        Assert.assertEquals(operDatastoreContext.getShardTransactionCommitTimeoutInSeconds(), 30);
        Assert.assertEquals(operDatastoreContext.getShardRaftConfig().getJournalRecoveryLogBatchSize(), 1);
        Assert.assertEquals(operDatastoreContext.getShardRaftConfig().getSnapshotBatchCount(), 20000);
        Assert.assertEquals(operDatastoreContext.getShardRaftConfig().getSnapshotDataThresholdPercentage(), 12);
        Assert.assertEquals(operDatastoreContext.getShardRaftConfig().getHeartBeatInterval(),
            Duration.create(500, TimeUnit.MILLISECONDS));
        Assert.assertEquals(operDatastoreContext.getShardTransactionCommitQueueCapacity(), 50000);
        Assert.assertEquals(operDatastoreContext.getShardInitializationTimeout(),
            new Timeout(300000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(operDatastoreContext.getShardLeaderElectionTimeout(),
            new Timeout(30000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(operDatastoreContext.isPersistent(), false);
        Assert.assertEquals(operDatastoreContext.getShardBatchedModificationCount(), 1000);
        Assert.assertEquals(operDatastoreContext.getShardCommitQueueExpiryTimeoutInMillis(), 120000);
        Assert.assertEquals(operDatastoreContext.isTransactionDebugContextEnabled(), false);
        Assert.assertEquals(operDatastoreContext.getMaximumMessageSliceSize(), 491520);
        Assert.assertEquals(operDatastoreContext.getFileBackedStreamingThreshold(), 134217728);
        Assert.assertEquals(operDatastoreContext.getShardRaftConfig().getSyncIndexThreshold(), 10);
        Assert.assertEquals(operDatastoreContext.getBackendAlivenessTimerInterval(), 30000000000L);
        Assert.assertEquals(operDatastoreContext.getRequestTimeout(), 120000000000L);
        Assert.assertEquals(operDatastoreContext.getNoProgressTimeout(), 900000000000L);
        Assert.assertNull(configuration.getInitialConfigData());
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void loadMissingInitConfigParam() throws ConfigurationException {
        InputStream inputStream = this.getClass()
                .getResourceAsStream("/testLightyControllerConfig-missingInitParam.json");
        final ControllerConfiguration configuration = ControllerConfigUtils.getConfiguration(inputStream);
        Assert.assertNotNull(configuration);
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void loadWrongInitParam() throws ConfigurationException {
        InputStream inputStream = this.getClass()
                .getResourceAsStream("/testLightyControllerConfig-wrongInitParam.json");
        final ControllerConfiguration configuration = ControllerConfigUtils.getConfiguration(inputStream);
        Assert.assertNotNull(configuration);
    }

    @Test
    public void loadOkDataInitConfig() throws ConfigurationException {
        InputStream inputStream = this.getClass()
                .getResourceAsStream("/testLightyControllerConfig-okDataInit.json");
        final ControllerConfiguration configuration = ControllerConfigUtils.getConfiguration(inputStream);
        Assert.assertNotNull(configuration);
        Assert.assertTrue(configuration.getInitialConfigData().getPathToInitDataFile().equals("test-path"));
        Assert.assertTrue(configuration.getInitialConfigData().getFormat()
                .equals(FileToDatastoreUtils.ImportFileFormat.JSON));
    }

    @Test
    public void loadControllerConfiguration() throws IOException, ConfigurationException {
        InputStream inputStream = this.getClass().getResourceAsStream("/testLightyControllerConfig.json");
        final ControllerConfiguration configuration = ControllerConfigUtils.getConfiguration(inputStream);
        Assert.assertNotNull(configuration);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void loadMissingConfiguration() throws ConfigurationException {
        InputStream inputStream = this.getClass().getResourceAsStream("/testMissingConfig.json");
        ControllerConfigUtils.getConfiguration(inputStream);
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void loadEmptyConfiguration() throws ConfigurationException {
        InputStream inputStream = this.getClass().getResourceAsStream("/testEmptyConfig.json");
        ControllerConfigUtils.getConfiguration(inputStream);
    }

    @Test
    public void loadEmptyJsonConfiguration() throws ConfigurationException {
        InputStream inputStream = this.getClass().getResourceAsStream("/testEmptyJsonConfig.json");
        ControllerConfiguration configuration = ControllerConfigUtils.getConfiguration(inputStream);
        Assert.assertNotNull(configuration);
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void loadNonJsonConfiguration() throws ConfigurationException {
        InputStream inputStream = this.getClass().getResourceAsStream("/testNonJsonConfig.json");
        ControllerConfigUtils.getConfiguration(inputStream);
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void loadDummyConfiguration() throws ConfigurationException {
        InputStream inputStream = this.getClass().getResourceAsStream("/testBadConfig.json");
        ControllerConfigUtils.getConfiguration(inputStream);
    }

    @Test
    public void loadConfigDatastoreCtxTest() throws IOException {
        DatastoreContext dataStoreContext = loadDatastoreContext(
            DatastoreConfigurationUtils.DATASTORECTX_CONFIG_ROOT_ELEMENT_NAME, LogicalDatastoreType.CONFIGURATION);
        Assert.assertNotNull(dataStoreContext);
        Assert.assertEquals(dataStoreContext.getShardTransactionIdleTimeout(),
            Duration.create(100, TimeUnit.MILLISECONDS));
        Assert.assertEquals(dataStoreContext.getOperationTimeoutInMillis(), 50000);
        Assert.assertEquals(dataStoreContext.getShardTransactionCommitTimeoutInSeconds(), 300);
        Assert.assertEquals(dataStoreContext.getShardRaftConfig().getJournalRecoveryLogBatchSize(), 10);
        Assert.assertEquals(dataStoreContext.getShardRaftConfig().getSnapshotBatchCount(), 200000);
        Assert.assertEquals(dataStoreContext.getShardRaftConfig().getSnapshotDataThresholdPercentage(), 15);
        Assert.assertEquals(dataStoreContext.getShardRaftConfig().getHeartBeatInterval(),
            Duration.create(5000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(dataStoreContext.getShardTransactionCommitQueueCapacity(), 500000);
        Assert.assertEquals(dataStoreContext.getShardInitializationTimeout(),
            new Timeout(3000000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(dataStoreContext.getShardLeaderElectionTimeout(),
            new Timeout(300000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(dataStoreContext.isPersistent(), false);
        Assert.assertEquals(dataStoreContext.getShardBatchedModificationCount(), 10000);
        Assert.assertEquals(dataStoreContext.getShardCommitQueueExpiryTimeoutInMillis(), 1200000);
        Assert.assertEquals(dataStoreContext.isTransactionDebugContextEnabled(), true);
        Assert.assertEquals(dataStoreContext.getMaximumMessageSliceSize(), 2048001);
        Assert.assertEquals(dataStoreContext.getFileBackedStreamingThreshold(), 135266304);
        Assert.assertEquals(dataStoreContext.getShardRaftConfig().getSyncIndexThreshold(), 11);
        Assert.assertEquals(dataStoreContext.getBackendAlivenessTimerInterval(), 31000000000L);
        Assert.assertEquals(dataStoreContext.getRequestTimeout(), 121000000000L);
        Assert.assertEquals(dataStoreContext.getNoProgressTimeout(), 901000000000L);
    }

    @Test
    public void loadOperationalDatastoreCtxTest() throws IOException {
        DatastoreContext dataStoreContext = loadDatastoreContext(
            DatastoreConfigurationUtils.DATASTORECTX_OPERATIONAL_ROOT_ELEMENT_NAME, LogicalDatastoreType.OPERATIONAL);
        Assert.assertNotNull(dataStoreContext);
        Assert.assertEquals(dataStoreContext.getShardTransactionIdleTimeout(),
            Duration.create(-100, TimeUnit.MILLISECONDS));
        Assert.assertEquals(dataStoreContext.getOperationTimeoutInMillis(), -50000);
        Assert.assertEquals(dataStoreContext.getShardTransactionCommitTimeoutInSeconds(), -300);
        Assert.assertEquals(dataStoreContext.getShardRaftConfig().getJournalRecoveryLogBatchSize(), -10);
        Assert.assertEquals(dataStoreContext.getShardRaftConfig().getSnapshotBatchCount(), -200000);
        Assert.assertEquals(dataStoreContext.getShardRaftConfig().getSnapshotDataThresholdPercentage(), 18);
        Assert.assertEquals(dataStoreContext.getShardRaftConfig().getHeartBeatInterval(),
            Duration.create(-5000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(dataStoreContext.getShardTransactionCommitQueueCapacity(), -500000);
        Assert.assertEquals(dataStoreContext.getShardInitializationTimeout(),
            new Timeout(-3000000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(dataStoreContext.getShardLeaderElectionTimeout(),
            new Timeout(-300000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(dataStoreContext.isPersistent(), true);
        Assert.assertEquals(dataStoreContext.getShardBatchedModificationCount(), -10000);
        Assert.assertEquals(dataStoreContext.getShardCommitQueueExpiryTimeoutInMillis(), -1200000);
        Assert.assertEquals(dataStoreContext.isTransactionDebugContextEnabled(), false);
        Assert.assertEquals(dataStoreContext.getMaximumMessageSliceSize(), 2048002);
        Assert.assertEquals(dataStoreContext.getFileBackedStreamingThreshold(), 136314880);
        Assert.assertEquals(dataStoreContext.getShardRaftConfig().getSyncIndexThreshold(), 12);
        Assert.assertEquals(dataStoreContext.getBackendAlivenessTimerInterval(), 32000000000L);
        Assert.assertEquals(dataStoreContext.getRequestTimeout(), 122000000000L);
        Assert.assertEquals(dataStoreContext.getNoProgressTimeout(), 902000000000L);
    }

    @Test
    public void loadControllerConfigurationTest() throws Exception {
        Set<String> expectedModuleNames = new HashSet<>();
        expectedModuleNames.add("network-topology");

        InputStream inputStream = this.getClass().getResourceAsStream("/testLightyControllerConfig-example.json");
        final ControllerConfiguration configuration = ControllerConfigUtils.getConfiguration(inputStream);
        Set<YangModuleInfo> models = configuration.getSchemaServiceConfig().getModels();
        Assert.assertEquals(models.size(), 2);

        for (String expectedModuleName : expectedModuleNames) {
            long expectedModuleCount = models.stream()
                    .filter(m -> m.getName().getLocalName().equals(expectedModuleName))
                    .count();
            Assert.assertTrue(expectedModuleCount > 0);
        }
    }

    @Test
    public void loadConfigurationsAndCompareTest() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("/testLightyControllerConfig-example.json");
        final ControllerConfiguration configuration = ControllerConfigUtils.getConfiguration(inputStream);

        inputStream = this.getClass().getResourceAsStream("/testLightyControllerConfig.json");
        final ControllerConfiguration configuration2 = ControllerConfigUtils.getConfiguration(inputStream);

        Assert.assertNotEquals(configuration2, configuration);
    }

    private DatastoreContext loadDatastoreContext(final String contextName,
            final LogicalDatastoreType logicalDatastoreType) throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream("/testLightyControllerConfig.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode configNode = mapper.readTree(inputStream);
        inputStream.close();
        Assert.assertTrue(configNode.has(ControllerConfigUtils.CONTROLLER_CONFIG_ROOT_ELEMENT_NAME));
        JsonNode controllerNode = configNode.path(ControllerConfigUtils.CONTROLLER_CONFIG_ROOT_ELEMENT_NAME);
        Assert.assertNotNull(controllerNode);
        JsonNode dataStoreCtxNode = controllerNode.path(contextName);
        Assert.assertNotNull(dataStoreCtxNode);
        return DatastoreConfigurationUtils.createDatastoreContext(dataStoreCtxNode, logicalDatastoreType);
    }
}

