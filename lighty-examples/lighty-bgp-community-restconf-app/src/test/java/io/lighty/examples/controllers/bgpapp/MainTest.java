/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.examples.controllers.bgpapp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MainTest {

    private static Main app;

    @BeforeAll
    static void startApp() throws Exception {
        app = new Main();
        app.start(new String[]{});
    }

    @Test
    void appStartedTest() {
        assertTrue(app.isRunning());
    }

    @AfterAll
    static void stop() {
        app.stop();
        assertFalse(app.isRunning());
    }

}
