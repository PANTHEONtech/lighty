/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.restconf.community.impl.tests;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import java.lang.reflect.Method;
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
public abstract class CommunityRestConfTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(CommunityRestConfTestBase.class);

    private LightyController lightyController;
    private CommunityRestConf communityRestConf;

    @BeforeClass(timeOut = 60_000)
    public void startControllerAndRestConf() throws Exception {
        LOG.info("Building LightyController");
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        lightyController = lightyControllerBuilder.from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(
                RestConfConfigUtils.YANG_MODELS)).build();

        LOG.info("Starting LightyController (waiting 10s after start)");
        ListenableFuture<Boolean> started = lightyController.start();
        started.get();
        LOG.info("LightyController started");

        LOG.info("Building CommunityRestConf");
        CommunityRestConfBuilder builder = new CommunityRestConfBuilder();
        builder.from(RestConfConfigUtils.getDefaultRestConfConfiguration(lightyController.getServices()));
        communityRestConf = builder.build();

        LOG.info("Starting CommunityRestConf (waiting 10s after start)");
        communityRestConf.start();
        LOG.info("CommunityRestConf started");
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
    public void shutdownLighty() throws Exception {
        if (communityRestConf != null) {
            LOG.info("Shutting down CommunityRestConf");
            ListenableFuture<Boolean> shutdown = communityRestConf.shutdown();
            shutdown.get();
            Thread.sleep(3_000);
        }
        if (lightyController != null) {
            LOG.info("Shutting down LightyController");
            ListenableFuture<Boolean> shutdown = lightyController.shutdown();
            shutdown.get();
            Thread.sleep(1_000);
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

    CommunityRestConf getCommunityRestConf() {
        return communityRestConf;
    }
}
