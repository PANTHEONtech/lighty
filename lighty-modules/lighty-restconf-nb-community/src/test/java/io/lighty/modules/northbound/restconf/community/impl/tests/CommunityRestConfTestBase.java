/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    public static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000;
    public static final long SLEEP_AFTER_SHUTDOWN_TIMEOUT_MILLIS = 3_000;

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
        CommunityRestConfBuilder builder = CommunityRestConfBuilder.from(
                RestConfConfigUtils.getDefaultRestConfConfiguration(lightyController.getServices()));
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

    @SuppressWarnings("checkstyle:illegalCatch")
    @AfterClass
    public void shutdownLighty() {
        if (communityRestConf != null) {
            LOG.info("Shutting down CommunityRestConf");
            try {
                communityRestConf.shutdown().get(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                Thread.sleep(SLEEP_AFTER_SHUTDOWN_TIMEOUT_MILLIS);
            } catch (InterruptedException e) {
                LOG.error("Interrupted while shutting down CommunityRestConf ", e);
            } catch (TimeoutException e) {
                LOG.error("Timeout while shutting down CommunityRestConf ", e);
            } catch (ExecutionException e) {
                LOG.error("Execution of CommunityRestConf shutdown failed", e);
            }

        }
        if (lightyController != null) {
            LOG.info("Shutting down LightyController");
            try {
                lightyController.shutdown().get(SHUTDOWN_TIMEOUT_MILLIS,TimeUnit.MILLISECONDS);
                Thread.sleep(SLEEP_AFTER_SHUTDOWN_TIMEOUT_MILLIS);
            } catch (Exception e) {
                LOG.error("Shutdown of LightyController failed", e);
            }
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
