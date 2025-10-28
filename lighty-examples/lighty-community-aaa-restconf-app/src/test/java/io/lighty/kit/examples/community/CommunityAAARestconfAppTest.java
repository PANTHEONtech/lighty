/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.kit.examples.community;

import io.lighty.kit.examples.community.aaa.restconf.Main;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CommunityAAARestconfAppTest {

    private static final String TEST_ADDRESS = "http://localhost:8888/restconf/data/ietf-yang-library:modules-state";
    private static final String BASIC_AUTH =
            "Basic " + Base64.getEncoder().encodeToString(("Admin:Admin123").getBytes(StandardCharsets.UTF_8));
    private static final String BASIC_AUTH_WRONG =
            "Basic " + Base64.getEncoder().encodeToString(("Admin:wrong").getBytes(StandardCharsets.UTF_8));
    private static final String AUTH = "Authorization";
    private static final String TEST_CONFIG_JSON = "/testConfig.json";
    private static HttpClient httpClient;
    private static Main main;

    @BeforeClass
    public static void startUp() {
        final URL config = CommunityAAARestconfAppTest.class.getResource(TEST_CONFIG_JSON);
        main = new Main();
        main.start(new String[]{config.getPath()}, false);
        httpClient = HttpClient.newHttpClient();
    }

    @AfterClass
    public static void tearDown() {
        httpClient = null;
        main.shutdown();
    }

    @Test
    public void readDataCorrectCredentials() throws Exception {
        HttpResponse<String> response
                = httpClient.send(createGetRequestJson(BASIC_AUTH), BodyHandlers.ofString());
        Assert.assertEquals(HttpStatus.OK_200, response.statusCode());
    }

    @Test
    public void readDataWrongCredentials() throws Exception {
        HttpResponse<String> response
                = httpClient.send(createGetRequestJson(BASIC_AUTH_WRONG), BodyHandlers.ofString());
        Assert.assertEquals(HttpStatus.UNAUTHORIZED_401, response.statusCode());
    }

    private HttpRequest createGetRequestJson(final String basicAuth) {
        return HttpRequest.newBuilder()
                .uri(URI.create(TEST_ADDRESS))
                .timeout(Duration.ofMinutes(1))
                .header(AUTH, basicAuth)
                .build();
    }
}
