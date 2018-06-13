package io.lighty.core.controller.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.opendaylight.infrautils.ready.SystemReadyListener;
import org.opendaylight.infrautils.ready.SystemReadyMonitor;
import org.opendaylight.infrautils.ready.SystemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LightySystemReadyMonitorImpl implements SystemReadyMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(LightySystemReadyMonitorImpl.class);

    private Set<SystemReadyListener> listeners;
    private SystemState state = SystemState.BOOTING;
    private boolean running = false;

    private static final LightySystemReadyMonitorImpl INSTANCE = new LightySystemReadyMonitorImpl();
    private static int counter = 60;
    private static ScheduledFuture<?> scheduledFuture;
    private LightySystemReadyMonitorImpl() {
        this.listeners = new HashSet<>();
    }

    public static LightySystemReadyMonitorImpl getInstance() {
        return INSTANCE;
    }

    public void init() {
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        scheduledFuture = executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (state == SystemState.ACTIVE) {
                    LOG.info("Lighty system is ready");
                    for (SystemReadyListener listener : listeners) {
                        listener.onSystemBootReady();
                   }
                   running = false;
                   scheduledFuture.cancel(false);
                } else {

                    if (counter-- == 0) {
                        LOG.error("Lighty system failed to start");
                        throw new IllegalStateException("Lighty controller did not start after 60 seconds");
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void registerListener(SystemReadyListener listener) {
        listeners.add(listener);
    }

    @Override
    public SystemState getSystemState() {
        return state;
    }

    public void setState(final SystemState state) {
        this.state = state;
    }
}
