/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.test.gnoi;

import gnoi.file.FileOuterClass;
import io.grpc.stub.StreamObserver;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.gnmi.connector.configuration.SessionConfiguration;
import io.lighty.modules.gnmi.connector.session.api.SessionManager;
import io.lighty.modules.gnmi.connector.session.api.SessionProvider;
import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDeviceBuilder;
import io.lighty.modules.gnmi.test.utils.TestUtils;
import io.lighty.modules.gnmi.test.utils.TimeoutUtil;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(FileServiceTest.class);

    private static final String TEST_SCHEMA_PATH = "src/test/resources/simulator_models";
    private static final int  TARGET_PORT = 10161;
    private static final String TARGET_HOST = "127.0.0.1";
    private static final int DUMMYFILE_CHUNKS = 5;

    private static SessionProvider sessionProvider;
    private static SimulatedGnmiDevice target;


    @Before
    public void setUp() throws NoSuchAlgorithmException, CertificateException, InvalidKeySpecException, IOException,
            URISyntaxException, ConfigurationException {

        final GnmiSimulatorConfiguration simulatorConfiguration = new GnmiSimulatorConfiguration();
        simulatorConfiguration.setTargetAddress(TARGET_HOST);
        simulatorConfiguration.setTargetPort(TARGET_PORT);
        simulatorConfiguration.setYangsPath(TEST_SCHEMA_PATH);

        target = new SimulatedGnmiDeviceBuilder().from(simulatorConfiguration).build();
        target.start();
        final SessionManager sessionManager = TestUtils.createSessionManagerWithCerts();
        final InetSocketAddress targetAddress = new InetSocketAddress(TARGET_HOST, TARGET_PORT);
        sessionProvider = sessionManager.createSession(
                new SessionConfiguration(targetAddress, false));
    }

    @After
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
                Assert.fail("Unexpected error received");
            }

            @Override
            public void onCompleted() {
                Assert.assertEquals(DUMMYFILE_CHUNKS, chunks.size());
                Assert.assertNotNull(hash.getHash());
                syncLatch.countDown();
            }
        };
        sessionProvider.getGnoiSession().getFileInvoker().get(request,responseStreamObserver);
        Assert.assertTrue(syncLatch.await(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));

    }

}
