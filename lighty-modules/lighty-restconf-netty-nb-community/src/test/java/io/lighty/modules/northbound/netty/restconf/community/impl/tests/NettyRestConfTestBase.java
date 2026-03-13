/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.netty.restconf.community.impl.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.aaa.config.CertificateManagerConfig;
import io.lighty.aaa.util.AAAConfigUtils;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.netty.restconf.community.impl.NettyRestConf;
import io.lighty.modules.northbound.netty.restconf.community.impl.NettyRestConfBuilder;
import io.lighty.modules.northbound.netty.restconf.community.impl.util.NettyRestConfUtils;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NettyRestConfTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(NettyRestConfTestBase.class);
    private static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000;

    private static LightyController lightyController;
    private static NettyRestConf nettyRestConf;

    @RegisterExtension
    final TestWatcher resultLogger = new TestWatcher() {
        @Override
        public void testSuccessful(ExtensionContext context) {
            LOG.info("Test {} completed and resulted in SUCCESS, with throwables null",
                context.getRequiredTestMethod().getName());
        }

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            LOG.info("Test {} completed and resulted in FAILURE, with throwables {}",
                context.getRequiredTestMethod().getName(), cause);
        }

        @Override
        public void testAborted(ExtensionContext context, Throwable cause) {
            LOG.info("Test {} completed and resulted in SKIP/ABORTED, with throwables {}",
                context.getRequiredTestMethod().getName(), cause);
        }

        @Override
        public void testDisabled(ExtensionContext context, Optional<String> reason) {
            LOG.info("Test {} completed and resulted in DISABLED",
                context.getRequiredTestMethod().getName());
        }
    };

    @BeforeAll
    @Timeout(value = 60_000, unit = TimeUnit.MILLISECONDS)
    public static void startControllerAndRestConf() throws Exception {
        final var moduleInfos = new HashSet<>(NettyRestConfUtils.YANG_MODELS);
        moduleInfos.add(org.opendaylight.yang.svc.v1.instance.identifier.patch.module.rev151121
            .YangModuleInfoImpl.getInstance());
        moduleInfos.add(org.opendaylight.yang.svc.v1.urn.opendaylight.yang.aaa.cert.mdsal.rev160321
            .YangModuleInfoImpl.getInstance());

        LOG.info("Building LightyController");
        final var lightyControllerBuilder = new LightyControllerBuilder();
        lightyController = lightyControllerBuilder.from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(
                moduleInfos)).build();

        LOG.info("Starting LightyController (waiting 20s after start)");
        final var started = lightyController.start();
        assertEquals(Boolean.TRUE, started.get(20_000, TimeUnit.MILLISECONDS),
            "Lighty controller was not started correctly");
        LOG.info("LightyController started");

        final var defaultAAAConfiguration = AAAConfigUtils.createDefaultAAAConfiguration();
        defaultAAAConfiguration.setCertificateManager(
            CertificateManagerConfig.getDefault(lightyController.getServices().getBindingDataBroker(),
                lightyController.getServices().getRpcProviderService()));

        LOG.info("Building NettyRestConf");
        final var builder = NettyRestConfBuilder.from(
                NettyRestConfUtils.getDefaultNettyRestConfConfiguration(lightyController.getServices()))
            .withWebEnvironment(NettyRestConfUtils.getAaaWebEnvironment(
                lightyController.getServices().getBindingDataBroker(),
                lightyController.getServices().getRpcProviderService(),
               defaultAAAConfiguration));
        nettyRestConf = builder.build();

        LOG.info("Starting NettyRestConf (waiting 10s after start)");
        assertEquals(Boolean.TRUE, nettyRestConf.start().get(10_000, TimeUnit.MILLISECONDS),
            "Lighty NettyRestConf module was not started correctly");
        LOG.info("NettyRestConf started");
    }

    @BeforeEach
    public void handleTestMethodName(final TestInfo testInfo) {
        String testName = testInfo.getTestMethod().map(Method::getName).orElse(testInfo.getDisplayName());
        LOG.info("Running test {}", testName);
    }

    @AfterAll
    public static void shutdownLighty() {
        boolean nettyShutdownResult = true;
        boolean lightyShutdownResult = true;
        if (nettyRestConf != null) {
            nettyShutdownResult = nettyRestConf.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
        if (lightyController != null) {
            lightyShutdownResult = lightyController.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
        assertTrue(nettyShutdownResult, "Netty failed to shutdown");
        assertTrue(lightyShutdownResult, "Lighty failed to shutdown");
    }

    LightyController getLightyController() {
        return lightyController;
    }

    NettyRestConf getNettyRestConf() {
        return nettyRestConf;
    }
}