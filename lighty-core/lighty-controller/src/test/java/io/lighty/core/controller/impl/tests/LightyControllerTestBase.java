/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl.tests;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
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
public abstract class LightyControllerTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(LightyControllerTestBase.class);

    private LightyController lightyController;

    @BeforeClass(timeOut = 600_000)
    public void startLighty() throws Exception {
        LOG.info("startLighty from TestBase called");
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        lightyController = lightyControllerBuilder.from(ControllerConfigUtils.getDefaultSingleNodeConfiguration()).build();
        ListenableFuture<Boolean> started = lightyController.start();
        started.get();
        LOG.info("startLighty from TestBase finished after sleep");
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
        if (lightyController != null) {
            LOG.info("Shutting down Lighty controller");
            ListenableFuture<Boolean> shutdown = lightyController.shutdown();
            shutdown.get();
            Thread.sleep(10_000);
        }
    }

    private String parseTestNGStatus(int testResultStatus) {
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

}
