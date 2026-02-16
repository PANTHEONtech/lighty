/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.controllers.nettyrestconfapp.tests;

import io.lighty.controllers.nettyrestconfapp.Main;
import java.io.IOException;
import java.net.URL;
import java.net.http.HttpResponse;
import org.opendaylight.aaa.api.PasswordCredentials;
import org.opendaylight.aaa.tokenauthrealm.auth.PasswordCredentialBuilder;
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
class RestconfAppTest {

    private static final Logger LOG = LoggerFactory.getLogger(RestconfAppTest.class);
    private static final PasswordCredentials CORRECT_CREDENTIALS =
        new PasswordCredentialBuilder().setUserName("Admin").setPassword("Admin123").build();
    private static final PasswordCredentials INCORRECT_CREDENTIALS =
        new PasswordCredentialBuilder().setUserName("Wrong").setPassword("Credential").build();

    static final URL CONFIG = RestconfAppTest.class.getResource("/testConfig.json");
    private static Main restconfApp;
    private static RestClient restClient;

    @BeforeClass
    public static void init() {
        restconfApp = new Main();
        restconfApp.start(new String[]{CONFIG.getPath()}, false);
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
                    }""", CORRECT_CREDENTIALS);

        operations = restClient.GET(
            "restconf/data",CORRECT_CREDENTIALS);
        Assert.assertEquals(operations.statusCode(), 200);
        operations = restClient.GET(
            "restconf/data/network-topology:network-topology?content=config", CORRECT_CREDENTIALS);
        Assert.assertEquals(operations.statusCode(), 200);
        Assert.assertTrue(operations.body().contains("new-node"));
        operations = restClient.GET(
            "restconf/data/network-topology:network-topology?content=nonconfig", CORRECT_CREDENTIALS);
        Assert.assertEquals(operations.statusCode(), 200);
    }

    @Test
    void simpleAuthorizationTest() throws IOException, InterruptedException {
        HttpResponse<String> authStatus;

        authStatus = restClient.GET("restconf/data", CORRECT_CREDENTIALS);
        Assert.assertEquals(authStatus.statusCode(), 200);
        authStatus = restClient.GET("restconf/data", INCORRECT_CREDENTIALS);
        Assert.assertEquals(authStatus.statusCode(), 401);
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
