/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.services;

import io.lighty.core.controller.api.LightyController;
import org.opendaylight.infrautils.ready.SystemReadyListener;
import org.opendaylight.infrautils.ready.SystemReadyMonitor;
import org.opendaylight.infrautils.ready.SystemState;

/**
 * This service provides API to control state of {@link SystemReadyMonitor} service provided by
 * {@link LightyController}. Methods of this service should be called by lighty application after
 * {@link LightyController} has been started and after plugins are in ready state.
 */
// TODO: upstream implementation of {@link SystemReadyMonitor} needs serious refactoring.
public interface LightySystemReadyService {

    /**
     * Called when lighty application star sequence is completed. This sets internal state of {@link SystemReadyMonitor}
     * to {@link SystemState#ACTIVE}, all registered listeners will be notified by calling
     * {@link SystemReadyListener#onSystemBootReady()} method.
     * @return number of registered listeners notified about system ready state change.
     */
    int onSystemBootReady();

    /**
     * Called when lighty application star sequence is fails. This sets internal state of {@link SystemReadyMonitor}
     * to {@link SystemState#FAILURE}, no listeners are notified.
     * @return number of registered listeners in {@link SystemReadyMonitor}.
     */
    int onSystemBootFailed();
}
