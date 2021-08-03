/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.applications.rcgnmi.module;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.lighty.core.controller.impl.config.ConfigurationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RcGnmiAppModuleTest {
    private static final long MODULE_TIMEOUT = 60;
    private static final TimeUnit MODULE_TIME_UNIT = TimeUnit.SECONDS;

    @Test
    public void gnmiModuleSmokeTest() throws InterruptedException, ExecutionException, TimeoutException,
            ConfigurationException {
        final RcGnmiAppModule module = new RcGnmiAppModule(RcGnmiAppModuleConfigUtils.loadDefaultConfig(),
                Executors.newCachedThreadPool(), null);
        Assertions.assertTrue(module.start().get(MODULE_TIMEOUT, MODULE_TIME_UNIT));
        Assertions.assertTrue(module.shutdown().get(MODULE_TIMEOUT, MODULE_TIME_UNIT));
    }

    @Test
    public void gnmiModuleStartFailedTest() throws InterruptedException, ExecutionException, TimeoutException,
            ConfigurationException {
        final RcGnmiAppConfiguration config = spy(RcGnmiAppModuleConfigUtils.loadDefaultConfig());
        when(config.getControllerConfig()).thenReturn(null);
        final RcGnmiAppModule rgnmiAppModule = new RcGnmiAppModule(config, Executors.newCachedThreadPool(), null);
        Assertions.assertFalse(rgnmiAppModule.start().get(MODULE_TIMEOUT, MODULE_TIME_UNIT));
    }
}

