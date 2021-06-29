/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.device.session.listener;

import io.grpc.ConnectivityState;
import io.lighty.gnmi.southbound.identifier.IdentifierUtils;
import io.lighty.gnmi.southbound.timeout.TimeoutUtils;
import io.lighty.modules.gnmi.connector.session.api.SessionProvider;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.node.state.NodeState;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.node.state.NodeStateBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnmiConnectionStatusListener implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiConnectionStatusListener.class);

    private final SessionProvider sessionProvider;
    private final DataBroker dataBroker;
    private final NodeId nodeId;
    private final ExecutorService executorService;
    private ConnectivityState currentState;
    private boolean listenerActive;
    // Callback related attributes
    private Runnable onStatusCallback;
    private ConnectivityState callbackDesiredState;


    public GnmiConnectionStatusListener(final SessionProvider sessionBroker, final DataBroker dataBroker,
                                        final NodeId nodeId, final ExecutorService executorService) {
        this.sessionProvider = sessionBroker;
        this.dataBroker = dataBroker;
        this.nodeId = nodeId;
        this.executorService = executorService;
    }

    public synchronized void init() {
        LOG.info("Starting listening on gRPC channel state change for node {}", nodeId);
        listenerActive = true;
        updateStateStatus();
    }

    private synchronized void updateStateStatus() {
        if (listenerActive) {
            ConnectivityState newState = sessionProvider.getChannelState();

            LOG.info("Channel state of node {} changed from {} to {}. Updating operational datastore...",
                    currentState == null ? "UNKNOWN" : currentState, nodeId, newState);

            this.currentState = newState;
            // Trigger registered callback on status change, if exists
            triggerCallbackIfPresent();

            sessionProvider.notifyOnStateChangedOneOff(currentState, this::updateStateStatus);
            writeStateToDataStore(newState);
        }
    }

    private void triggerCallbackIfPresent() {
        if (onStatusCallback != null && callbackDesiredState == currentState) {
            LOG.debug("Triggering registered callback on node {} connectivity status change {}", nodeId,
                    callbackDesiredState);
            executorService.execute(onStatusCallback);
            onStatusCallback = null;
            callbackDesiredState = null;
        }
    }

    private synchronized void writeStateToDataStore(final ConnectivityState state) {
        try {
            @NonNull WriteTransaction tx = dataBroker.newWriteOnlyTransaction();

            Node operationalNode = new NodeBuilder()
                    .setNodeId(nodeId)
                    .addAugmentation(new GnmiNodeBuilder()
                            .setNodeState(new NodeStateBuilder().setNodeStatus(convertToNodeState(state))
                                    .build())
                            .build())
                    .build();
            tx.merge(LogicalDatastoreType.OPERATIONAL, IdentifierUtils.gnmiNodeIID(nodeId), operationalNode);
            tx.commit().get(TimeoutUtils.DATASTORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Unable to write connection state of gRPC channel of node {} to datastore", nodeId, e);
        }
    }

    @Override
    public synchronized void close() throws ExecutionException, InterruptedException, TimeoutException {
        LOG.info("Stopping listening on gRPC channel state for node {}", nodeId);
        listenerActive = false;
        currentState = ConnectivityState.SHUTDOWN;
        // Delete connection state data from operational datastore
        @NonNull WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.OPERATIONAL, IdentifierUtils.gnmiNodeIID(nodeId));
        writeTransaction.commit().get(TimeoutUtils.DATASTORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    private NodeState.NodeStatus convertToNodeState(ConnectivityState state) {
        switch (state) {
            case READY:
                return NodeState.NodeStatus.READY;
            case CONNECTING:
                return NodeState.NodeStatus.CONNECTING;
            case SHUTDOWN:
                return NodeState.NodeStatus.SHUTDOWN;
            case IDLE:
                return NodeState.NodeStatus.IDLE;
            default:
                return NodeState.NodeStatus.TRANSIENTFAILURE;
        }
    }

    /**
     * Registers callback which will be called when status reaches desired state.
     * @param callback runnable to call
     * @param state desired state
     */
    public void registerOnStatusCallback(final Runnable callback, final ConnectivityState state) {
        LOG.debug("Registering callback on node {} connectivity status change {}", nodeId,
                state);
        onStatusCallback = callback;
        callbackDesiredState = state;
    }

}
