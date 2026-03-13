/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.examples.controllers.restconfapp.tests;

import io.lighty.examples.controllers.restconfapp.Main;
import java.io.IOException;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test starts lighty.io RESTCONF / NETCONF application.
 * RESTCONF API is available at http://localhost:8888/restconf
 * is used by REST client to test access to global data store.
 * This is integration test and requires free port 8888 on localhost.
 * This test is roughly same as single-feature test in OpenDaylight which starts:
 * feature:install odl-netconf-all
 */
class RestconfAppTest {

    private static final Logger LOG = LoggerFactory.getLogger(RestconfAppTest.class);

    private static Main restconfApp;
    private static RestClient restClient;

    @BeforeAll
    public static void init() {
        restconfApp = new Main();
        restconfApp.start();
        restClient = new RestClient("http://localhost:8888/");
    }

    /**
     * Perform basic GET operations via RESTCONF.
     */
    @Test
    void simpleApplicationTest() throws IOException, InterruptedException {
        HttpResponse<String> operations;
        restClient.POST("restconf/data/network-topology:network-topology/topology=topology-netconf",
            """
                    {
                        "netconf-topology:node": [
                            {
                                "node-id": "new-node"
                            }
                        ]
                    }""");

        operations = restClient.GET("restconf/operations");
        Assertions.assertEquals(operations.statusCode(), 200);
        operations = restClient.GET("restconf/data/network-topology:network-topology?content=config");
        Assertions.assertEquals(operations.statusCode(), 200);
        operations = restClient.GET("restconf/data/network-topology:network-topology?content=nonconfig");
        Assertions.assertEquals(operations.statusCode(), 200);
    }

    /**
     * Check if OpenApi service and UI is responding.
     */
    @Test
    void openApiURLsTest() throws IOException, InterruptedException {
        HttpResponse<String> operations;
        operations = restClient.GET("openapi/explorer/index.html");
        Assertions.assertEquals(operations.statusCode(), 200);
        operations = restClient.GET("openapi/explorer/index.html");
        Assertions.assertEquals(operations.statusCode(), 200);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    @AfterAll
    public static void shutdown() {
        restconfApp.shutdown();
        try {
            restClient.close();
        } catch (Exception e) {
            LOG.error("Shutdown of restClient failed", e);
        }
    }

}
