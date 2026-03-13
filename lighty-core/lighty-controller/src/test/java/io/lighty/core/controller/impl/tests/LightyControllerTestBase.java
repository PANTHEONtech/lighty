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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LightyControllerTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(LightyControllerTestBase.class);
    public static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000;

    private static LightyController lightyController;

    // Replaces the TestNG @AfterMethod ITestResult logic
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
    public static void startLighty() throws Exception {
        LOG.info("startLighty from TestBase called");
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        lightyController = lightyControllerBuilder.from(ControllerConfigUtils.getDefaultSingleNodeConfiguration())
            .build();
        ListenableFuture<Boolean> started = lightyController.start();
        started.get();
        LOG.info("startLighty from TestBase finished after sleep");
    }

    @BeforeEach
    public void handleTestMethodName(TestInfo testInfo) {
        String testName = testInfo.getTestMethod().map(Method::getName).orElse(testInfo.getDisplayName());
        LOG.info("Running test {}", testName);
    }

    @AfterAll
    public static void shutdownLighty() {
        if (lightyController != null) {
            LOG.info("Shutting down Lighty controller");
            lightyController.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    LightyController getLightyController() {
        return lightyController;
    }
}
