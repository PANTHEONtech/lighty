/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.api;

import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

class LightyModuleTest {
    private static final long MAX_INIT_TIMEOUT = 15000L;
    private static final long MAX_SHUTDOWN_TIMEOUT = 15000L;

    private ExecutorService executorService;
    private LightyModule moduleUnderTest;

    private LightyModule getModuleUnderTest(final ExecutorService service) throws Exception {
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        return lightyControllerBuilder
                .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration())
                .withExecutorService(service)
                .build();
    }

    private ExecutorService getExecutorService() {
        return executorService;
    }

    @BeforeMethod
    void initExecutor() {
        this.executorService = Mockito.spy(new ScheduledThreadPoolExecutor(1));
    }

    @AfterMethod
    void shutdownExecutor() {
        this.executorService.shutdownNow();
    }

    @Test
    void testStartShutdown() throws Exception {
        this.moduleUnderTest = getModuleUnderTest(getExecutorService());
        this.moduleUnderTest.start().get(MAX_INIT_TIMEOUT, TimeUnit.MILLISECONDS);
        Mockito.verify(executorService, Mockito.times(1)).execute(Mockito.any());
        this.moduleUnderTest.shutdown(MAX_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        Mockito.verify(executorService, Mockito.times(2)).execute(Mockito.any());
    }

    @Test
    void testStartStop_whenAlreadyStartedStopped() throws Exception {
        this.moduleUnderTest = getModuleUnderTest(getExecutorService());
        try {
            this.moduleUnderTest.start().get(MAX_INIT_TIMEOUT, TimeUnit.MILLISECONDS);
            this.moduleUnderTest.start().get(MAX_INIT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            Assert.fail("Init timed out.", e);
        }
        Mockito.verify(executorService, Mockito.times(1)).execute(Mockito.any());
        this.moduleUnderTest.shutdown(MAX_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        Mockito.verify(executorService, Mockito.times(2)).execute(Mockito.any());
        this.moduleUnderTest.shutdown(MAX_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        Mockito.verify(executorService, Mockito.times(2)).execute(Mockito.any());
    }

    @Test
    void testShutdown_before_start() throws Exception {
        this.moduleUnderTest = getModuleUnderTest(getExecutorService());
        this.moduleUnderTest.shutdown(MAX_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        Mockito.verify(executorService, Mockito.times(0)).execute(Mockito.any());
    }
}
