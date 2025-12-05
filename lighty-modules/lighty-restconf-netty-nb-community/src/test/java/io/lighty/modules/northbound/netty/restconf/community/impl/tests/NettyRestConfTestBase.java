/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.netty.restconf.community.impl.tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

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
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public abstract class NettyRestConfTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(NettyRestConfTestBase.class);
    private static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000;

    private LightyController lightyController;
    private NettyRestConf nettyRestConf;

    @BeforeClass(timeOut = 60_000)
    public void startControllerAndRestConf() throws Exception {
        final var moduleInfos = new HashSet<>(NettyRestConfUtils.YANG_MODELS);
        moduleInfos.add(org.opendaylight.yang.svc.v1.instance.identifier.patch.module.rev151121
            .YangModuleInfoImpl.getInstance());
        moduleInfos.add(org.opendaylight.yang.svc.v1.urn.opendaylight.yang.aaa.cert.mdsal.rev160321
            .YangModuleInfoImpl.getInstance());

        LOG.info("Building LightyController");
        final var lightyControllerBuilder = new LightyControllerBuilder();
        lightyController = lightyControllerBuilder.from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(
                moduleInfos)).build();

        LOG.info("Starting LightyController (waiting 10s after start)");
        final var started = lightyController.start();
        assertEquals(started.get(10_000, TimeUnit.MILLISECONDS), Boolean.TRUE,
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
        assertEquals(nettyRestConf.start().get(10_000, TimeUnit.MILLISECONDS), Boolean.TRUE,
            "Lighty NettyRestConf module was not started correctly");
        LOG.info("NettyRestConf started");
    }

    @BeforeMethod
    public void handleTestMethodName(final Method method) {
        LOG.info("Running test {}", method.getName());
    }

    @AfterMethod
    public void afterTest(final ITestResult result) {
        LOG.info("Test {} completed and resulted in {}, with throwables {}",
                result.getName(), parseTestNGStatus(result.getStatus()), result.getThrowable());
    }

    @AfterClass
    public void shutdownLighty() {
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

    private static String parseTestNGStatus(final int testResultStatus) {
        return switch (testResultStatus) {
            case -1 -> "CREATED";
            case 1 -> "SUCCESS";
            case 2 -> "FAILURE";
            case 3 -> "SKIP";
            case 4 -> "SUCCESS_PERCENTAGE_FAILURE";
            case 16 -> "STARTED";
            default -> "N/A";
        };
    }

    LightyController getLightyController() {
        return lightyController;
    }

    NettyRestConf getNettyRestConf() {
        return nettyRestConf;
    }
}
