/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.services;

import org.opendaylight.infrautils.ready.SystemReadyListener;
import org.opendaylight.infrautils.ready.SystemReadyMonitor;
import org.opendaylight.infrautils.ready.SystemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LightySystemReadyMonitorImpl implements LightySystemReadyService, SystemReadyMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(LightySystemReadyMonitorImpl.class);

    private final List<SystemReadyListener> listeners;
    private SystemState state;

    public LightySystemReadyMonitorImpl() {
        this.listeners = new ArrayList<>();
        this.state = SystemState.BOOTING;
        LOG.info("SystemReadyMonitorImpl: {}", state);
    }

    @Override
    public synchronized void registerListener(SystemReadyListener listener) {
        LOG.info("registerListener: {}", state);
        switch (state) {
            case BOOTING:
                listeners.add(listener);
                break;
            case ACTIVE:
                listener.onSystemBootReady();
                break;
            case FAILURE:
                LOG.warn("ignoring listener, system is in {} state", state);
                break;
            default:
                throw new UnsupportedOperationException("State " + state.name() + " is not supported !");
        }
    }

    @Override
    public SystemState getSystemState() {
        LOG.info("getSystemState: {}", state);
        return state;
    }

    @Override
    public synchronized int onSystemBootReady() {
        state = SystemState.ACTIVE;
        LOG.info("onSystemBootReady {} {}", state, listeners.size());
        listeners.forEach(l->{
            l.onSystemBootReady();
        });
        int size = listeners.size();
        listeners.clear();
        return size;
    }

    @Override
    public synchronized int onSystemBootFailed() {
        state = SystemState.FAILURE;
        LOG.warn("onSystemBootFailed {} {}", state, listeners.size());
        return listeners.size();
    }

    @Override
    public String getFailureCause() {
        return "";
    }
}
