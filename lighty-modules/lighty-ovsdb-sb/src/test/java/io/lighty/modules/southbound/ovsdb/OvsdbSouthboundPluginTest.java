/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.ovsdb;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.southbound.ovsdb.config.OvsdbSouthboundConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class OvsdbSouthboundPluginTest {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbSouthboundPluginTest.class);

    private LightyController lightyController;
    private OvsdbSouthboundPlugin ovsdbSouthboundPlugin;

    @BeforeClass
    public void startControllerAndOvsdb() throws Exception {
        LOG.info("Building LightyController");
        final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();

        final ControllerConfiguration configuration =
                ControllerConfigUtils.getDefaultSingleNodeConfiguration(OvsdbSouthboundConfigUtils.YANG_MODELS);

        this.lightyController = lightyControllerBuilder
                .from(configuration)
                .build();

        LOG.info("Starting LightyController (waiting 10s after start)");
        final ListenableFuture<Boolean> started = lightyController.start();
        started.get();
        LOG.info("LightyController started");

        LOG.info("Building ovsdb southbound plugin");
        this.ovsdbSouthboundPlugin = new OvsdbSouthboundPlugin(lightyController.getServices());

        LOG.info("Starting ovsdb southbound plugin (waiting 10s after start)");
        this.ovsdbSouthboundPlugin.start().get();
        LOG.info("Ovsdb southbound plugin started");
    }

    @Test
    public void simpleTest() throws Exception {
        Assert.assertNotNull(lightyController);
        Assert.assertNotNull(ovsdbSouthboundPlugin);
    }

    @AfterClass
    public void shutdownLighty() throws Exception {
        if (ovsdbSouthboundPlugin != null) {
            LOG.info("Shutting ovsdb southbound plugin");
            final ListenableFuture<Boolean> shutdown = ovsdbSouthboundPlugin.shutdown();
            shutdown.get();
        }
        if (lightyController != null) {
            LOG.info("Shutting down LightyController");
            final ListenableFuture<Boolean> shutdown = lightyController.shutdown();
            shutdown.get();
        }
    }
}