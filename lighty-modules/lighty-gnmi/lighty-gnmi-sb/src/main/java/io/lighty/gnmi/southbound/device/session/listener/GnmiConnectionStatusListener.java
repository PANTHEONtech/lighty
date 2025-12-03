/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.device.session.listener;

import com.google.common.util.concurrent.FluentFuture;
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
import org.opendaylight.mdsal.common.api.CommitInfo;
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

    /**
     * Update device connection state in md-sal datastore to READY.
     *
     * <p>As far as the state may change in time based on actual underlying connection, this method will perform the
     * write transaction into md-sal only if last observed state of underlying connection is still READY.</p>
     *
     * @throws GnmiConnectionStatusException when current state of underlying connection is different from READY.
     */
    public synchronized FluentFuture<CommitInfo> copyDeviceStatusReadyToDatastore()
            throws GnmiConnectionStatusException {
        if (ConnectivityState.READY.equals(currentState)) {
            return writeStateToDataStoreAsync(this.currentState);
        } else {
            throw new GnmiConnectionStatusException(
                    String.format("Last observed status was %s, while READY was expected", currentState),
                    currentState);
        }
    }

    private synchronized void updateStateStatus() {
        if (listenerActive) {
            ConnectivityState newState = sessionProvider.getChannelState();

            LOG.info("Channel state of node {} changed from {} to {}. Updating operational datastore...",
                    nodeId.getValue(), currentState == null ? "UNKNOWN" : currentState, newState);

            this.currentState = newState;
            // Trigger registered callback on status change, if exists
            triggerCallbackIfPresent();

            sessionProvider.notifyOnStateChangedOneOff(currentState, this::updateStateStatus);
            if (this.currentState != ConnectivityState.READY) {
                // Ready status should be updated after creating device mountpoint
                writeStateToDataStore(this.currentState);
            }
            LOG.debug("Current session status {}", currentState);
        }
    }

    private void triggerCallbackIfPresent() {
        if (onStatusCallback != null && callbackDesiredState == currentState) {
            LOG.debug("Triggering registered callback on node {} connectivity status change {}", nodeId.getValue(),
                    callbackDesiredState);
            executorService.execute(onStatusCallback);
            onStatusCallback = null;
            callbackDesiredState = null;
        }
    }

    private synchronized void writeStateToDataStore(final ConnectivityState state) {
        try {
            final FluentFuture<CommitInfo> commitFuture = writeStateToDataStoreAsync(state);
            commitFuture.get(TimeoutUtils.DATASTORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Unable to write connection state of gRPC channel of node {} to datastore", nodeId.getValue(), e);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while writing connection state of gRPC channel of node {} to datastore",
                    nodeId.getValue(), e);
            Thread.currentThread().interrupt();
        }
    }

    private synchronized FluentFuture<CommitInfo> writeStateToDataStoreAsync(final ConnectivityState state) {
        final @NonNull WriteTransaction tx = dataBroker.newWriteOnlyTransaction();

        final Node operationalNode = new NodeBuilder()
                .setNodeId(nodeId)
                .addAugmentation(new GnmiNodeBuilder()
                        .setNodeState(new NodeStateBuilder().setNodeStatus(convertToNodeState(state))
                                .build())
                        .build())
                .build();
        tx.merge(LogicalDatastoreType.OPERATIONAL, IdentifierUtils.gnmiNodeID(nodeId), operationalNode);
        return (FluentFuture<CommitInfo>) tx.commit();
    }

    @Override
    public synchronized void close() throws ExecutionException, InterruptedException, TimeoutException {
        LOG.info("Stopping listening on gRPC channel state for node {}", nodeId.getValue());
        listenerActive = false;
        currentState = ConnectivityState.SHUTDOWN;
        // Delete connection state data from operational datastore
        @NonNull WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.OPERATIONAL, IdentifierUtils.gnmiNodeID(nodeId));
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
        LOG.debug("Registering callback on node {} connectivity status change {}", nodeId.getValue(),
                state);
        onStatusCallback = callback;
        callbackDesiredState = state;
    }

}
