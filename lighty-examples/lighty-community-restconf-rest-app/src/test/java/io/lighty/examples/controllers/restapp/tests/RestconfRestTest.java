/*
 * Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.restapp.tests;

import io.lighty.examples.controllers.restapp.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class RestconfRestTest {
    private static final Logger LOG = LoggerFactory.getLogger(RestconfRestTest.class);

    private static final String RPC_PATH = "restconf/operations/hello:hello-world";
    private static final String RPC_INPUT = "{\"input\":{\"name\":\"Your Name\"}}";
    private static final String RPC_OUTPUT = "{\"hello:output\":{\"greeting\":\"Hello Your Name\"}}";
    private static final long SLEEP_AFTER_SHUTDOWN_TIMEOUT_MILLIS = 3_000;

    private static Main restconfApp;
    private static RestClient restClient;

    @BeforeClass
    public static void init() {
        restconfApp = new Main();
        restconfApp.start();
        restClient = new RestClient("http://localhost:8888/");
    }

    @Test
    public void rpcTest() throws Exception {
        final var response = restClient.POST(RPC_PATH, RPC_INPUT);
        assertEquals(response.statusCode(), 200);
        assertEquals(response.body(), RPC_OUTPUT);
    }

    @AfterClass
    public static void shutdown() {
        restconfApp.shutdown();
        try {
            restClient.close();
            Thread.sleep(SLEEP_AFTER_SHUTDOWN_TIMEOUT_MILLIS);
        } catch (Exception e) {
            LOG.error("Shutdown of restClient failed", e);
        }
    }
}
