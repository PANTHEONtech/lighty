/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.examples.controllers.bgpapp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.core.controller.impl.config.ConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MainTest {

    private static Main app;

    @Test
    void testStartNoConfig() throws Exception {
        app = new Main();
        app.start(new String[]{});
        assertTrue(app.isRunning());
    }

    @Test
    void testStartConfigMissingModels() throws InterruptedException, ExecutionException, TimeoutException,
            ConfigurationException, IOException, URISyntaxException {
        app = new Main();
        app.start(new String[] {"-c", absolutePathOfResource("/missingSomeModelsConfig.json")});
        assertTrue(app.isRunning());
    }

    @Test
    void testStartConfigNoModels() throws InterruptedException, ExecutionException, TimeoutException,
            ConfigurationException, IOException, URISyntaxException {
        app = new Main();
        app.start(new String[] {"-c", absolutePathOfResource("/missingSchemaServiceConfig.json")});
        assertTrue(app.isRunning());
    }

    @AfterEach
    void stop() {
        app.stop();
        assertFalse(app.isRunning());
    }

    private static String absolutePathOfResource(final String resourcePath) throws URISyntaxException {
        return new File(MainTest.class.getResource(resourcePath).toURI()).getAbsolutePath();
    }

}
