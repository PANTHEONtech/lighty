/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.api;

import com.google.common.util.concurrent.SettableFuture;
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

public abstract class AbstractLightyModuleTest {
    private ExecutorService executorService;
    private LightyModule moduleUnderTest;

    protected abstract LightyModule getModuleUnderTest();
    protected abstract long getMaxInitTimeout();
    protected abstract long getMaxShutdownTimeout();

    /**
     * This method is used in testStartBlocking_and_shutdown test to wait until thread in which blocking start was
     * executed is finished after shutdown was called. If you experience timeouts while waiting for startBlocking method
     * thread to finish, you can try to override this method with bigger value.
     * @return timeout in milliseconds.
     */
    protected long getSleepAfterShutdownTimeout() {
        return 100;
    }

    protected ExecutorService getExecutorService() {
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
    public void testStart() throws Exception {
        this.moduleUnderTest = getModuleUnderTest();
        startLightyModuleAndFailIfTimedOut();
        Mockito.verify(executorService, Mockito.times(1)).execute(Mockito.any());
    }

    @Test
    public void testStart_whenAlreadyStarted() throws Exception {
        this.moduleUnderTest = getModuleUnderTest();
        try {
            this.moduleUnderTest.start().get(getMaxInitTimeout(), TimeUnit.MILLISECONDS);
            this.moduleUnderTest.start().get(getMaxInitTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e ) {
            Assert.fail("Init timed out.", e);
        }
        Mockito.verify(executorService, Mockito.times(1)).execute(Mockito.any());
    }


    @Test
    public void testShutdown() throws Exception {
        this.moduleUnderTest = getModuleUnderTest();
        startLightyModuleAndFailIfTimedOut();
        shutDownLightyModuleAndFailIfTimedOut();

        Mockito.verify(executorService, Mockito.times(2)).execute(Mockito.any());
    }

    @Test
    public void testShutdown_whenAlreadyShutDown() throws Exception {
        this.moduleUnderTest = getModuleUnderTest();
        startLightyModuleAndFailIfTimedOut();
        try {
            this.moduleUnderTest.shutdown().get(getMaxShutdownTimeout(), TimeUnit.MILLISECONDS);
            this.moduleUnderTest.shutdown().get(getMaxShutdownTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            Assert.fail("Shutdown timed out.", e);
        }

        Mockito.verify(executorService, Mockito.times(2)).execute(Mockito.any());
    }

    @Test
    public void testShutdown_before_start() throws Exception {
        this.moduleUnderTest = getModuleUnderTest();
        shutDownLightyModuleAndFailIfTimedOut();

        Mockito.verify(executorService, Mockito.times(0)).execute(Mockito.any());
    }

    @Test
    public void testStartBlocking_and_shutdown() throws Exception {
        this.moduleUnderTest = getModuleUnderTest();
        Future<Boolean> startBlockingFuture;
        if (this.moduleUnderTest instanceof AbstractLightyModule) {
            startBlockingFuture = startBlockingOnLightyModuleAbstractClass();
        } else{
            startBlockingFuture = startBlockingOnLightyModuleInterface();
        }
        //test if thread which invokes startBlocking method is still running (it should be)
        Assert.assertFalse(startBlockingFuture.isDone());

        shutDownLightyModuleAndFailIfTimedOut();
        try {
            //test if thread which invokes startBlocking method is done after shutdown was called
            //(after small timeout due to synchronization);
            startBlockingFuture.get(getSleepAfterShutdownTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            Assert.fail("Waiting for finish of startBlocking method thread timed out. you may consider to adjust" +
                    "timeout by overriding getSleepAfterShutdownTimeout() method", e);
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
            initDoneFuture.get(getMaxInitTimeout(), TimeUnit.MILLISECONDS);
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
        Thread.sleep(getMaxInitTimeout());
        return startFuture;
    }

    private void startLightyModuleAndFailIfTimedOut() throws ExecutionException, InterruptedException {
        try {
            this.moduleUnderTest.start().get(getMaxInitTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e ) {
            Assert.fail("Init timed out.", e);
        }
    }

    private void shutDownLightyModuleAndFailIfTimedOut() throws Exception {
        try {
            this.moduleUnderTest.shutdown().get(getMaxShutdownTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            Assert.fail("Shutdown timed out.", e);
        }
    }
}
