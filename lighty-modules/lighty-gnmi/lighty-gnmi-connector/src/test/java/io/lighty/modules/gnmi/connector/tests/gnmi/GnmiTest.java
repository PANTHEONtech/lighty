/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.tests.gnmi;

import com.google.common.util.concurrent.ListenableFuture;
import gnmi.Gnmi;
import gnmi.gNMIGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.lighty.modules.gnmi.connector.configuration.SessionConfiguration;
import io.lighty.modules.gnmi.connector.gnmi.session.api.GnmiSession;
import io.lighty.modules.gnmi.connector.gnmi.util.AddressUtil;
import io.lighty.modules.gnmi.connector.session.api.SessionManager;
import io.lighty.modules.gnmi.connector.session.api.SessionProvider;
import io.lighty.modules.gnmi.connector.tests.commons.TestUtils;
import io.lighty.modules.gnmi.connector.tests.commons.TimeoutUtil;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

public class GnmiTest {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiTest.class);
    private static final InetSocketAddress DEFAULT_SERVER_ADDRESS = new InetSocketAddress(AddressUtil.LOCALHOST, 9090);

    private TestGrpcServiceImpl service;
    private Server server;

    @BeforeEach
    public void before() throws IOException {
        service = new TestGrpcServiceImpl();
        server = ServerBuilder
                .forPort(DEFAULT_SERVER_ADDRESS.getPort())
                .addService(service)
                .build();
        LOG.info("Starting server");
        server.start();
    }

    @AfterEach
    public void after() {
        LOG.info("Shutting down server");
        server.shutdown();
        try {
            if (!server.awaitTermination(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Termination of server failed!");
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting for server shutdown");
        }
    }

    @Test
    public void rawGnmiTest() {
        LOG.info("Creating channel to server");
        final ManagedChannel channel = ManagedChannelBuilder
                .forAddress(DEFAULT_SERVER_ADDRESS.getAddress().getHostAddress(), DEFAULT_SERVER_ADDRESS.getPort())
                .usePlaintext()
                .build();

        final gNMIGrpc.gNMIBlockingStub stub = gNMIGrpc.newBlockingStub(channel);
        String gnmiVersion = "version 1";
        service.gnmiVersion = gnmiVersion;

        final Gnmi.CapabilityRequest request = Gnmi.CapabilityRequest.newBuilder().build();
        Gnmi.CapabilityResponse response = stub.capabilities(request);
        Assert.assertEquals(gnmiVersion, response.getGNMIVersion());

        gnmiVersion = "version 2";
        service.gnmiVersion = "version 2";
        response = stub.capabilities(request);
        Assert.assertEquals(gnmiVersion, response.getGNMIVersion());

        LOG.info("Shutting down channel");
        channel.shutdown();
    }

    @Test
    @SuppressWarnings("IllegalCatch")
    public void lightyGnmiSessionTest() throws Exception {
        final SessionManager sessionManager = TestUtils.createSessionManagerWithCerts();
        try (SessionProvider session =
                     sessionManager.createSession(new SessionConfiguration(DEFAULT_SERVER_ADDRESS, true))) {

            final Gnmi.CapabilityRequest request = Gnmi.CapabilityRequest.newBuilder().build();

            String gnmiVersion = "version 1";
            service.gnmiVersion = gnmiVersion;
            ListenableFuture<Gnmi.CapabilityResponse> result = session.getGnmiSession().capabilities(request);
            Assert.assertEquals(gnmiVersion,
                    result.get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).getGNMIVersion());

            gnmiVersion = "version 2";
            service.gnmiVersion = gnmiVersion;
            result = session.getGnmiSession().capabilities(request);
            Assert.assertEquals(gnmiVersion,
                    result.get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).getGNMIVersion());
        } catch (Exception e) {
            Assert.fail("Exception thrown!", e);
        }
    }

    @Test
    public void sessionManagerChannelCachingTest() throws Exception {
        // create second server
        final TestGrpcServiceImpl service2 = new TestGrpcServiceImpl();
        final Server server2 = ServerBuilder.forPort(8081).addService(service2).build();
        server2.start();

        // prepare configurations
        final SessionConfiguration config = new SessionConfiguration(DEFAULT_SERVER_ADDRESS, true);
        final SessionConfiguration config2 = new SessionConfiguration(new InetSocketAddress(
                AddressUtil.LOCALHOST, 8081), true);

        final String gnmiVersion = "version 1";
        service.gnmiVersion = gnmiVersion;
        final String gnmiVersion2 = "version 2";
        service2.gnmiVersion = gnmiVersion2;

        // create session manager and get caching unmodifiable maps
        final SessionManager sessionManager = TestUtils.createSessionManagerWithCerts();
        final Map<SessionConfiguration, ManagedChannel> channelCache = sessionManager.getChannelCache();
        final Map<SessionConfiguration, Integer> openSessionsCounter = sessionManager.getOpenSessionsCounter();

        Assert.assertTrue(channelCache.isEmpty());
        Assert.assertTrue(openSessionsCounter.isEmpty());

        // create sessions
        ArrayList<SessionProvider> sessions = new ArrayList<>(10);
        ArrayList<SessionProvider> sessions2 = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            sessions.add(sessionManager.createSession(config));
            sessions2.add(sessionManager.createSession(config2));
        }

        // check if everything is OK in cache
        Assert.assertEquals(channelCache.size(), 2);
        Assert.assertEquals(openSessionsCounter.size(), 2);
        Assert.assertEquals(openSessionsCounter.get(config), Integer.valueOf(10));
        Assert.assertEquals(openSessionsCounter.get(config2), Integer.valueOf(10));

        // close half of the sessions
        for (int i = sessions.size() - 1; i >= 5; i--) {
            sessions.remove(i).close();
            sessions2.remove(i).close();
        }

        // check if everything is OK in cache
        Assert.assertEquals(channelCache.size(), 2);
        Assert.assertEquals(openSessionsCounter.size(), 2);
        Assert.assertEquals(openSessionsCounter.get(config), Integer.valueOf(5));
        Assert.assertEquals(openSessionsCounter.get(config2), Integer.valueOf(5));

        // send capabilities request from remaining sessions
        final Gnmi.CapabilityRequest request = Gnmi.CapabilityRequest.newBuilder().build();
        assertCapabilitiesVersion(gnmiVersion, request,
                sessions.stream().map(SessionProvider::getGnmiSession).collect(Collectors.toList()));
        assertCapabilitiesVersion(gnmiVersion2, request,
                sessions2.stream().map(SessionProvider::getGnmiSession).collect(Collectors.toList()));

        // close sessions to first server and check cache
        for (SessionProvider session : sessions) {
            session.close();
        }
        Assert.assertEquals(channelCache.size(), 1);
        Assert.assertEquals(openSessionsCounter.size(), 1);
        Assert.assertEquals(openSessionsCounter.get(config2), Integer.valueOf(5));

        // close sessions to second server and check cache
        for (SessionProvider session : sessions2) {
            session.close();
        }
        Assert.assertTrue(channelCache.isEmpty());
        Assert.assertTrue(openSessionsCounter.isEmpty());

        server2.shutdown();
    }

    private void assertCapabilitiesVersion(final String expectedGnmiVersion, final Gnmi.CapabilityRequest request,
                                           final List<GnmiSession> sessionsToCheck) {
        sessionsToCheck.forEach(gnmiSession -> {
            try {
                Assert.assertEquals(expectedGnmiVersion,
                        gnmiSession.capabilities(request)
                                .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).getGNMIVersion());
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Assert.fail("Exception thrown!", e);
            }
        });
    }

    private static final class TestGrpcServiceImpl extends gNMIGrpc.gNMIImplBase {
        private String gnmiVersion = null;

        @Override
        public void capabilities(final gnmi.Gnmi.CapabilityRequest request,
                                 final StreamObserver<Gnmi.CapabilityResponse> responseObserver) {
            LOG.info("Service: got request: {} - {}", request.getClass(), request);
            final Gnmi.CapabilityResponse response = Gnmi.CapabilityResponse.newBuilder()
                    .setGNMIVersion(gnmiVersion).build();
            LOG.info("Service: returning response: {}", response);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

}
