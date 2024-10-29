/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.api;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.TimeUnit;

/**
 * This is common interface for all Lighty modules.
 * Main Lighty components are modules.
 * There is only one core module {@link LightyController} and other modules depend on this module.
 * Typically, module is north-bound plugin or south-bound plugin,
 * or any logical component of Lighty application using services provided by core module.
 *
 * @author juraj.veverka
 */
public interface LightyModule {
    /**
     * Start in background and return immediately.
     * @return true if module initialization was successful, false or exception otherwise.
     */
    ListenableFuture<Boolean> start();

    /**
     * Shutdown module.
     *
     * @return true if module shutdown was successful, false or exception otherwise.
     * @throws Exception thrown while module shutdown failed
     */
    ListenableFuture<Boolean> shutdown() throws Exception;

    /**
     * Shutdown module and wait for completion for specified amount of time.
     *
     * @param duration duration of time to wait for completion
     * @param unit unit of time duration
     * @return true if module shutdown was successful in specified time, false otherwise.
     */
    boolean shutdown(long duration, TimeUnit unit);
}
