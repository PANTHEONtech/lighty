/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.bgp.deployer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.bgp.config.BgpConfigUtils;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(TestBase.class);
    protected static final long WAIT_TIME = 20_000;

    protected static BgpModule bgpModule;
    protected static LightyServices lightyServices;
    protected static LightyController controller;

    @BeforeAll
    static void setup() throws ConfigurationException, InterruptedException, ExecutionException, TimeoutException {
        ControllerConfiguration controllerConfiguration
                = ControllerConfigUtils.getDefaultSingleNodeConfiguration(BgpConfigUtils.ALL_BGP_MODELS);
        controller = new LightyControllerBuilder().from(controllerConfiguration).build();
        assertTrue(controller.start().get(WAIT_TIME, TimeUnit.MILLISECONDS));
        lightyServices = controller.getServices();
        bgpModule = new BgpModule(lightyServices);
        assertTrue(bgpModule.start().get(WAIT_TIME, TimeUnit.MILLISECONDS));
    }

    @AfterAll
    static void shutdown() {
        final boolean bgpShutdown = shutdownModule(bgpModule);
        final boolean controllerShutdown = shutdownModule(controller);
        assertTrue(bgpShutdown);
        assertTrue(controllerShutdown);
    }

    @SuppressWarnings({"checkstyle:illegalCatch"})
    private static boolean shutdownModule(final LightyModule module) {
        try {
            return module.shutdown().get(WAIT_TIME, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOG.error("Shutdown of {} module failed", module.getClass().getName(), e);
            return false;
        }
    }

}
