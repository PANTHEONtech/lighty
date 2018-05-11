/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.api;

import com.google.common.util.concurrent.ListenableFuture;

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
     * @return
     *   true if module initialization was successful, false or exception otherwise.
     */
    ListenableFuture<Boolean> start();

    /**
     * Start and block until shutdown is requested.
     * @throws InterruptedException
     *   thrown in case module initialization fails.
     */
    void startBlocking() throws InterruptedException;

    /**
     * shutdown module
     * @return
     *   true if module shutdown was successful, false or exception otherwise.
     * @throws Exception - thrown while module shutdown failed
     */
    ListenableFuture<Boolean> shutdown() throws Exception;

}
