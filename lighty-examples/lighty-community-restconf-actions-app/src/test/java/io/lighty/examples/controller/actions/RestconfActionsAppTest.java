/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controller.actions;

import static org.testng.Assert.assertEquals;

import io.lighty.examples.controllers.actions.Main;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.eclipse.jetty.client.api.ContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This test starts Lighty.io RESTCONF Actions application.
 * RESTCONF API is available at http://localhost:8888/restconf
 * and it is used by REST client to test access to global data store.
 * This is integration test and requires free port 8888 on localhost.
 * This test is roughly same as single-feature test in OpenDaylight which starts:
 * feature:install odl-netconf-all
 */
public class RestconfActionsAppTest {
    private static final Logger LOG = LoggerFactory.getLogger(RestconfActionsAppTest.class);

    private static final String DOM_ACTION_PATH = "restconf/data/example-data-center:device/start";
    private static final String DOM_ACTION_INPUT = "{\"input\":{\"start-at\":\"2021-09-09T16:20:00Z\"}}";
    private static final String DOM_ACTION_OUTPUT =
            "{\"example-data-center:output\":{\"example-data-center:start-finished-at\":\"2021-09-09T16:20:00Z\"}}";
    private static final String BINDING_ACTION_PATH = "restconf/data/example-data-center:server=server-earth/reset";
    private static final String BINDING_ACTION_INPUT = "{\"input\":{\"reset-at\":\"2021-09-09T16:20:00Z\"}}";
    private static final String BINDING_ACTION_OUTPUT =
            "{\"example-data-center:output\":{\"example-data-center:reset-finished-at\":\"2021-09-09T16:20:00Z\"}}";
    private static final long SLEEP_AFTER_SHUTDOWN_TIMEOUT_MILLIS = 3_000;

    private static Main restconfApp;
    private static RestClient restClient;

    @BeforeClass
    public static void init() {
        restconfApp = new Main();
        restconfApp.start();
        restClient = new RestClient("http://localhost:8888/");
    }

    /**
     * Perform basic GET operations via RESTCONF.
     */
    @Test
    public void simpleApplicationTest() throws TimeoutException, ExecutionException, InterruptedException {
        ContentResponse operations = null;
        operations = restClient.GET("restconf/operations");
        assertEquals(operations.getStatus(), 200);
        operations = restClient.GET("restconf/data/network-topology:network-topology?content=config");
        assertEquals(operations.getStatus(), 200);
        operations = restClient.GET("restconf/data/network-topology:network-topology?content=nonconfig");
        assertEquals(operations.getStatus(), 200);
    }

    /**
     * Check if Swagger service and UI is responding.
     */
    @Test
    public void swaggerURLsTest() {
        ContentResponse operations = null;
        try {
            operations = restClient.GET("apidoc/openapi3/18/apis/single");
            assertEquals(operations.getStatus(), 200);
            operations = restClient.GET("apidoc/explorer/index.html");
            assertEquals(operations.getStatus(), 200);
        } catch (TimeoutException | ExecutionException | InterruptedException e) {
            Assert.fail();
        }
    }

    /**
     * Check that it is possible to invoke example DOM action.
     */
    @Test
    public void domActionInvocationTest() throws Exception {
        final var response = restClient.POST(DOM_ACTION_PATH, DOM_ACTION_INPUT);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.getContentAsString(), DOM_ACTION_OUTPUT);
    }

    @Test
    public void bindingActionInvocationTest() throws Exception {
        final var response = restClient.POST(BINDING_ACTION_PATH, BINDING_ACTION_INPUT);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.getContentAsString(), BINDING_ACTION_OUTPUT);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
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
