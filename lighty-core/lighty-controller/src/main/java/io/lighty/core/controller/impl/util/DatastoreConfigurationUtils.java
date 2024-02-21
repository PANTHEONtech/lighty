/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.util;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;

public final class DatastoreConfigurationUtils {

    public static final String DATASTORECTX_CONFIG_ROOT_ELEMENT_NAME = "configurationDatastoreContext";
    public static final String DATASTORECTX_OPERATIONAL_ROOT_ELEMENT_NAME = "operationalDatastoreContext";
    public static final String TEMP_FILE_DIRECTORY = "./data";
    public static final String NO_CUSTOM_POLICY = "";

    private DatastoreConfigurationUtils() {
    }

    public static DatastoreContext createDatastoreContext(final JsonNode configNode,
            final LogicalDatastoreType logicalDatastoreType) {
        final Builder builder = DatastoreContext.newBuilder()
                .operationTimeoutInMillis(configNode.path("operationTimeoutInMillis")
                        .asLong(DatastoreContext.DEFAULT_OPERATION_TIMEOUT_IN_MS))
                .shardTransactionCommitTimeoutInSeconds(configNode.path("shardTransactionCommitTimeoutInSeconds")
                        .asInt(DatastoreContext.DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS))
                .shardJournalRecoveryLogBatchSize(configNode.path("shardJournalRecoveryLogBatchSize")
                        .asInt(DatastoreContext.DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE))
                .shardSnapshotBatchCount(configNode.path("shardSnapshotBatchCount")
                        .asInt(DatastoreContext.DEFAULT_SNAPSHOT_BATCH_COUNT))
                .shardSnapshotDataThresholdPercentage(configNode.path("shardSnapshotDataThresholdPercentage")
                        .asInt(DatastoreContext.DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE))
                .shardHeartbeatIntervalInMillis(configNode.path("shardHeartbeatIntervalInMillis")
                        .asInt(DatastoreContext.DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS))
                .shardTransactionCommitQueueCapacity(configNode.path("shardTransactionCommitQueueCapacity")
                        .asInt(DatastoreContext.DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY))
                .persistent(configNode.path("persistent").asBoolean(DatastoreContext.DEFAULT_PERSISTENT))
                .logicalStoreType(logicalDatastoreType)
                .shardBatchedModificationCount(configNode.path("shardBatchedModificationCount")
                        .asInt(DatastoreContext.DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT))
                .shardCommitQueueExpiryTimeoutInMillis(configNode.path("shardCommitQueueExpiryTimeoutInMillis")
                        .asLong(DatastoreContext.DEFAULT_SHARD_COMMIT_QUEUE_EXPIRY_TIMEOUT_IN_MS))
                .customRaftPolicyImplementation(NO_CUSTOM_POLICY)
                .maximumMessageSliceSize(configNode.path("maximumMessageSliceSize")
                        .asInt(DatastoreContext.DEFAULT_MAX_MESSAGE_SLICE_SIZE))
                .tempFileDirectory(TEMP_FILE_DIRECTORY)
                .syncIndexThreshold(configNode.path("syncIndexThreshold")
                        .asLong(DatastoreContext.DEFAULT_SYNC_INDEX_THRESHOLD));

        return setNotNullElementWithoutDefaultConstant(configNode, builder).build();
    }

    private static Builder setNotNullElementWithoutDefaultConstant(final JsonNode configNode, final Builder builder) {
        if (!configNode.path("transactionDebugContextEnabled").asText().isBlank()) {
            builder.transactionDebugContextEnabled(configNode.path("transactionDebugContextEnabled").asBoolean());
        }
        if (!configNode.path("fileBackedStreamingThresholdInMegabytes").asText().isBlank()) {
            builder.fileBackedStreamingThresholdInMegabytes(
                    configNode.path("fileBackedStreamingThresholdInMegabytes").asInt());
        }
        if (!configNode.path("backendAlivenessTimerIntervalInSeconds").asText().isBlank()) {
            builder.backendAlivenessTimerIntervalInSeconds(
                    configNode.path("backendAlivenessTimerIntervalInSeconds").asLong());
        }
        if (!configNode.path("frontendRequestTimeoutInSeconds").asText().isBlank()) {
            builder.frontendRequestTimeoutInSeconds(configNode.path("frontendRequestTimeoutInSeconds").asLong());
        }
        if (!configNode.path("frontendNoProgressTimeoutInSeconds").asText().isBlank()) {
            builder.frontendNoProgressTimeoutInSeconds(configNode.path("frontendNoProgressTimeoutInSeconds").asLong());
        }
        if (!configNode.path("shardTransactionIdleTimeout").asText().isBlank()) {
            builder.shardTransactionIdleTimeout(configNode.path("shardTransactionIdleTimeout").asLong(),
                    TimeUnit.MILLISECONDS);
        }
        if (!configNode.path("shardInitializationTimeout").asText().isBlank()) {
            builder.shardInitializationTimeout(configNode.path("shardInitializationTimeout").asLong(),
                    TimeUnit.MILLISECONDS);
        }
        if (!configNode.path("shardLeaderElectionTimeout").asText().isBlank()) {
            builder.shardLeaderElectionTimeout(configNode.path("shardLeaderElectionTimeout").asLong(),
                    TimeUnit.MILLISECONDS);
        }
        return builder;
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

    public static Map<String, Object> getDefaultDatastoreProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("operational.persistent", "false");
        return props;
    }
}
