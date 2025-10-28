/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.listener;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.gnmi.southbound.device.connection.DeviceConnectionManager;
import io.lighty.gnmi.southbound.identifier.IdentifierUtils;
import io.lighty.gnmi.southbound.timeout.TimeoutUtils;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiNode;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.node.state.NodeState;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.node.state.NodeStateBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GnmiNodeListener implements DataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiNodeListener.class);

    private final DeviceConnectionManager deviceConnectionManager;
    private final DataBroker dataBroker;
    private final ExecutorService executorService;

    public GnmiNodeListener(final DeviceConnectionManager deviceConnectionManager, final DataBroker dataBroker,
                            final ExecutorService executorService) {
        this.deviceConnectionManager = deviceConnectionManager;
        this.dataBroker = dataBroker;
        this.executorService = executorService;
    }


    @Override
    public void onDataTreeChanged(@NonNull List<DataTreeModification<Node>> changes) {
        LOG.debug("Data tree change on gNMI topology triggered");
        for (final DataTreeModification<Node> change : changes) {
            final DataObjectModification<Node> rootNode = change.getRootNode();
            final NodeId nodeId = IdentifierUtils.nodeIdOfPathArgument(rootNode.getIdentifier());
            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    if (nodeParamsUpdated(rootNode)) {
                        LOG.info("Received change in gNMI node connection configuration. Node ID: {}", nodeId);
                        disconnectNode(nodeId);
                        connectNode(rootNode.getDataAfter());
                    }
                    break;
                case DELETE:
                    LOG.info("Received delete node {} event, disconnecting ...", nodeId);
                    disconnectNode(nodeId);
                    break;
                default:
                    LOG.warn("Unsupported tree modification received, {}", rootNode.getModificationType());
                    break;
            }

        }
    }

    private void disconnectNode(final NodeId nodeId) {
        deviceConnectionManager.closeConnection(nodeId);
        // Delete operational data
        @NonNull WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.OPERATIONAL, IdentifierUtils.gnmiNodeIID(nodeId));
        try {
            writeTransaction.commit().get(TimeoutUtils.DATASTORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Failed deleting node state of node {} from operational datastore", nodeId.getValue(), e);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while deleting node state of node {} from operational datastore",
                    nodeId.getValue(), e);
            Thread.currentThread().interrupt();
        }
    }

    private void connectNode(final Node node) {
        final ListenableFuture<CommitInfo> connectionResult = deviceConnectionManager.connectDevice(node);
        Futures.addCallback(connectionResult, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable final CommitInfo result) {
                LOG.info("Connection with node {} established successfully", node.getNodeId());
            }

            @Override
            public void onFailure(Throwable throwable) {
                // Write failure reason to datastore only if future was not cancelled
                // (connection future is cancelled when node is deleted while connecting)
                if (!(throwable instanceof CancellationException)) {
                    try {
                        LOG.error("Connection of node {} failed", node.getNodeId(), throwable);
                        writeConnectionFailureReasonToDatastore(node.getNodeId(), throwable.toString());
                    } catch (TimeoutException | ExecutionException e) {
                        throw new RuntimeException(
                                String.format("Failed writing reason of connection failure of node %s to datastore",
                                        node.getNodeId().getValue()), e);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(
                                String.format("Interrupted while writing connection failure of node %s to datastore",
                                        node.getNodeId().getValue()), e);
                    }
                } else {
                    LOG.info("Connection initialization to node {} was cancelled", node.getNodeId());
                }

            }
        }, executorService);
    }

    private boolean nodeParamsUpdated(final DataObjectModification<Node> rootNode) {
        final Node nodeBefore = rootNode.getDataBefore();
        final Node nodeAfter = rootNode.getDataAfter();
        if (nodeBefore == null || nodeAfter == null) {
            return true;
        } else {
            final GnmiNode before = requireNonNull(nodeBefore.augmentation(GnmiNode.class),
                    "Node must be augmented by gNMI");
            final GnmiNode after = requireNonNull(nodeAfter.augmentation(GnmiNode.class),
                    "Node must be augmented by gNMI");
            return !Objects.equals(before.getConnectionParameters(), after.getConnectionParameters())
                || !Objects.equals(before.getExtensionsParameters(), after.getExtensionsParameters());
        }
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "False positive, method is actually called in connectNode()")
    private void writeConnectionFailureReasonToDatastore(NodeId nodeId, String message)
            throws InterruptedException, ExecutionException, TimeoutException {
        @NonNull final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();

        final Node operationalNode = new NodeBuilder()
                .setNodeId(nodeId)
                .addAugmentation(new GnmiNodeBuilder()
                        .setNodeState(new NodeStateBuilder().setNodeStatus(NodeState.NodeStatus.FAILURE)
                                .setFailureDetails(message)
                                .build())
                        .build())
                .build();

        tx.merge(LogicalDatastoreType.OPERATIONAL, IdentifierUtils.gnmiNodeIID(nodeId), operationalNode);
        tx.commit().get(TimeoutUtils.DATASTORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

}
