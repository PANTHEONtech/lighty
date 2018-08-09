/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl.util;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;

public final class DatastoreConfigurationUtils {

    public static final String DATASTORECTX_CONFIG_ROOT_ELEMENT_NAME = "configurationDatastoreContext";
    public static final String DATASTORECTX_OPERATIONAL_ROOT_ELEMENT_NAME = "operationalDatastoreContext";
    public static final String TEMP_FILE_DIRECTORY = "./data";
    public static final String NO_CUSTOM_POLICY = "";

    private DatastoreConfigurationUtils() {
    }

    public static DatastoreContext createDatastoreContext(final JsonNode configNode, final LogicalDatastoreType logicalDatastoreType) {
        return DatastoreContext.newBuilder()
                .shardTransactionIdleTimeout(configNode.path("shardTransactionIdleTimeout").asLong(), TimeUnit.MILLISECONDS)
                .operationTimeoutInMillis(configNode.path("operationTimeoutInMillis").asLong())
                .shardTransactionCommitTimeoutInSeconds(configNode.path("shardTransactionCommitTimeoutInSeconds").asInt())
                .shardJournalRecoveryLogBatchSize(configNode.path("shardJournalRecoveryLogBatchSize").asInt())
                .shardSnapshotBatchCount(configNode.path("shardSnapshotBatchCount").asInt())
                .shardSnapshotDataThresholdPercentage(configNode.path("shardSnapshotDataThresholdPercentage").asInt())
                .shardHeartbeatIntervalInMillis(configNode.path("shardHeartbeatIntervalInMillis").asInt())
                .shardTransactionCommitQueueCapacity(configNode.path("shardTransactionCommitQueueCapacity").asInt())
                .shardInitializationTimeout(configNode.path("shardInitializationTimeout").asLong(), TimeUnit.MILLISECONDS)
                .shardLeaderElectionTimeout(configNode.path("shardLeaderElectionTimeout").asLong(), TimeUnit.MILLISECONDS)
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
                .fileBackedStreamingThresholdInMegabytes(configNode.path("fileBackedStreamingThresholdInMegabytes").asInt())
                .syncIndexThreshold(configNode.path("syncIndexThreshold").asInt())
                .backendAlivenessTimerIntervalInSeconds(configNode.path("backendAlivenessTimerIntervalInSeconds").asInt())
                .frontendRequestTimeoutInSeconds(configNode.path("frontendRequestTimeoutInSeconds").asLong())
                .frontendNoProgressTimeoutInSeconds(configNode.path("frontendNoProgressTimeoutInSeconds").asLong())
                .build();
    }

    public static DatastoreContext createDefaultOperationalDatastoreContext() {
        return DatastoreContext.newBuilder()
                .logicalStoreType(LogicalDatastoreType.OPERATIONAL)
                .tempFileDirectory(TEMP_FILE_DIRECTORY)
                .persistent(false)
                .build();
    }

    public static DatastoreContext createDefaultConfigDatastoreContext() {
        return DatastoreContext.newBuilder()
                .logicalStoreType(LogicalDatastoreType.CONFIGURATION)
                .tempFileDirectory(TEMP_FILE_DIRECTORY)
                .build();
    }

}
