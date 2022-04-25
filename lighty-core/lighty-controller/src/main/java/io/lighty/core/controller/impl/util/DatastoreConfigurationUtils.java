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
        return DatastoreContext.newBuilder()
                .shardTransactionIdleTimeout(configNode.path("shardTransactionIdleTimeout").asLong(),
                    TimeUnit.MILLISECONDS)
                .operationTimeoutInMillis(configNode.path("operationTimeoutInMillis").asLong())
                .shardTransactionCommitTimeoutInSeconds(configNode.path("shardTransactionCommitTimeoutInSeconds")
                    .asInt())
                .shardJournalRecoveryLogBatchSize(configNode.path("shardJournalRecoveryLogBatchSize").asInt())
                .shardSnapshotBatchCount(configNode.path("shardSnapshotBatchCount").asInt())
                .shardSnapshotDataThresholdPercentage(configNode.path("shardSnapshotDataThresholdPercentage").asInt())
                .shardHeartbeatIntervalInMillis(configNode.path("shardHeartbeatIntervalInMillis").asInt())
                .shardTransactionCommitQueueCapacity(configNode.path("shardTransactionCommitQueueCapacity").asInt())
                .shardInitializationTimeout(configNode.path("shardInitializationTimeout").asLong(),
                    TimeUnit.MILLISECONDS)
                .shardLeaderElectionTimeout(configNode.path("shardLeaderElectionTimeout").asLong(),
                    TimeUnit.MILLISECONDS)
                .persistent(configNode.path("persistent").asBoolean())
                .logicalStoreType(logicalDatastoreType)
                .shardBatchedModificationCount(configNode.path("shardBatchedModificationCount").asInt())
                .shardCommitQueueExpiryTimeoutInMillis(configNode.path("shardCommitQueueExpiryTimeoutInMillis").asInt())
                .transactionDebugContextEnabled(configNode.path("transactionDebugContextEnabled").booleanValue())
                .useTellBasedProtocol(configNode.path("useTellBasedProtocol").booleanValue())
                .customRaftPolicyImplementation(NO_CUSTOM_POLICY)
                .maximumMessageSliceSize(configNode.path("maximumMessageSliceSize").asInt())
                .tempFileDirectory(TEMP_FILE_DIRECTORY)
                .fileBackedStreamingThresholdInMegabytes(configNode.path("fileBackedStreamingThresholdInMegabytes")
                    .asInt())
                .syncIndexThreshold(configNode.path("syncIndexThreshold").asInt())
                .backendAlivenessTimerIntervalInSeconds(configNode.path("backendAlivenessTimerIntervalInSeconds")
                    .asInt())
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

    public static Map<String, Object> getDefaultDatastoreProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("operational.persistent", "false");
        return props;
    }
}
