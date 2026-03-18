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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class LightyControllerTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(LightyControllerTestBase.class);
    public static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000;

    private static LightyController lightyController;

    @BeforeAll
    static void startLighty() throws Exception {
        LOG.info("startLighty from TestBase called");
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        lightyController = lightyControllerBuilder.from(ControllerConfigUtils.getDefaultSingleNodeConfiguration())
                .build();
        ListenableFuture<Boolean> started = lightyController.start();
        started.get();

        LOG.info("startLighty from TestBase finished after sleep");
    }

    @BeforeEach
    void handleTestMethodName(final TestInfo testInfo) {
        String testName = testInfo.getTestMethod()
            .map(Method::getName)
            .orElse("unknown");

        LOG.info("Running test {}", testName);
    }

    @AfterEach
    void afterTest(final TestInfo testInfo) {
        String testName = testInfo.getTestMethod()
            .map(Method::getName)
            .orElse("unknown");

        LOG.info("Test {} completed", testName);
    }

    @AfterAll
    static void shutdownLighty() {
        if (lightyController != null) {
            LOG.info("Shutting down Lighty controller");
            lightyController.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    LightyController getLightyController() {
        return lightyController;
    }
}
