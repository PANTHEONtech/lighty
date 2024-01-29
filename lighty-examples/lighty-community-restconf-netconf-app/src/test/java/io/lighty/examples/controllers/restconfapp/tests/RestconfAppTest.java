/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.examples.controllers.restconfapp.tests;

import io.lighty.examples.controllers.restconfapp.Main;
import java.io.IOException;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This test starts lighty.io RESTCONF / NETCONF application.
 * RESTCONF API is available at http://localhost:8888/restconf
 * is used by REST client to test access to global data store.
 * This is integration test and requires free port 8888 on localhost.
 * This test is roughly same as single-feature test in OpenDaylight which starts:
 * feature:install odl-netconf-all
 */
public class RestconfAppTest {

    private static final Logger LOG = LoggerFactory.getLogger(RestconfAppTest.class);

    private static Main restconfApp;
    private static RestClient restClient;

    @BeforeClass
    public static void init() {
        restconfApp = new Main();
        restconfApp.start();
        restClient = new RestClient("http://localhost:8181/");
    }

    /**
     * Perform basic GET operations via RESTCONF.
     */
    @Test
    public void simpleApplicationTest() throws IOException, InterruptedException {
        HttpResponse<String> operations;
        restClient.POST("rests/data/network-topology:network-topology/topology=topology-netconf",
            """
                    {
                        "netconf-topology:node": [
                            {
                                "node-id": "new-node"
                            }
                        ]
                    }""");

        operations = restClient.GET("rests/operations");
        Assert.assertEquals(operations.statusCode(), 200);
        operations = restClient.GET("rests/data/network-topology:network-topology?content=config");
        Assert.assertEquals(operations.statusCode(), 200);
        operations = restClient.GET("rests/data/network-topology:network-topology?content=nonconfig");
        Assert.assertEquals(operations.statusCode(), 200);
    }

    /**
     * Check if OpenApi service and UI is responding.
     */
    @Test
    public void openApiURLsTest() throws IOException, InterruptedException {
        HttpResponse<String> operations;
        operations = restClient.GET("openapi/explorer/index.html");
        Assert.assertEquals(operations.statusCode(), 200);
        operations = restClient.GET("openapi/explorer/index.html");
        Assert.assertEquals(operations.statusCode(), 200);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    @AfterClass
    public static void shutdown() {
        restconfApp.shutdown();
        try {
            restClient.close();
        } catch (Exception e) {
            LOG.error("Shutdown of restClient failed", e);
        }
    }

}
