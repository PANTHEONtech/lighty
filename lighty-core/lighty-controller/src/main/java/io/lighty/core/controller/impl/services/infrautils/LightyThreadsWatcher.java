package io.lighty.core.controller.impl.services.infrautils;

import static java.lang.management.ManagementFactory.getThreadMXBean;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.opendaylight.infrautils.utils.concurrent.Executors.newListeningSingleThreadScheduledExecutor;
import static org.opendaylight.infrautils.utils.concurrent.LoggingFutures.addErrorLogging;

import com.codahale.metrics.jvm.ThreadDeadlockDetector;
import com.codahale.metrics.jvm.ThreadDump;
import com.google.common.annotations.VisibleForTesting;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Automatic JVM thread limit and deadlock detection logging.
 *
 * @author Michael Vorburger.ch
 */
class LightyThreadsWatcher implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(LightyThreadsWatcher.class);

    private final int maxThreads;
    private final ScheduledExecutorService scheduledExecutor;
    private final ThreadDeadlockDetector threadDeadlockDetector = new ThreadDeadlockDetector();
    private final ThreadDump threadDump = new ThreadDump(getThreadMXBean());

    private final Duration interval;
    private final Duration maxDeadlockLog;
    private final Duration maxMaxThreadsLog;

    private volatile Instant lastDeadlockLog;
    private volatile Instant lastMaxThreadsLog;

    LightyThreadsWatcher(int maxThreads, Duration interval,
                   Duration maxThreadsMaxLogInterval, Duration deadlockedThreadsMaxLogInterval) {
        this.maxThreads = maxThreads;
        this.interval = interval;
        this.maxDeadlockLog = deadlockedThreadsMaxLogInterval;
        this.maxMaxThreadsLog = maxThreadsMaxLogInterval;
        this.scheduledExecutor = newListeningSingleThreadScheduledExecutor("infrautils.metrics.ThreadsWatcher", LOG);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    void start() {
        addErrorLogging(scheduledExecutor.scheduleAtFixedRate(this, 0, interval.toNanos(), NANOSECONDS), LOG,
                "scheduleAtFixedRate");
    }

    void close() {
        scheduledExecutor.shutdown();
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public Duration getInterval() {
        return interval;
    }

    public Duration getMaxThreadsMaxLogInterval() {
        return maxMaxThreadsLog;
    }

    public Duration getDeadlockedThreadsMaxLogInterval() {
        return maxDeadlockLog;
    }

    @Override
    public void run() {
        int currentNumberOfThreads = getThreadMXBean().getThreadCount();
        Set<String> deadlockedThreadsStackTrace = threadDeadlockDetector.getDeadlockedThreads();
        if (!deadlockedThreadsStackTrace.isEmpty()) {
            LOG.error("Oh nose - there are {} deadlocked threads!! :-(", deadlockedThreadsStackTrace.size());
            for (String deadlockedThreadStackTrace : deadlockedThreadsStackTrace) {
                LOG.error("Deadlocked thread stack trace: {}", deadlockedThreadStackTrace);
            }
            if (isConsidered(lastDeadlockLog, Instant.now(), maxDeadlockLog)) {
                logAllThreads();
                lastDeadlockLog = Instant.now();
            }

        } else if (currentNumberOfThreads >= maxThreads) {
            LOG.warn("Oh nose - there are now {} threads, more than maximum threshold {}! "
                            + "(totalStarted: {}, peak: {}, daemons: {})",
                    currentNumberOfThreads, maxThreads, getThreadMXBean().getTotalStartedThreadCount(),
                    getThreadMXBean().getPeakThreadCount(), getThreadMXBean().getDaemonThreadCount());
            if (isConsidered(lastMaxThreadsLog, Instant.now(), maxMaxThreadsLog)) {
                logAllThreads();
                lastMaxThreadsLog = Instant.now();
            }
        }
    }

    @VisibleForTesting
    boolean isConsidered(Instant lastOccurence, Instant now, Duration maxFrequency) {
        return lastOccurence == null || Duration.between(lastOccurence, now).compareTo(maxFrequency) >= 0;
    }

    @VisibleForTesting
    void logAllThreads() {
        try (OutputStream loggingOutputStream = new LoggingOutputStream()) {
            threadDump.dump(loggingOutputStream);
        } catch (IOException e) {
            LOG.error("LoggingOutputStream.close() failed", e);
        }
    }

    private static class LoggingOutputStream extends ByteArrayOutputStream {

        @Override
        public void close() throws IOException {
            String lines = this.toString("UTF-8"); // UTF-8 because that is what ThreadDump writes it in
            LOG.warn("Thread Dump:\n{}", lines);
        }
    }

}
