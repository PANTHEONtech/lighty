/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.device.connection;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import gnmi.Gnmi;
import io.lighty.gnmi.southbound.capabilities.GnmiDeviceCapability;
import io.lighty.gnmi.southbound.capabilities.MissingEncodingException;
import io.lighty.gnmi.southbound.device.session.listener.GnmiConnectionStatusException;
import io.lighty.gnmi.southbound.device.session.security.SessionSecurityException;
import io.lighty.gnmi.southbound.identifier.IdentifierUtils;
import io.lighty.gnmi.southbound.mountpoint.GnmiMountPointRegistrator;
import io.lighty.gnmi.southbound.mountpoint.broker.GnmiDataBroker;
import io.lighty.gnmi.southbound.mountpoint.broker.GnmiDataBrokerFactory;
import io.lighty.gnmi.southbound.requests.utils.GnmiRequestUtils;
import io.lighty.gnmi.southbound.schema.SchemaContextHolder;
import io.lighty.gnmi.southbound.schema.impl.SchemaException;
import io.lighty.gnmi.southbound.timeout.TimeoutUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.node.state.NodeStateBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.node.state.node.state.AvailableCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.node.state.node.state.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.node.state.node.state.available.capabilities.AvailableCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Establishes and holds connections of gNMI devices.
 */
public class DeviceConnectionManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceConnectionManager.class);

    private final GnmiMountPointRegistrator mountPointRegistrator;
    private final SchemaContextHolder schemaContextHolder;
    private final GnmiDataBrokerFactory gnmiDataBrokerFactory;
    private final Map<NodeId, DeviceConnection> activeDevices;
    private final DeviceConnectionInitializer connectionInitializer;
    private final DataBroker dataBroker;
    private final ExecutorService executorService;

    public DeviceConnectionManager(final GnmiMountPointRegistrator mountPointRegistrator,
            final SchemaContextHolder schemaContextHolder, final GnmiDataBrokerFactory gnmiDataBrokerFactory,
            final DeviceConnectionInitializer connectionInitializer, final DataBroker dataBroker,
            final ExecutorService executors) {
        this.mountPointRegistrator = mountPointRegistrator;
        this.schemaContextHolder = schemaContextHolder;
        this.gnmiDataBrokerFactory = gnmiDataBrokerFactory;
        this.connectionInitializer = connectionInitializer;
        this.dataBroker = dataBroker;
        this.executorService = executors;
        this.activeDevices = new ConcurrentHashMap<>();
    }

    public ListenableFuture<CommitInfo> connectDevice(final Node node) {
        if (!activeDevices.containsKey(node.getNodeId())) {
            try {
                /*
                 Establish connection with device (future will be set with GnmiDeviceManager for device when
                 state of the gRPC channel is READY
                 */
                final ListenableFuture<DeviceConnection> deviceConnectionFuture =
                    connectionInitializer.initConnection(node);

                return Futures.transformAsync(deviceConnectionFuture,
                    deviceConnection -> prepareDeviceConnection(node, deviceConnection),
                    executorService);

            } catch (SessionSecurityException e) {
                return Futures.immediateFailedFuture(e);
            }

        } else {
            LOG.debug("Node {} is already active", node.getNodeId());
            return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<CommitInfo> prepareDeviceConnection(final Node node,
            final DeviceConnection deviceConnection) {
        final ListenableFuture<Void> mountPointCreatedFuture = createMountPoint(node, deviceConnection);

        return Futures.transformAsync(mountPointCreatedFuture,
            voidResult -> {
                final FluentFuture<CommitInfo> statusReadyFuture = deviceConnection.setDeviceStatusReady();

                // handle GnmiConnectionStatusException in `statusReadyFuture`
                return Futures.catchingAsync(statusReadyFuture, GnmiConnectionStatusException.class,
                    statusException -> {
                        LOG.error("Connection status unexpectedly changed from READY to {} while creating"
                                + " Mountpoint", statusException.getCurrentState());
                        throw statusException;
                    },
                    executorService);
            },
            executorService);
    }

    private ListenableFuture<Void> createMountPoint(final Node node, final DeviceConnection deviceConnection) {

        final ListenableFuture<Gnmi.CapabilityResponse> readCapabilitiesFuture =
                deviceConnection.getGnmiSession().capabilities(GnmiRequestUtils.makeDefaultCapabilityRequest());

        return Futures.transformAsync(readCapabilitiesFuture,
            capabilityResponse -> {
                LOG.debug("Received gNMI Capabiltiies response from {} : {}",node.getNodeId(), capabilityResponse);

                if (!capabilityResponse.getSupportedEncodingsList().contains(Gnmi.Encoding.JSON_IETF)) {
                    return Futures.immediateFailedFuture(
                            new MissingEncodingException("gNMI Device must support JSON_IETF encoding"));
                }

                final List<GnmiDeviceCapability> capabilitiesList = new ArrayList<>();
                final Optional<List<Gnmi.ModelData>> forceCapabilities =
                    deviceConnection.getConfigurableParameters().getModelDataList();
                if (forceCapabilities.isPresent()) {
                    final Gnmi.CapabilityResponse capabilitiesResponseBuilder =
                        Gnmi.CapabilityResponse.newBuilder().addAllSupportedModels(forceCapabilities.get()).build();
                    capabilitiesList.addAll(GnmiRequestUtils.fromCapabilitiesResponse(capabilitiesResponseBuilder));
                } else {
                    capabilitiesList.addAll(GnmiRequestUtils.fromCapabilitiesResponse(capabilityResponse));
                }
                try {
                    final EffectiveModelContext schemaContext = schemaContextHolder.getSchemaContext(capabilitiesList);
                    deviceConnection.setSchemaContext(schemaContext);
                    final GnmiDataBroker gnmiDataBroker = gnmiDataBrokerFactory.create(deviceConnection);
                    mountPointRegistrator.registerMountPoint(node, schemaContext, gnmiDataBroker);
                    activeDevices.put(node.getNodeId(), deviceConnection);
                    saveCapabilitiesList(node.getNodeId(), capabilitiesList);
                    return Futures.immediateFuture(null);

                } catch (SchemaException | ExecutionException | TimeoutException e) {
                    return Futures.immediateFailedFuture(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Futures.immediateFailedFuture(e);
                }
            },
            executorService);
    }

    private void saveCapabilitiesList(final NodeId nodeId, final List<GnmiDeviceCapability> gnmiDeviceCapabilities)
            throws InterruptedException, ExecutionException, TimeoutException {

        final List<AvailableCapability> capabilityList = gnmiDeviceCapabilities.stream()
                .map(cap -> new AvailableCapabilityBuilder().setCapability(cap.toString()).build())
                .collect(Collectors.toList());

        final Node operationalNode = new NodeBuilder()
                .setNodeId(nodeId)
                .addAugmentation(new GnmiNodeBuilder()
                        .setNodeState(new NodeStateBuilder()
                                .setAvailableCapabilities(new AvailableCapabilitiesBuilder()
                                        .setAvailableCapability(capabilityList)
                                        .build())
                                .build())
                        .build())
                .build();

        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.OPERATIONAL, IdentifierUtils.gnmiNodeIID(nodeId), operationalNode);
        tx.commit().get(TimeoutUtils.DATASTORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    public boolean nodeActive(final NodeId nodeId) {
        return activeDevices.containsKey(nodeId);
    }

    public boolean nodeConnecting(final NodeId nodeId) {
        return connectionInitializer.isNodeConnecting(nodeId);
    }

    @SuppressWarnings({"checkstyle:illegalCatch"})
    public void closeConnection(final NodeId nodeId) {
        if (!nodeActive(nodeId) && !nodeConnecting(nodeId)) {
            LOG.warn("Node {} is not registered, not deleting", nodeId);
        }
        if (nodeConnecting(nodeId)) {
            try {
                connectionInitializer.cancelInitializer(nodeId);
            } catch (Exception e) {
                LOG.warn("Failed closing initializer of connecting node {}", nodeId);
            }
        }
        if (activeDevices.containsKey(nodeId)) {
            final DeviceConnection deviceConnection = activeDevices.get(nodeId);
            try {
                deviceConnection.close();
            } catch (Exception e) {
                LOG.warn("Failed closing device manager of connected node {}", nodeId);
            }
            activeDevices.remove(nodeId);
            mountPointRegistrator.unregisterMountPoint(nodeId);
        }
    }

    @Override
    public void close() {
        LOG.info("Closing all connections to devices");
        for (NodeId nodeId : Sets.union(connectionInitializer.getActiveInitializers(), activeDevices.keySet())) {
            closeConnection(nodeId);
        }
    }
}
