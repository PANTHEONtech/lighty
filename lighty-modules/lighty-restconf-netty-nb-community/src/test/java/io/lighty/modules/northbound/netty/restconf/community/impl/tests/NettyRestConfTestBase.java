/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.netty.restconf.community.impl.tests;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.aaa.config.CertificateManagerConfig;
import io.lighty.aaa.util.AAAConfigUtils;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.netty.restconf.community.impl.NettyRestConf;
import io.lighty.modules.northbound.netty.restconf.community.impl.NettyRestConfBuilder;
import io.lighty.modules.northbound.netty.restconf.community.impl.util.NettyRestConfUtils;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * author: vincent on 15.8.2017.
 */
public abstract class NettyRestConfTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(NettyRestConfTestBase.class);
    public static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000;

    private LightyController lightyController;
    private NettyRestConf nettyRestConf;

    @BeforeClass(timeOut = 60_000)
    public void startControllerAndRestConf() throws Exception {

        final Set<YangModuleInfo> moduleInfos = new java.util.HashSet<>(NettyRestConfUtils.YANG_MODELS);
        moduleInfos.add(org.opendaylight.yang.svc.v1.instance.identifier.patch.module.rev151121
            .YangModuleInfoImpl.getInstance());
        moduleInfos.add(org.opendaylight.yang.svc.v1.urn.opendaylight.yang.aaa.cert.mdsal.rev160321
                .YangModuleInfoImpl.getInstance());

        LOG.info("Building LightyController");
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        lightyController = lightyControllerBuilder.from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(
                moduleInfos)).build();

        LOG.info("Starting LightyController (waiting 10s after start)");
        ListenableFuture<Boolean> started = lightyController.start();
        started.get();
        LOG.info("LightyController started");

        final var defaultAAAConfiguration = AAAConfigUtils.createDefaultAAAConfiguration();
        defaultAAAConfiguration.setCertificateManager(
            CertificateManagerConfig.getDefault(lightyController.getServices().getBindingDataBroker(),
                lightyController.getServices().getRpcProviderService()));

        LOG.info("Building NettyRestConf");
        NettyRestConfBuilder builder = NettyRestConfBuilder.from(
                NettyRestConfUtils.getDefaultNettyRestConfConfiguration(lightyController.getServices()))
            .withWebEnvironment(NettyRestConfUtils.getAaaWebEnvironment(
                lightyController.getServices().getBindingDataBroker(),
                lightyController.getServices().getRpcProviderService(),
               defaultAAAConfiguration));
        nettyRestConf = builder.build();

        LOG.info("Starting NettyRestConf (waiting 10s after start)");
        nettyRestConf.start().get(10_000, TimeUnit.MILLISECONDS);
        LOG.info("NettyRestConf started");
    }

    @BeforeMethod
    public void handleTestMethodName(Method method) {
        String testName = method.getName();
        LOG.info("Running test {}", testName);
    }

    @AfterMethod
    public void afterTest(ITestResult result) {
        LOG.info("Test {} completed and resulted in {}, with throwables {}",
                result.getName(), parseTestNGStatus(result.getStatus()), result.getThrowable());
    }

    @AfterClass
    public void shutdownLighty() {
        if (nettyRestConf != null) {
            nettyRestConf.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
        if (lightyController != null) {
            lightyController.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    private static String parseTestNGStatus(int testResultStatus) {
        switch (testResultStatus) {
            case -1:
                return "CREATED";
            case 1:
                return "SUCCESS";
            case 2:
                return "FAILURE";
            case 3:
                return "SKIP";
            case 4:
                return "SUCCESS_PERCENTAGE_FAILURE";
            case 16:
                return "STARTED";
            default:
                return "N/A";
        }
    }

    LightyController getLightyController() {
        return lightyController;
    }

    NettyRestConf getNettyRestConf() {
        return nettyRestConf;
    }
}
