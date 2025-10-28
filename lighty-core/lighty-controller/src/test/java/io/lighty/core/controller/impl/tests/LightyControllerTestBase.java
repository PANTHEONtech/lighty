/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.tests;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public abstract class LightyControllerTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(LightyControllerTestBase.class);
    public static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000;

    private LightyController lightyController;

    @BeforeClass()
    public void startLighty() throws Exception {
        LOG.info("startLighty from TestBase called");
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        lightyController = lightyControllerBuilder.from(ControllerConfigUtils.getDefaultSingleNodeConfiguration())
                .build();
        ListenableFuture<Boolean> started = lightyController.start();
        started.get();
        LOG.info("startLighty from TestBase finished after sleep");
    }

    @BeforeMethod
    public void handleTestMethodName(final Method method) {
        String testName = method.getName();
        LOG.info("Running test {}", testName);
    }

    @AfterMethod
    public void afterTest(final ITestResult result) {
        LOG.info("Test {} completed and resulted in {}, with throwables {}",
                result.getName(), parseTestNGStatus(result.getStatus()), result.getThrowable());
    }

    @AfterClass
    public void shutdownLighty() {
        if (lightyController != null) {
            LOG.info("Shutting down Lighty controller");
            lightyController.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    private static String parseTestNGStatus(final int testResultStatus) {
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
