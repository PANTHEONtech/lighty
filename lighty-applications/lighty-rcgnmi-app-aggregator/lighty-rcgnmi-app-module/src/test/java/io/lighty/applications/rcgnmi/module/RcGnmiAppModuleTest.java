/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.applications.rcgnmi.module;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.lighty.core.controller.impl.config.ConfigurationException;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class RcGnmiAppModuleTest {
    private static final int MODULE_TIMEOUT = 60;

    private RcGnmiAppModule rcgnmiModule;

    @AfterEach
    public void tearDown() {
        assertTrue(rcgnmiModule.close());
    }

    @Test
    public void gnmiModuleSmokeTest() throws ConfigurationException {
        rcgnmiModule = new RcGnmiAppModule(RcGnmiAppModuleConfigUtils.loadDefaultConfig(),
                Executors.newCachedThreadPool(), MODULE_TIMEOUT, null);
        assertTrue(rcgnmiModule.initModules());
    }

    @Test
    public void gnmiModuleStartFailedTest() throws ConfigurationException {
        final var config = spy(RcGnmiAppModuleConfigUtils.loadDefaultConfig());
        when(config.getControllerConfig()).thenReturn(null);
        rcgnmiModule = new RcGnmiAppModule(config, Executors.newCachedThreadPool(), MODULE_TIMEOUT, null);
        assertFalse(rcgnmiModule.initModules());
    }
}
