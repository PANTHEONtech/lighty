/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.services;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.infrautils.ready.SystemReadyListener;
import org.opendaylight.infrautils.ready.SystemReadyMonitor;
import org.opendaylight.infrautils.ready.SystemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightySystemReadyMonitorImpl implements LightySystemReadyService, SystemReadyMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(LightySystemReadyMonitorImpl.class);

    private final List<SystemReadyListener> listeners = new ArrayList<>();

    private SystemState state = SystemState.BOOTING;
    private Exception failureCause;

    public LightySystemReadyMonitorImpl() {
        LOG.info("SystemReadyMonitorImpl: {}", state);
    }

    @Override
    public synchronized void registerListener(final SystemReadyListener listener) {
        LOG.info("registerListener: {}", state);
        switch (state) {
            case BOOTING:
                listeners.add(listener);
                break;
            case ACTIVE:
                notifyListener(listener);
                break;
            case FAILURE:
                LOG.warn("ignoring listener, system is in {} state", state);
                break;
            default:
                throw new UnsupportedOperationException("State " + state.name() + " is not supported !");
        }
    }


    @Override
    public synchronized SystemState getSystemState() {
        LOG.info("getSystemState: {}", state);
        return state;
    }

    @Override
    public synchronized int onSystemBootReady() {
        state = SystemState.ACTIVE;
        LOG.info("onSystemBootReady {} {}", state, listeners.size());

        for (SystemReadyListener listener : listeners) {
            if (state != SystemState.ACTIVE) {
                break;
            }
            notifyListener(listener);
        }

        if (failureCause != null) {
            throw new IllegalStateException("Services failed to completely start", failureCause);
        }

        int size = listeners.size();
        listeners.clear();
        return size;
    }

    @Override
    public synchronized int onSystemBootFailed() {
        state = SystemState.FAILURE;
        LOG.warn("onSystemBootFailed {} {}", state, listeners.size());
        failureCause = new Exception("Unknown reason");
        return listeners.size();
    }

    @Override
    public synchronized String getFailureCause() {
        return failureCause == null ? "" : failureCause.getMessage();
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private void notifyListener(final SystemReadyListener listener) {
        try {
            listener.onSystemBootReady();
        } catch (Exception e) {
            LOG.error("Listener {} failed", listener, e);
            if (failureCause == null) {
                failureCause = e;
            } else {
                failureCause.addSuppressed(e);
            }
        }
    }
}
