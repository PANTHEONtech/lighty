/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.device;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import gnmi.Gnmi;
import gnmi.Gnmi.CapabilityResponse;
import io.grpc.ConnectivityState;
import io.lighty.gnmi.southbound.device.connection.DeviceConnectionInitializer;
import io.lighty.gnmi.southbound.device.connection.DeviceConnectionManager;
import io.lighty.gnmi.southbound.device.session.security.KeystoreGnmiSecurityProvider;
import io.lighty.gnmi.southbound.mountpoint.GnmiMountPointRegistrator;
import io.lighty.gnmi.southbound.mountpoint.broker.GnmiDataBrokerFactory;
import io.lighty.gnmi.southbound.schema.impl.SchemaContextHolderImpl;
import io.lighty.gnmi.southbound.schema.impl.SchemaException;
import io.lighty.modules.gnmi.connector.gnmi.session.api.GnmiSession;
import io.lighty.modules.gnmi.connector.session.SessionManagerFactory;
import io.lighty.modules.gnmi.connector.session.api.SessionManager;
import io.lighty.modules.gnmi.connector.session.api.SessionProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMDataBrokerAdapter;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.connection.parameters.ConnectionParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;

class SessionInitializeTest {

    private static final int NUMBER_OF_NODES = 10;
    private static final long TIMEOUT_MILLIS = 30_000;

    private ScheduledExecutorService scheduledService;

    private DeviceConnectionInitializer connectionInitializer;
    private DeviceConnectionManager connectionManager;

    @Mock
    public GnmiMountPointRegistrator mountPointRegistratorMock;
    @Mock
    private SessionProvider sessionProviderMock;
    @Mock
    private GnmiSession gnmiSessionMock;

    @BeforeEach
    public void setup() {
        final ExecutorService gnmiExecutorService = Executors.newCachedThreadPool();
        scheduledService = Executors.newScheduledThreadPool(100);

        final BindingDOMDataBrokerAdapter dataBrokerMock
                = Mockito.mock(BindingDOMDataBrokerAdapter.class, Answers.RETURNS_DEEP_STUBS);
        final KeystoreGnmiSecurityProvider securityProviderMock = Mockito.mock(KeystoreGnmiSecurityProvider.class);
        final SessionManager sessionManagerMock = Mockito.mock(SessionManager.class);
        final SessionManagerFactory sessionManagerFactoryMock = Mockito.mock(SessionManagerFactory.class);
        final SchemaContextHolderImpl schemaContextHolderMock = Mockito.mock(SchemaContextHolderImpl.class);
        final GnmiDataBrokerFactory gnmiDataBrokerFactoryMock = Mockito.mock(GnmiDataBrokerFactory.class);
        final WriteTransaction txMock = Mockito.mock(WriteTransaction.class);

        MockitoAnnotations.initMocks(this);

        when(sessionManagerFactoryMock.createSessionManager(any()))
                .thenAnswer(invocation -> sessionManagerMock);
        when(sessionManagerMock.createSession(any()))
                .thenAnswer(invocation -> sessionProviderMock);
        when(sessionProviderMock.getChannelState())
                .thenAnswer(invocation -> ConnectivityState.READY);
        when(sessionProviderMock.getGnmiSession())
                .thenAnswer(invocation -> gnmiSessionMock);

        connectionInitializer = new DeviceConnectionInitializer(securityProviderMock, sessionManagerFactoryMock,
                dataBrokerMock, gnmiExecutorService);
        connectionManager = new DeviceConnectionManager(mountPointRegistratorMock, schemaContextHolderMock,
                gnmiDataBrokerFactoryMock, connectionInitializer, dataBrokerMock, gnmiExecutorService);

        when(dataBrokerMock.newWriteOnlyTransaction())
                .thenAnswer(invocation -> txMock);
        when(txMock.commit())
                .thenAnswer(invocation -> CommitInfo.emptyFluentFuture());
    }

    /*
        Tests behaviour of DevicesConnectionManager if nodes are not yet connected by DeviceSessionInitializerProvider.
     */
    @Test
    public void deviceInitializerDevicesConnecting() throws Exception {

        final List<Node> gnmiNodes = prepareGnmiNodes(NUMBER_OF_NODES);

        // Connect devices
        when(sessionProviderMock.getChannelState())
                .thenAnswer(invocation -> ConnectivityState.CONNECTING);
        final List<ListenableFuture<CommitInfo>> futureResults = new ArrayList<>();
        for (Node node : gnmiNodes) {
            futureResults.add(connectionManager.connectDevice(node));
        }

        // Nodes should be in initializing stage, since the future is not set
        for (Node node : gnmiNodes) {
            Assertions.assertTrue(connectionManager.nodeConnecting(node.getNodeId()));
            Assertions.assertFalse(connectionManager.nodeActive(node.getNodeId()));
        }

        // Close initializer, should close all initializing nodes
        connectionInitializer.close();
        for (Node node : gnmiNodes) {
            Assertions.assertFalse(connectionManager.nodeConnecting(node.getNodeId()));
        }

        Assertions.assertTrue(Futures.allAsList(futureResults).isDone());
        Assertions.assertTrue(Futures.allAsList(futureResults).isCancelled());
        Assertions.assertTrue(Futures.successfulAsList(futureResults).get().stream().allMatch(Objects::isNull));

        // Check if mount point register was not called once
        Mockito.verify(mountPointRegistratorMock, times(0))
                .registerMountPoint(any(), any(), any());
    }

    @Test
    public void deviceInitializerDevicesReady() throws Exception {

        final List<Node> gnmiNodes = prepareGnmiNodes(NUMBER_OF_NODES);

        when(gnmiSessionMock.capabilities(any()))
                .thenAnswer(invocation -> Futures.immediateFuture(CapabilityResponse.newBuilder()
                        .addSupportedEncodings(Gnmi.Encoding.JSON_IETF).build()));

        // Connect devices
        final List<ListenableFuture<CommitInfo>> futureResults = new ArrayList<>();
        for (Node node : gnmiNodes) {
            futureResults.add(connectionManager.connectDevice(node));
        }

        Futures.allAsList(futureResults).get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        for (Node node : gnmiNodes) {
            Assertions.assertTrue(connectionManager.nodeActive(node.getNodeId()));
            Assertions.assertFalse(connectionManager.nodeConnecting(node.getNodeId()));
        }

        // TODO assert that reading calls session
    }

    /*
        Tests behaviour of DevicesConnectionManager and DeviceSessionInitializerProvider if nodes becomes connected
         after the underlying gRPC channel state eventually (after some state changes) becomes READY
     */
    @Test
    public void sessionCreationOnListenerStatusChangeTestAfter500ms()
            throws InterruptedException, ExecutionException, TimeoutException, SchemaException {

        final List<Node> gnmiNodes = prepareGnmiNodes(NUMBER_OF_NODES);

        when(gnmiSessionMock.capabilities(any()))
                .thenAnswer(invocation -> Futures.scheduleAsync(
                    () -> Futures.immediateFuture(CapabilityResponse.newBuilder()
                            .addSupportedEncodings(Gnmi.Encoding.JSON_IETF).build()),
                        ThreadLocalRandom.current().nextLong(500),
                        TimeUnit.MILLISECONDS, scheduledService));

        ArgumentCaptor<Runnable> updateStatusCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(sessionProviderMock).notifyOnStateChangedOneOff(any(), updateStatusCaptor.capture());

        // Connect devices
        when(sessionProviderMock.getChannelState())
            .thenAnswer(invocation -> ConnectivityState.CONNECTING);
        final List<ListenableFuture<CommitInfo>> futureResults = new ArrayList<>();
        for (Node node : gnmiNodes) {
            futureResults.add(connectionManager.connectDevice(node));
        }

        for (Node node : gnmiNodes) {
            Assertions.assertTrue(connectionManager.nodeConnecting(node.getNodeId()));
            Assertions.assertFalse(connectionManager.nodeActive(node.getNodeId()));
        }

        // invoke channel state
        when(sessionProviderMock.getChannelState())
                .thenAnswer(invocation -> ConnectivityState.READY);
        updateStatusCaptor.getAllValues().stream().forEach(cap -> cap.run());
        Futures.allAsList(futureResults).get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);


        // Assert all devices are connected (active)
        for (Node node : gnmiNodes) {
            // Node should be active now
            Assertions.assertTrue(connectionManager.nodeActive(node.getNodeId()));
            // Node should not be initializing now
            Assertions.assertFalse(connectionInitializer.isNodeConnecting(node.getNodeId()));
        }

        // Check that mount point create was called NUMBER_OF_NODES times
        Mockito.verify(mountPointRegistratorMock, times(NUMBER_OF_NODES)).registerMountPoint(any(),
                any(), any());
    }

    private static List<Node> prepareGnmiNodes(final int nodeCount) {
        List<Node> nodeList = new ArrayList<>();
        nodeList = IntStream.range(0, nodeCount)
                .mapToObj(i -> createNode("node-" + i, i + 9000))
                .collect(Collectors.toList());

        return nodeList;
    }

    private static Node createNode(final String nameOfNode, final int port) {
        return new NodeBuilder()
                .setNodeId(new NodeId(nameOfNode))
                .addAugmentation(new GnmiNodeBuilder().setConnectionParameters(
                        new ConnectionParametersBuilder()
                                .setHost(new Host(new IpAddress(Ipv4Address.getDefaultInstance("127.0.0.1"))))
                                .setPort(new PortNumber(Uint16.valueOf(port)))
                                .build())
                        .build())
                .build();
    }
}
