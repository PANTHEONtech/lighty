/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;

import io.lighty.applications.rnc.module.config.RncLightyModuleConfigUtils;
import io.lighty.applications.rnc.module.config.RncLightyModuleConfiguration;
import io.lighty.core.controller.impl.config.ConfigurationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.testng.annotations.Test;

public class RncLightyModuleSmokeTest {
    private static final long MODULE_TIMEOUT = 60;
    private static final TimeUnit MODULE_TIME_UNIT = TimeUnit.SECONDS;

    @Test
    public void rncLightyModuleSmokeTest()
            throws ConfigurationException, InterruptedException, ExecutionException, TimeoutException {
        RncLightyModule rncModule = new RncLightyModule(RncLightyModuleConfigUtils.loadDefaultConfig());
        rncModule.start().get(MODULE_TIMEOUT, MODULE_TIME_UNIT);
        rncModule.shutdown().get(MODULE_TIMEOUT, MODULE_TIME_UNIT);
    }

    @Test
    public void rncLightyModuleStartFailed() throws InterruptedException, ExecutionException, TimeoutException,
            ConfigurationException {
        final RncLightyModuleConfiguration config = spy(RncLightyModuleConfigUtils.loadDefaultConfig());
        when(config.getControllerConfig()).thenReturn(null);
        RncLightyModule rncModule = new RncLightyModule(config);
        final Boolean isStarted = rncModule.start().get(MODULE_TIMEOUT, MODULE_TIME_UNIT);

        assertFalse(isStarted);
    }
}
