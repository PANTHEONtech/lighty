/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.test;

import com.fasterxml.jackson.databind.JsonNode;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * This test starts lighty.io and spring boot application.
 * REST APIs which are available at http://localhost:8080
 * are used by REST client to test access to global data store.
 * This is integration test and requires free port 8080 on localhost.
 */
public class SpringBootAppTest {

    final private static Logger LOG = LoggerFactory.getLogger(SpringBootAppTest.class);

    private static ConfigurableApplicationContext appContext;
    private static RestClient restClient;

    @BeforeClass
    public static void init() {
        appContext = SpringApplication.run(MainApp.class, new String[]{});
        restClient = new RestClient("http://localhost:8080/");
    }

    @Test
    public void simpleApplicationTest() {
        ContentResponse contentResponse = null;
        JsonNode jsonData = null;
        ObjectMapper mapper = new ObjectMapper();
        String netconfTopologyId = "topology-netconf";
        String topologyId = "test-topology";
        String[] expectedTopologyIds = new String[] { netconfTopologyId, topologyId };
        try {
            Assert.assertNotNull(appContext);

            //1. get list of topology Ids
            //   only one topology ID "topology-netconf" is expected
            contentResponse = restClient.GET("topology/list");
            Assert.assertEquals(contentResponse.getStatus(), 200);
            String[] topologyIds = mapper.readValue(contentResponse.getContent(), String[].class);
            Assert.assertNotNull(topologyIds);
            Assert.assertEquals(topologyIds.length, 1);
            Assert.assertEquals(topologyIds[0], netconfTopologyId);

            //2. create new empty topology and check if it was created
            //   this step creates empty topology instance in global data store
            contentResponse = restClient.PUT("topology/id/" + topologyId);
            Assert.assertEquals(contentResponse.getStatus(), 200);
            contentResponse = restClient.GET("topology/list");
            topologyIds = mapper.readValue(contentResponse.getContent(), String[].class);
            Assert.assertNotNull(topologyIds);
            Assert.assertEquals(topologyIds.length, 2);
            Collection<String> d = Arrays.asList(topologyIds);
            Assert.assertTrue(d.containsAll(Arrays.asList(expectedTopologyIds)));

            //3. delete created topology and check if it was deleted
            //   this step removes created topology
            contentResponse = restClient.DELETE("topology/id/" + topologyId);
            Assert.assertEquals(contentResponse.getStatus(), 200);
            contentResponse = restClient.GET("topology/list");
            topologyIds = mapper.readValue(contentResponse.getContent(), String[].class);
            Assert.assertNotNull(topologyIds);
            Assert.assertEquals(topologyIds.length, 1);
            Assert.assertEquals(topologyIds[0], netconfTopologyId);

        } catch (IOException | TimeoutException | ExecutionException | InterruptedException e) {
            Assert.fail();
        }
    }

    @AfterClass
    public static void shutdown() {
        if (appContext != null) {
            appContext.stop();
        }
        if (restClient != null) {
            try {
                restClient.close();
            } catch (Exception e) {
                LOG.error("Exception: ", e);
            }
        }
    }

}
