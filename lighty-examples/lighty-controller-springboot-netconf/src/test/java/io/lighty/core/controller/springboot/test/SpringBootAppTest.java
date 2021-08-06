/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lighty.core.controller.springboot.MainApp;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This test starts lighty.io and spring boot application.
 * REST APIs which are available at http://localhost:8080
 * are used by REST client to test access to global data store.
 * This is integration test and requires free port 8080 on localhost.
 */
public class SpringBootAppTest {

    final private static Logger LOG = LoggerFactory.getLogger(SpringBootAppTest.class);
    public static final long SLEEP_AFTER_SHUTDOWN_TIMEOUT_MILLIS = 1_000;

    private static ConfigurableApplicationContext appContext;
    private static RestClient restClient;

    @BeforeClass
    public static void init() {
        appContext = SpringApplication.run(MainApp.class, new String[]{});
        restClient = new RestClient("http://localhost:8888/");
    }

    @Test
    public void simpleApplicationTest() throws Exception {
        ContentResponse contentResponse = null;
        ObjectMapper mapper = new ObjectMapper();
        String netconfTopologyId = "topology-netconf";
        String topologyId = "test-topology";
        String[] expectedTopologyIds = new String[] { netconfTopologyId, topologyId };

        Assert.assertNotNull(appContext);

        //0. login as user with admin role
        //   only user with admin role is able to perform all operations
        String loginRequest = "{ \"username\": \"bob\", \"password\": \"secret\" }";
        contentResponse = restClient.POST("services/security/login", loginRequest);
        Assert.assertEquals(200, contentResponse.getStatus());

        //1. get list of topology Ids
        //   only one topology ID "topology-netconf" is expected
        contentResponse = restClient.GET("services/data/topology/list");
        Assert.assertEquals(200, contentResponse.getStatus());
        ArrayList<String> topologyIds = mapper.readValue(contentResponse.getContent(), ArrayList.class);
        Assert.assertNotNull(topologyIds);
        Assert.assertEquals(1, topologyIds.size(), 1);
        Assert.assertEquals(netconfTopologyId, topologyIds.get(0));

        //2. create new empty topology and check if it was created
        //   this step creates empty topology instance in global data store
        contentResponse = restClient.PUT("services/data/topology/id/" + topologyId);
        Assert.assertEquals(200, contentResponse.getStatus());
        contentResponse = restClient.GET("services/data/topology/list");
        topologyIds = mapper.readValue(contentResponse.getContent(), ArrayList.class);
        Assert.assertNotNull(topologyIds);
        Assert.assertEquals(2, topologyIds.size());
        Assert.assertTrue(topologyIds.containsAll(Arrays.asList(expectedTopologyIds)));

        //3. delete created topology and check if it was deleted
        //   this step removes created topology
        contentResponse = restClient.DELETE("services/data/topology/id/" + topologyId);
        Assert.assertEquals(contentResponse.getStatus(), 200);
        contentResponse = restClient.GET("services/data/topology/list");
        topologyIds = mapper.readValue(contentResponse.getContent(), ArrayList.class);
        Assert.assertNotNull(topologyIds);
        Assert.assertEquals(1, topologyIds.size(), 1);
        Assert.assertEquals(netconfTopologyId, topologyIds.get(0));

        //4. user session logout
        contentResponse = restClient.GET("services/security/logout");
        Assert.assertEquals(contentResponse.getStatus(), 200);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    @AfterClass
    public static void shutdown() {
        if (appContext != null) {
            appContext.stop();
        }
        if (restClient != null) {
            try {
                restClient.close();
                Thread.sleep(SLEEP_AFTER_SHUTDOWN_TIMEOUT_MILLIS);
            } catch (Exception e) {
                LOG.error("Shutdown of restClient failed", e);
            }
        }
    }

}
