/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.api;

import com.google.common.util.concurrent.SettableFuture;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LightyModuleTest {
    private static final long MAX_INIT_TIMEOUT = 15000L;
    private static final long MAX_SHUTDOWN_TIMEOUT = 15000L;
    private static final long SLEEP_AFTER_SHUTDOWN_TIMEOUT = 800L;

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
    public void initExecutor() {
        this.executorService = Mockito.spy(new ScheduledThreadPoolExecutor(1));
    }

    @AfterMethod
    public void shutdownExecutor() {
        this.executorService.shutdownNow();
    }

    @Test
    public void testStartShutdown() throws Exception {
        this.moduleUnderTest = getModuleUnderTest(getExecutorService());
        this.moduleUnderTest.start().get(MAX_INIT_TIMEOUT, TimeUnit.MILLISECONDS);
        Mockito.verify(executorService, Mockito.times(1)).execute(Mockito.any());
        this.moduleUnderTest.shutdown(MAX_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        Mockito.verify(executorService, Mockito.times(2)).execute(Mockito.any());
    }

    @Test
    public void testStartStop_whenAlreadyStartedStopped() throws Exception {
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
    public void testShutdown_before_start() throws Exception {
        this.moduleUnderTest = getModuleUnderTest(getExecutorService());
        this.moduleUnderTest.shutdown(MAX_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        Mockito.verify(executorService, Mockito.times(0)).execute(Mockito.any());
    }

    @Test
    public void testStartBlocking_and_shutdown() throws Exception {
        this.moduleUnderTest = getModuleUnderTest(getExecutorService());
        startStopBlocking(this.moduleUnderTest instanceof AbstractLightyModule);
    }

    @Test
    public void testStartStopBlocking() throws Exception {
        this.moduleUnderTest = getModuleUnderTest(getExecutorService());
        startStopBlocking(false);
    }

    private void startStopBlocking(final boolean isAbstract) throws Exception {
        Future<Boolean> startBlockingFuture;
        if (isAbstract) {
            startBlockingFuture = startBlockingOnLightyModuleAbstractClass();
        } else {
            startBlockingFuture = startBlockingOnLightyModuleInterface();
        }
        //test if thread which invokes startBlocking method is still running (it should be)
        Assert.assertFalse(startBlockingFuture.isDone());

        this.moduleUnderTest.shutdown(MAX_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        try {
            //test if thread which invokes startBlocking method is done after shutdown was called
            //(after small timeout due to synchronization);
            startBlockingFuture.get(SLEEP_AFTER_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            Assert.fail("Waiting for finish of startBlocking method thread timed out. you may consider to adjust"
                    + "timeout by overriding SLEEP_AFTER_SHUTDOWN_TIMEOUT", e);
        }

        Mockito.verify(executorService, Mockito.times(2)).execute(Mockito.any());
    }

    private Future<Boolean> startBlockingOnLightyModuleAbstractClass() throws ExecutionException, InterruptedException {
        SettableFuture<Boolean> initDoneFuture = SettableFuture.create();
        Future<Boolean> startFuture = Executors.newSingleThreadExecutor().submit(() -> {
            ((AbstractLightyModule)this.moduleUnderTest).startBlocking(initDoneFuture::set);
            return true;
        });
        try {
            initDoneFuture.get(MAX_INIT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            Assert.fail("Init timed out.", e);
        }
        return startFuture;
    }

    private Future<Boolean> startBlockingOnLightyModuleInterface() throws InterruptedException {
        Future<Boolean> startFuture = Executors.newSingleThreadExecutor().submit(() -> {
            this.moduleUnderTest.startBlocking();
            return true;
        });
        Thread.sleep(MAX_INIT_TIMEOUT);
        return startFuture;
    }
}
