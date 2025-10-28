/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.test.gnoi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import gnoi.file.FileOuterClass;
import io.grpc.stub.StreamObserver;
import io.lighty.modules.gnmi.connector.configuration.SessionConfiguration;
import io.lighty.modules.gnmi.connector.session.api.SessionManager;
import io.lighty.modules.gnmi.connector.session.api.SessionProvider;
import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.utils.GnmiSimulatorConfUtils;
import io.lighty.modules.gnmi.test.utils.TestUtils;
import io.lighty.modules.gnmi.test.utils.TimeoutUtil;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(FileServiceTest.class);

    private static final String TEST_SCHEMA_PATH = "src/test/resources/additional/models";
    private static final String SIMULATOR_CONFIG = "/json/simulator_config.json";
    private static final String SERVER_KEY = "src/test/resources/certs/server-pkcs8.key";
    private static final String SERVER_CERT = "src/test/resources/certs/server.crt";
    private static final int  TARGET_PORT = 10161;
    private static final String TARGET_HOST = "127.0.0.1";
    private static final int DUMMYFILE_CHUNKS = 5;

    private static SessionProvider sessionProvider;
    private static SimulatedGnmiDevice target;


    @BeforeEach
    public void setUp() throws Exception {
        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(this.getClass().getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfiguration.setTargetAddress(TARGET_HOST);
        simulatorConfiguration.setTargetPort(TARGET_PORT);
        simulatorConfiguration.setYangsPath(TEST_SCHEMA_PATH);
        simulatorConfiguration.setCertKeyPath(SERVER_KEY);
        simulatorConfiguration.setCertPath(SERVER_CERT);

        target = new SimulatedGnmiDevice(simulatorConfiguration);
        target.start();
        final SessionManager sessionManager = TestUtils.createSessionManagerWithCerts();
        final InetSocketAddress targetAddress = new InetSocketAddress(TARGET_HOST, TARGET_PORT);
        sessionProvider = sessionManager.createSession(
                new SessionConfiguration(targetAddress, false));
    }

    @AfterEach
    public void after() throws Exception {
        sessionProvider.close();
        target.stop();
    }

    @Test
    public void downloadDummyFileTest() throws InterruptedException {
        final FileOuterClass.GetRequest request = FileOuterClass.GetRequest.newBuilder().build();
        final CountDownLatch syncLatch = new CountDownLatch(1);
        final StreamObserver<FileOuterClass.GetResponse> responseStreamObserver = new StreamObserver<>() {
            private FileOuterClass.GetResponse hash;
            private final List<FileOuterClass.GetResponse> chunks = new ArrayList<>();

            @Override
            public void onNext(FileOuterClass.GetResponse value) {
                if (!value.hasHash()) {
                    LOG.info("Received chunk of dummy file: {}", value.getContents().toString());
                    chunks.add(value);
                } else {
                    hash = value;
                }
            }

            @Override
            public void onError(Throwable throwable) {
                fail("Unexpected error received");
            }

            @Override
            public void onCompleted() {
                assertEquals(DUMMYFILE_CHUNKS, chunks.size());
                assertNotNull(hash.getHash());
                syncLatch.countDown();
            }
        };
        sessionProvider.getGnoiSession().getFileInvoker().get(request,responseStreamObserver);
        assertTrue(syncLatch.await(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
    }

}
