package io.lighty.core.controller.impl.services.infrautils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.errorprone.annotations.Var;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinatorMonitor;
import org.opendaylight.infrautils.jobcoordinator.RollbackCallable;
import org.opendaylight.infrautils.metrics.Counter;
import org.opendaylight.infrautils.metrics.Meter;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.infrautils.utils.concurrent.JdkFutures;
import org.opendaylight.infrautils.utils.concurrent.LoggingThreadUncaughtExceptionHandler;
import org.opendaylight.infrautils.utils.concurrent.ThreadFactoryProvider;
import org.opendaylight.openflowplugin.applications.frm.nodeconfigurator.JobQueue;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;

public class LightyJobCoordinator implements JobCoordinator, JobCoordinatorMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(LightyJobCoordinator.class);

    private static final long RETRY_WAIT_BASE_TIME_MILLIS = 1000;

    private static final int FJP_MAX_CAP = 0x7fff; // max #workers - 1; copy/pasted from ForkJoinPool private

    private static final AtomicInteger THREAD_INDEX = new AtomicInteger(0);
    private final ForkJoinPool.ForkJoinWorkerThreadFactory factory = pool -> {
        ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        worker.setName("jobcoordinator-main-task-" + THREAD_INDEX.getAndIncrement());
        return worker;
    };
    private final ForkJoinPool fjPool = new ForkJoinPool(
            Math.min(FJP_MAX_CAP, Runtime.getRuntime().availableProcessors()), factory,
            LoggingThreadUncaughtExceptionHandler.toLogger(LOG), false);

    private final Map<String, LightyJobQueue> jobQueueMap = new ConcurrentHashMap<>();
    private final ReentrantLock jobQueueMapLock = new ReentrantLock();
    private final Condition jobQueueMapCondition = jobQueueMapLock.newCondition();

    private final Meter jobsCreated;
    private final Meter jobsCleared;
    private final Counter jobsPending;
    private final Counter jobsIncomplete;
    private final Meter jobsFailed;
    private final Meter jobsRetriesForFailure;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5,
            ThreadFactoryProvider.builder().namePrefix("jobcoordinator-onfailure-executor").logger(LOG).build().get());

    private final Thread jobQueueHandlerThread;
    private final AtomicBoolean jobQueueHandlerThreadStarted = new AtomicBoolean(false);

    @GuardedBy("jobQueueMapLock")
    private boolean isJobAvailable = false;

    private volatile boolean shutdown = false;

    @Inject
    public LightyJobCoordinator(MetricProvider metricProvider) {
        jobsCreated = metricProvider.newMeter(this, "odl.infrautils.jobcoordinator.jobsCreated");
        jobsCleared = metricProvider.newMeter(this, "odl.infrautils.jobcoordinator.jobsCleared");
        jobsPending = metricProvider.newCounter(this, "odl.infrautils.jobcoordinator.jobsPending");
        jobsIncomplete = metricProvider.newCounter(this, "odl.infrautils.jobcoordinator.jobsIncomplete");
        jobsFailed = metricProvider.newMeter(this, "odl.infrautils.jobcoordinator.jobsFailed");
        jobsRetriesForFailure = metricProvider.newMeter(this, "odl.infrautils.jobcoordinator.jobsRetriesForFailure");

        jobQueueHandlerThread = ThreadFactoryProvider.builder()
                .namePrefix("JobCoordinator-JobQueueHandler")
                .logger(LOG)
                .build().get()
                .newThread(new JobQueueHandler());
    }

    @PreDestroy
    public void destroy() {
        LOG.info("JobCoordinator shutting down... (tasks still running may be stopped/cancelled/interrupted)");

        jobQueueMapLock.lock();
        try {
            shutdown = true;
            jobQueueMapCondition.signalAll();
        } finally {
            jobQueueMapLock.unlock();
        }

        fjPool.shutdownNow();
        scheduledExecutorService.shutdownNow();

        try {
            jobQueueHandlerThread.join(10000);
        } catch (InterruptedException e) {
            // Shouldn't get interrupted - either way we don't care.
        }

        LOG.info("JobCoordinator now closed for business.");
    }

    @Override
    public void enqueueJob(String key, Callable<List<ListenableFuture<Void>>> mainWorker) {
        enqueueJob(key, mainWorker, null, JobCoordinator.DEFAULT_MAX_RETRIES);
    }

    @Override
    public void enqueueJob(String key, Callable<List<ListenableFuture<Void>>> mainWorker,
                           RollbackCallable rollbackWorker) {
        enqueueJob(key, mainWorker, rollbackWorker, JobCoordinator.DEFAULT_MAX_RETRIES);
    }

    @Override
    public void enqueueJob(String key, Callable<List<ListenableFuture<Void>>> mainWorker, int maxRetries) {
        enqueueJob(key, mainWorker, null, maxRetries);
    }

    @Override
    public void enqueueJob(String key, Callable<List<ListenableFuture<Void>>> mainWorker,
                           RollbackCallable rollbackWorker, int maxRetries) {

        jobQueueMapLock.lock();
        try {
            LightyJobQueue jobQueue = jobQueueMap.computeIfAbsent(key, mapKey -> new LightyJobQueue());
            LightyJobEntry jobEntry = new LightyJobEntry(key, jobQueue.getQueueId(), mainWorker, rollbackWorker, maxRetries);
            jobQueue.addEntry(jobEntry);
            LOG.trace("Added a job with key {}, job {} to the queue {}",
                    key, jobEntry.getId(), jobQueue.getQueueId());
        } finally {
            jobQueueMapLock.unlock();
        }

        jobsPending.increment();
        jobsIncomplete.increment();
        jobsCreated.mark();

        signalForNextJob();
    }

    @Override
    public long getClearedTaskCount() {
        return jobsCleared.get();
    }

    @Override
    public long getCreatedTaskCount() {
        return jobsCreated.get();
    }

    @Override
    public long getIncompleteTaskCount() {
        return jobsIncomplete.get();
    }

    @Override
    public long getPendingTaskCount() {
        return jobsPending.get();
    }

    @Override
    public long getFailedJobCount() {
        return jobsFailed.get();
    }

    @Override
    public long getRetriesCount() {
        return jobsRetriesForFailure.get();
    }

    /**
     * Cleanup the submitted job from the job queue.
     **/
    private void clearJob(LightyJobEntry jobEntry) {
        String jobKey = jobEntry.getKey();
        LOG.trace("About to clear job with key {}, job{} from queue {}",
                jobKey, jobEntry.getId(), jobEntry.getQueueId());
        LightyJobQueue jobQueue = jobQueueMap.get(jobKey);
        if (jobQueue != null) {


            jobQueueMapLock.lock();
            try {
                if (jobQueue.isEmpty()) {
                    jobQueueMap.remove(jobKey);
                    LOG.trace("Removed jobQueue {} for jobKey {} while clearing job {}",
                            jobQueue.getQueueId(), jobKey, jobEntry.getId());
                }
            } finally {
                jobQueueMapLock.unlock();
            }
            jobEntry.setEndTime(System.currentTimeMillis());
            jobQueue.onJobFinished(jobEntry.getEndTime() - jobEntry.getStartTime());
            jobQueue.setExecutingEntry(null);

        } else {
            LOG.error("clearJob: jobQueueMap did not contain the key for this entry: {}", jobEntry);
        }
        jobsCleared.mark();
        jobsIncomplete.decrement();
        signalForNextJob();
    }

    private void signalForNextJob() {
        if (jobQueueHandlerThreadStarted.compareAndSet(false, true)) {
            jobQueueHandlerThread.start();
        }

        jobQueueMapLock.lock();
        try {
            isJobAvailable = true;
            jobQueueMapCondition.signalAll();
        } finally {
            jobQueueMapLock.unlock();
        }
    }

    private boolean executeTask(Runnable task) {
        try {
            fjPool.execute(task);
            return true;
        } catch (RejectedExecutionException e) {
            if (!fjPool.isShutdown()) {
                LOG.error("ForkJoinPool task rejected", e);
            }

            return false;
        }
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private Future<?> scheduleTask(Runnable task, long delay, TimeUnit unit) {
        try {
            return scheduledExecutorService.schedule(task, delay, unit);
        } catch (RejectedExecutionException e) {
            if (!scheduledExecutorService.isShutdown()) {
                LOG.error("ScheduledExecutorService rejected task", e);
            }
            return immediateFailedFuture(e);
        }
    }

    @VisibleForTesting
    // Ideally this would be package-scoped but the unit test class is in a separate package.
    protected Thread getJobQueueHandlerThread() {
        return jobQueueHandlerThread;
    }

    /**
     * JobCallback class is used as a future callback for main and rollback
     * workers to handle success and failure.
     */
    private class JobCallback implements FutureCallback<List<Void>> {
        private final LightyJobEntry jobEntry;

        JobCallback(LightyJobEntry jobEntry) {
            this.jobEntry = jobEntry;
        }

        /**
         * This implies that all the future instances have returned success. --
         * TODO: Confirm this
         */
        @Override
        public void onSuccess(List<Void> voids) {
            LOG.trace("Job completed successfully with key {}, job {} from queue {}",
                    jobEntry.getKey(), jobEntry.getId(), jobEntry.getQueueId());
            clearJob(jobEntry);
        }

        /**
         * This method is used to handle failure callbacks. If more retry
         * needed, the retrycount is decremented and mainworker is executed
         * again. After retries completed, rollbackworker is executed. If
         * rollbackworker fails, this is a double-fault. Double fault is logged
         * and ignored.
         */
        @Override
        public void onFailure(Throwable throwable) {
            int retryCount = jobEntry.decrementRetryCountAndGet();
            jobsRetriesForFailure.mark();

            if (retryCount == 0 && jobEntry.getMaxRetries() > 0) {
                LOG.error("Job still failed on final retry: {}", jobEntry, throwable);
            } else if (retryCount == 0 && jobEntry.getMaxRetries() == 0) {
                LOG.error("Job failed, no retries: {}", jobEntry, throwable);
            } else {
                // If retryCount > 0, then the log should not be polluted with confusing WARN messages (because we're
                // about to retry it again, shortly; if it ultimately still fails, there will be a WARN); so DEBUG.
                LOG.debug("Job failed, will retry: {}", jobEntry, throwable);
            }
            if (jobEntry.getMainWorker() == null) {
                LOG.error("Job failed with Double-Fault. Bailing Out: {}", jobEntry);
                clearJob(jobEntry);
                return;
            }

            if (retryCount > 0) {
                long waitTime = RETRY_WAIT_BASE_TIME_MILLIS / retryCount;
                Futures.addCallback(JdkFutures.toListenableFuture(scheduleTask(() -> {
                    MainTask worker = new MainTask(jobEntry);
                    executeTask(worker);
                }, waitTime, TimeUnit.MILLISECONDS)), new FutureCallback<Object>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.error("Retry of job failed; rolling back or clearing job: {}", jobEntry, throwable);
                        rollbackOrClear(jobEntry);
                    }

                    @Override
                    public void onSuccess(Object result) {
                        LOG.debug("Retry of job submission succeeded with key {}, job {} from queue {}",
                                jobEntry.getKey(), jobEntry.getId(), jobEntry.getQueueId());
                    }
                }, MoreExecutors.directExecutor());
            } else {
                rollbackOrClear(jobEntry);
            }
        }
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private void rollbackOrClear(LightyJobEntry jobEntry) {
        jobsFailed.mark();
        if (jobEntry.getRollbackWorker() != null) {
            jobEntry.setMainWorker(null);
            RollbackTask rollbackTask = new RollbackTask(jobEntry);
            executeTask(rollbackTask);
            return;
        }
        clearJob(jobEntry);
    }

    /**
     * RollbackTask is used to execute the RollbackCallable provided by the
     * application in the eventuality of a failure.
     */
    private class RollbackTask implements Runnable {
        private final LightyJobEntry jobEntry;

        RollbackTask(LightyJobEntry jobEntry) {
            this.jobEntry = jobEntry;
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        public void run() {
            RollbackCallable rollbackWorker = jobEntry.getRollbackWorker();
            @Var List<ListenableFuture<Void>> futures = null;
            if (rollbackWorker != null) {
                try {
                    futures = rollbackWorker.apply(jobEntry.getFutures());
                } catch (Throwable e) {
                    LOG.error("Exception when executing job's rollbackWorker: {}", jobEntry, e);
                }
            } else {
                LOG.error("Unexpected no (null) rollback worker on job: {}", jobEntry);
            }

            if (futures == null || futures.isEmpty()) {
                LOG.trace("From RollbackTask: futures is null or empty. Clearing the jobQueue with key {},"
                        + " job {} from queue {}", jobEntry.getKey(), jobEntry.getId(), jobEntry.getQueueId());
                clearJob(jobEntry);
                return;
            }

            jobEntry.setFutures(futures);
            ListenableFuture<List<Void>> listenableFuture = Futures.allAsList(futures);
            Futures.addCallback(listenableFuture, new JobCallback(jobEntry), MoreExecutors.directExecutor());
        }
    }

    /**
     * Execute the MainWorker callable.
     */
    private class MainTask implements Runnable {
        private static final int LONG_JOBS_THRESHOLD_MS = 1000;
        private final LightyJobEntry jobEntry;

        MainTask(LightyJobEntry jobEntry) {
            this.jobEntry = jobEntry;
        }

        @Override
        @SuppressWarnings("checkstyle:illegalcatch")
        public void run() {
            @Var List<ListenableFuture<Void>> futures = null;
            long jobStartTimestampNanos = System.nanoTime();
            jobEntry.setStartTime(System.currentTimeMillis());
            LOG.trace("Running job with key {}, job {} from queue {}",
                    jobEntry.getKey(), jobEntry.getId(), jobEntry.getQueueId());

            try {
                Callable<List<ListenableFuture<Void>>> mainWorker = jobEntry.getMainWorker();
                if (mainWorker != null) {
                    futures = mainWorker.call();
                } else {
                    LOG.error("Unexpected no (null) main worker on job: {}", jobEntry);
                }
                long jobExecutionTimeNanos = System.nanoTime() - jobStartTimestampNanos;
                printJobs(jobEntry.getKey(), TimeUnit.NANOSECONDS.toMillis(jobExecutionTimeNanos));
            } catch (Throwable e) {
                jobsFailed.mark();
                LOG.error("Direct Exception (not failed Future) when executing job, won't even retry: {}", jobEntry, e);
            }

            if (futures == null || futures.isEmpty()) {
                LOG.trace("From MainTask: futures is null or empty. Clearing the jobQueue with key {},"
                        + " job {} from queue {}", jobEntry.getKey(), jobEntry.getId(), jobEntry.getQueueId());
                clearJob(jobEntry);
                return;
            }

            List<ListenableFuture<Void>> nonNullFutures = futures.stream().filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (nonNullFutures.isEmpty()) {
                LOG.trace("From MainTask nonNullFutures: Clearing the jobQueue with key {}, job {} from queue {}",
                        jobEntry.getKey(), jobEntry.getId(), jobEntry.getQueueId());
                clearJob(jobEntry);
                return;
            }

            jobEntry.setFutures(futures);
            ListenableFuture<List<Void>> listenableFuture = Futures.allAsList(futures);
            Futures.addCallback(listenableFuture, new JobCallback(jobEntry), MoreExecutors.directExecutor());
        }

        private void printJobs(String key, long jobExecutionTime) {
            if (jobExecutionTime > LONG_JOBS_THRESHOLD_MS) {
                LOG.warn("Job with key {}, job {} from queue {} took {}ms to complete",
                        jobEntry.getKey(), jobEntry.getId(), jobEntry.getQueueId(), jobExecutionTime);
                return;
            }
            LOG.trace("Job with key {}, job {} from queue {} took {}ms to complete",
                    jobEntry.getKey(), jobEntry.getId(), jobEntry.getQueueId(), jobExecutionTime);
        }
    }

    private class JobQueueHandler implements Runnable {
        @Override
        @SuppressWarnings("checkstyle:illegalcatch")
        public void run() {
            LOG.info("Starting JobQueue Handler Thread");
            while (true) {
                try {
                    for (Map.Entry<String, LightyJobQueue> entry : jobQueueMap.entrySet()) {
                        if (shutdown) {
                            break;
                        }

                        LightyJobQueue jobQueue = entry.getValue();
                        if (jobQueue.getExecutingEntry() != null) {
                            continue;
                        }
                        LightyJobEntry jobEntry = jobQueue.poll();
                        if (jobEntry == null) {
                            // job queue is empty. so continue with next job queue entry
                            continue;
                        }
                        jobQueue.setExecutingEntry(jobEntry);
                        MainTask worker = new MainTask(jobEntry);
                        LOG.trace("Executing job with key {}, job {} from queue {}",
                                jobEntry.getKey(), jobEntry.getId(), jobEntry.getQueueId());

                        if (executeTask(worker)) {
                            jobsPending.decrement();
                        }
                    }

                    if (!waitForJobIfNeeded()) {
                        break;
                    }
                } catch (Throwable e) {
                    LOG.error("Exception while executing the tasks", e);
                }
            }
        }

        // Suppress "Exceptional return value of java.util.concurrent.locks.Condition.await" - we really don't care
        // if the Condition was signaled or timed out as we use isJobAvailable to break or continue waiting.
        @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
        private boolean waitForJobIfNeeded() throws InterruptedException {
            jobQueueMapLock.lock();
            try {
                while (!isJobAvailable && !shutdown) {
                    jobQueueMapCondition.await(1, TimeUnit.SECONDS);
                }
                isJobAvailable = false;
                return !shutdown;
            } finally {
                jobQueueMapLock.unlock();
            }
        }
    }


    public Map<String, LightyJobQueue> getJobQueueMap() {
        return jobQueueMap;
    }

    @Override
    public String toString() {
        boolean isJobAvailableFromLock;
        jobQueueMapLock.lock();
        try {
            isJobAvailableFromLock = isJobAvailable;
        } finally {
            jobQueueMapLock.unlock();
        }

        return MoreObjects.toStringHelper(this).add("incompleteTasks", getIncompleteTaskCount())
                .add("pendingTasks", getPendingTaskCount()).add("failedJobs", getFailedJobCount())
                .add("clearedTasks", getClearedTaskCount()).add("createdTasks", getCreatedTaskCount())
                .add("retriesCount", getRetriesCount())
                .add("fjPool", fjPool).add("jobQueueMap", jobQueueMap).add("jobQueueMapLock", jobQueueMapLock)
                .add("scheduledExecutorService", scheduledExecutorService).add("isJobAvailable", isJobAvailableFromLock)
                .add("jobQueueMapCondition", jobQueueMapCondition)
                .toString();
    }
}
