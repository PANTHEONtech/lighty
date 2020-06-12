/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.util;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.config.yang.config.distributed_datastore_provider.ConfigProperties;
import org.opendaylight.controller.config.yang.config.distributed_datastore_provider.OperationalProperties;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

public final class DatastoreConfigurationUtils {

    public static final String DATASTORECTX_CONFIG_ROOT_ELEMENT_NAME = "configurationDatastoreContext";
    public static final String DATASTORECTX_OPERATIONAL_ROOT_ELEMENT_NAME = "operationalDatastoreContext";
    public static final String TEMP_FILE_DIRECTORY = "./data";
    public static final String NO_CUSTOM_POLICY = "";

    private DatastoreConfigurationUtils() {
    }

    public static DatastoreContext createDatastoreContext(JsonNode configNode,
                                                          LogicalDatastoreType logicalDatastoreType) {
        return DatastoreContext.newBuilder()
                .shardTransactionIdleTimeout(
                        configNode.path("shardTransactionIdleTimeout").asLong(), TimeUnit.MILLISECONDS)
                .operationTimeoutInMillis(configNode.path("operationTimeoutInMillis").asLong())
                .shardTransactionCommitTimeoutInSeconds(
                        configNode.path("shardTransactionCommitTimeoutInSeconds").asInt())
                .shardJournalRecoveryLogBatchSize(configNode.path("shardJournalRecoveryLogBatchSize").asInt())
                .shardSnapshotBatchCount(configNode.path("shardSnapshotBatchCount").asInt())
                .shardSnapshotDataThresholdPercentage(configNode.path("shardSnapshotDataThresholdPercentage").asInt())
                .shardHeartbeatIntervalInMillis(configNode.path("shardHeartbeatIntervalInMillis").asInt())
                .shardTransactionCommitQueueCapacity(configNode.path("shardTransactionCommitQueueCapacity").asInt())
                .shardInitializationTimeout(
                        configNode.path("shardInitializationTimeout").asLong(), TimeUnit.MILLISECONDS)
                .shardLeaderElectionTimeout(
                        configNode.path("shardLeaderElectionTimeout").asLong(), TimeUnit.MILLISECONDS)
                .persistent(configNode.path("persistent").asBoolean())
                .logicalStoreType(logicalDatastoreType)
                .shardBatchedModificationCount(configNode.path("shardBatchedModificationCount").asInt())
                .shardCommitQueueExpiryTimeoutInMillis(configNode.path("shardCommitQueueExpiryTimeoutInMillis").asInt())
                .transactionDebugContextEnabled(configNode.path("transactionDebugContextEnabled").booleanValue())
                .maxShardDataChangeExecutorPoolSize(configNode.path("maxShardDataChangeExecutorPoolSize").asInt())
                .maxShardDataChangeExecutorQueueSize(configNode.path("maxShardDataChangeExecutorQueueSize").asInt())
                .maxShardDataChangeListenerQueueSize(configNode.path("maxShardDataChangeListenerQueueSize").asInt())
                .maxShardDataStoreExecutorQueueSize(configNode.path("maxShardDataStoreExecutorQueueSize").asInt())
                .useTellBasedProtocol(configNode.path("useTellBasedProtocol").booleanValue())
                .customRaftPolicyImplementation(NO_CUSTOM_POLICY)
                .maximumMessageSliceSize(configNode.path("maximumMessageSliceSize").asInt())
                .tempFileDirectory(TEMP_FILE_DIRECTORY)
                .fileBackedStreamingThresholdInMegabytes(
                        configNode.path("fileBackedStreamingThresholdInMegabytes").asInt())
                .syncIndexThreshold(configNode.path("syncIndexThreshold").asInt())
                .backendAlivenessTimerIntervalInSeconds(
                        configNode.path("backendAlivenessTimerIntervalInSeconds").asInt())
                .frontendRequestTimeoutInSeconds(configNode.path("frontendRequestTimeoutInSeconds").asLong())
                .frontendNoProgressTimeoutInSeconds(configNode.path("frontendNoProgressTimeoutInSeconds").asLong())
                .build();
    }

    public static DatastoreContext createDefaultOperationalDatastoreContext() {
        OperationalProperties props = new OperationalProperties();
        return DatastoreContext.newBuilder()
                .logicalStoreType(LogicalDatastoreType.OPERATIONAL)
                .tempFileDirectory(TEMP_FILE_DIRECTORY)
                .fileBackedStreamingThresholdInMegabytes(props.getFileBackedStreamingThresholdInMegabytes()
                        .getValue().intValue())
                .maxShardDataChangeExecutorPoolSize(props.getMaxShardDataChangeExecutorPoolSize().getValue().intValue())
                .maxShardDataChangeExecutorQueueSize(props.getMaxShardDataChangeExecutorQueueSize()
                        .getValue().intValue())
                .maxShardDataChangeListenerQueueSize(props.getMaxShardDataChangeListenerQueueSize()
                        .getValue().intValue())
                .maxShardDataStoreExecutorQueueSize(props.getMaxShardDataStoreExecutorQueueSize().getValue().intValue())
                .shardTransactionIdleTimeoutInMinutes(props.getShardTransactionIdleTimeoutInMinutes().getValue())
                .operationTimeoutInSeconds(props.getOperationTimeoutInSeconds().getValue())
                .shardJournalRecoveryLogBatchSize(props.getShardJournalRecoveryLogBatchSize()
                        .getValue().intValue())
                .shardSnapshotBatchCount(props.getShardSnapshotBatchCount().getValue().intValue())
                .shardSnapshotDataThresholdPercentage(props.getShardSnapshotDataThresholdPercentage()
                        .getValue().intValue())
                .shardHeartbeatIntervalInMillis(props.getShardHeartbeatIntervalInMillis().getValue())
                .shardInitializationTimeoutInSeconds(props.getShardInitializationTimeoutInSeconds().getValue())
                .shardLeaderElectionTimeoutInSeconds(props.getShardLeaderElectionTimeoutInSeconds().getValue())
                .shardTransactionCommitTimeoutInSeconds(
                        props.getShardTransactionCommitTimeoutInSeconds().getValue().intValue())
                .shardTransactionCommitQueueCapacity(
                        props.getShardTransactionCommitQueueCapacity().getValue().intValue())
                .persistent(false)
                .shardIsolatedLeaderCheckIntervalInMillis(
                        props.getShardIsolatedLeaderCheckIntervalInMillis().getValue())
                .shardElectionTimeoutFactor(props.getShardElectionTimeoutFactor().getValue())
                .transactionCreationInitialRateLimit(props.getTransactionCreationInitialRateLimit().getValue())
                .shardBatchedModificationCount(props.getShardBatchedModificationCount().getValue().intValue())
                .shardCommitQueueExpiryTimeoutInSeconds(
                        props.getShardCommitQueueExpiryTimeoutInSeconds().getValue().intValue())
                .transactionDebugContextEnabled(props.getTransactionDebugContextEnabled())
                .customRaftPolicyImplementation(props.getCustomRaftPolicyImplementation())
                .maximumMessageSliceSize(props.getMaximumMessageSliceSize().getValue().intValue())
                .useTellBasedProtocol(props.getUseTellBasedProtocol())
                .syncIndexThreshold(props.getSyncIndexThreshold().getValue())
                .backendAlivenessTimerIntervalInSeconds(props.getBackendAlivenessTimerIntervalInSeconds().getValue())
                .frontendRequestTimeoutInSeconds(props.getFrontendRequestTimeoutInSeconds().getValue())
                .frontendNoProgressTimeoutInSeconds(props.getFrontendNoProgressTimeoutInSeconds().getValue())
                .build();
    }

    public static DatastoreContext createDefaultConfigDatastoreContext() {
        ConfigProperties props = new ConfigProperties();
        return DatastoreContext.newBuilder()
                .logicalStoreType(LogicalDatastoreType.CONFIGURATION)
                .tempFileDirectory(TEMP_FILE_DIRECTORY)
                .fileBackedStreamingThresholdInMegabytes(props.getFileBackedStreamingThresholdInMegabytes()
                        .getValue().intValue())
                .maxShardDataChangeExecutorPoolSize(props.getMaxShardDataChangeExecutorPoolSize().getValue().intValue())
                .maxShardDataChangeExecutorQueueSize(props.getMaxShardDataChangeExecutorQueueSize()
                        .getValue().intValue())
                .maxShardDataChangeListenerQueueSize(props.getMaxShardDataChangeListenerQueueSize()
                        .getValue().intValue())
                .maxShardDataStoreExecutorQueueSize(props.getMaxShardDataStoreExecutorQueueSize().getValue().intValue())
                .shardTransactionIdleTimeoutInMinutes(props.getShardTransactionIdleTimeoutInMinutes().getValue())
                .operationTimeoutInSeconds(props.getOperationTimeoutInSeconds().getValue())
                .shardJournalRecoveryLogBatchSize(props.getShardJournalRecoveryLogBatchSize()
                        .getValue().intValue())
                .shardSnapshotBatchCount(props.getShardSnapshotBatchCount().getValue().intValue())
                .shardSnapshotDataThresholdPercentage(props.getShardSnapshotDataThresholdPercentage()
                        .getValue().intValue())
                .shardHeartbeatIntervalInMillis(props.getShardHeartbeatIntervalInMillis().getValue())
                .shardInitializationTimeoutInSeconds(props.getShardInitializationTimeoutInSeconds().getValue())
                .shardLeaderElectionTimeoutInSeconds(props.getShardLeaderElectionTimeoutInSeconds().getValue())
                .shardTransactionCommitTimeoutInSeconds(
                        props.getShardTransactionCommitTimeoutInSeconds().getValue().intValue())
                .shardTransactionCommitQueueCapacity(
                        props.getShardTransactionCommitQueueCapacity().getValue().intValue())
                .persistent(props.getPersistent().booleanValue())
                .shardIsolatedLeaderCheckIntervalInMillis(
                        props.getShardIsolatedLeaderCheckIntervalInMillis().getValue())
                .shardElectionTimeoutFactor(props.getShardElectionTimeoutFactor().getValue())
                .transactionCreationInitialRateLimit(props.getTransactionCreationInitialRateLimit().getValue())
                .shardBatchedModificationCount(props.getShardBatchedModificationCount().getValue().intValue())
                .shardCommitQueueExpiryTimeoutInSeconds(
                        props.getShardCommitQueueExpiryTimeoutInSeconds().getValue().intValue())
                .transactionDebugContextEnabled(props.getTransactionDebugContextEnabled())
                .customRaftPolicyImplementation(props.getCustomRaftPolicyImplementation())
                .maximumMessageSliceSize(props.getMaximumMessageSliceSize().getValue().intValue())
                .useTellBasedProtocol(props.getUseTellBasedProtocol())
                .syncIndexThreshold(props.getSyncIndexThreshold().getValue())
                .backendAlivenessTimerIntervalInSeconds(props.getBackendAlivenessTimerIntervalInSeconds().getValue())
                .frontendRequestTimeoutInSeconds(props.getFrontendRequestTimeoutInSeconds().getValue())
                .frontendNoProgressTimeoutInSeconds(props.getFrontendNoProgressTimeoutInSeconds().getValue())
                .build();
    }

}
