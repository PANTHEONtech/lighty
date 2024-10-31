/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.api;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class implement {@link LightyModule} interface with
 * synchronization of {@link LightyModule#start()},
 * {@link LightyModule#startBlocking()} and {@link LightyModule#shutdown()}
 * methods. Users who don't want to implement their own synchronization
 * can extend this class and provide just
 * {@link AbstractLightyModule#initProcedure()} and
 * {@link AbstractLightyModule#stopProcedure()} methods.These methods
 * will be then automatically called in
 * {@link AbstractLightyModule#start()},
 * {@link AbstractLightyModule#startBlocking()} and
 * {@link AbstractLightyModule#shutdown()} methods.
 *
 * <p>
 * <b>Example usage:</b>
 * <pre>
 * <code>
 *     public class MyLightyModule extends AbstractLightyModule {
 *         private SomeBean someBean;
 *         ...
 *        {@literal @}Override
 *         protected boolean initProcedure() {
 *             this.someBean = new SomeBean();
 *             boolean success = this.someBean.init();
 *             return success;
 *         }
 *
 *        {@literal @}Override
 *         protected boolean stopProcedure() {
 *             boolean stopSuccess = this.someBean.stop();
 *             this.someBean = null;
 *             return stopSuccess;
 *         }
 *     }
 * </code>
 * </pre>
 *
 * @author andrej.zan
 */
public abstract class AbstractLightyModule implements LightyModule {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLightyModule.class);
    private final CountDownLatch shutdownLatch;
    private ListeningExecutorService executorService;
    private boolean executorIsProvided;
    private volatile boolean running;

    public AbstractLightyModule(final ExecutorService executorService) {
        if (executorService == null) {
            this.executorIsProvided = false;
            LOG.debug("ExecutorService for LightyModule {} was not provided. By default single thread ExecutorService"
                    + " will be used.", this.getClass().getSimpleName());
        } else {
            this.executorService = MoreExecutors.listeningDecorator(executorService);
            this.executorIsProvided = true;
        }
        this.shutdownLatch = new CountDownLatch(1);
        this.running = false;
    }

    public AbstractLightyModule() {
        this(null);
    }

    /**
     * This method is called in {@link AbstractLightyModule#start()} method.
     * Implementation of this method should initialize everything necessary.
     * @return success of initialization
     * @throws InterruptedException if initialization was interrupted.
     */
    protected abstract boolean initProcedure() throws InterruptedException;

    /**
     * This method is called in {@link AbstractLightyModule#shutdown()} method.
     * Implementation of this method should do everything necessary to
     * shutdown correctly (e.g. stop initialized beans, release resources, ...).
     * @return success of stop.
     * @throws InterruptedException if stopping was interrupted.
     */
    protected abstract boolean stopProcedure() throws InterruptedException, ExecutionException;

    @Override
    public synchronized ListenableFuture<Boolean> start() {
        if (this.running) {
            LOG.warn("LightyModule {} is already started.", this.getClass().getSimpleName());
            return Futures.immediateFuture(true);
        }

        if (this.executorService == null) {
            LOG.debug("Creating default single thread ExecutorService for LightyModule {}.",
                    this.getClass().getSimpleName());
            this.executorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        }

        LOG.info("Submitted start of LightyModule {}.", this.getClass().getSimpleName());
        return this.executorService.submit(() -> {
            synchronized (this) {
                LOG.debug("Starting initialization of LightyModule {}", this.getClass().getSimpleName());
                this.running = true;
                boolean initResult = initProcedure();
                LOG.info("LightyModule {} started.", this.getClass().getSimpleName());
                return initResult;
            }
        });
    }

    @Override
    public void startBlocking() throws InterruptedException {
        LOG.info("Start LightyModule {} and block until it will shutdown.", this.getClass().getSimpleName());
        start();
        LOG.debug("Waiting for LightyModule {} shutdown.", this.getClass().getSimpleName());
        this.shutdownLatch.await();
        LOG.info("LightyModule {} shutdown complete. Stop blocking.", this.getClass().getSimpleName());
    }

    /**
     * Start and block until shutdown is requested.
     * @param initFinishCallback callback that will be called after start is completed.
     * @throws InterruptedException thrown in case module initialization fails.
     * @deprecated Use @{@code start.get()} instead in case you want blocking start.
     */
    @Deprecated(forRemoval = true)
    public void startBlocking(final Consumer<Boolean> initFinishCallback) throws InterruptedException {
        Futures.addCallback(start(), new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {
                initFinishCallback.accept(true);
            }

            @Override
            public void onFailure(final Throwable cause) {
                initFinishCallback.accept(false);
            }
        }, MoreExecutors.directExecutor());
        this.shutdownLatch.await();
    }

    @Override
    public synchronized ListenableFuture<Boolean> shutdown() {
        if (!this.running) {
            LOG.warn("LightyModule {} is already shut down.", this.getClass().getSimpleName());
            return Futures.immediateFuture(true);
        }
        LOG.info("Submitted shutdown of LightyModule {}.", this.getClass().getSimpleName());
        ListenableFuture<Boolean> shutdownFuture = this.executorService.submit(() -> {
            synchronized (AbstractLightyModule.this) {
                LOG.debug("Starting shutdown procedure of LightyModule {}.", this.getClass().getSimpleName());
                final boolean stopResult = stopProcedure();
                this.running = false;
                LOG.info("LightyModule {} shutdown complete.", this.getClass().getSimpleName());
                return stopResult;
            }
        });

        if (!this.executorIsProvided) {
            return Futures.transform(shutdownFuture, (result) -> {
                synchronized (AbstractLightyModule.this) {
                    LOG.debug("Shutdown default ExecutorService of LightyModule {}.", this.getClass().getSimpleName());
                    this.executorService.shutdown();
                    this.executorService = null;
                    return true;
                }
            }, MoreExecutors.directExecutor());
        }

        return shutdownFuture;
    }

    @SuppressWarnings("IllegalCatch")
    @Override
    public final boolean shutdown(final long duration, final TimeUnit unit) {
        try {
            final var stopSuccess = shutdown().get(duration, unit);
            if (stopSuccess) {
                LOG.info("LightyModule {} stopped successfully!", getClass().getSimpleName());
            } else {
                LOG.error("Unable to stop LightyModule {}!", getClass().getSimpleName());
            }
            return stopSuccess;
        } catch (final InterruptedException e) {
            LOG.error("Interrupted while shutting down {}:", getClass().getSimpleName(), e);
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            LOG.error("Exception while shutting down {}:", getClass().getSimpleName(), e);
        }
        return false;
    }

    /**
     * Invoke blocking shutdown after blocking start.
     *
     * <p>
     * Release CountDownLatch locking this thread and shutdown.
     * @param duration duration to wait for shutdown to complete
     * @param unit {@link TimeUnit} of {@code duration}
     * @return {@code boolean} indicating shutdown sucess
     * @deprecated Use {@code shutdown()} or {@code shutdown(duration, unit)} instead in case you want
     *     blocking shutdown.
     */
    @Deprecated(forRemoval = true)
    public final boolean shutdownBlocking(final long duration, final TimeUnit unit) {
        shutdownLatch.countDown();
        return shutdown(duration, unit);
    }
}
