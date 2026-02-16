/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import io.lighty.applications.rnc.module.config.RncLightyModuleConfigUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

class OpenApiTest {
    private static final String BASE_URL = "http://localhost:8888";
    private static final String PRIMARY_NAME = "urls.primaryName";
    private static final String APIDOC_INDEX = "/openapi/explorer/index.html";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private RncLightyModule rncModule;

    @DataProvider(name = "openApiUris")
    public static String[] openApiUris() {
        return new String[]{
            APIDOC_INDEX,
            APIDOC_INDEX + "#/controller%20aaa-app-config",
            APIDOC_INDEX + "#/controller%20netconf-node-topology",
            APIDOC_INDEX + "#/controller%20network-topology/get_restconf_data_network_topology_network_topology",
            APIDOC_INDEX + "#/controller%20cluster-admin"};
    }

    @BeforeClass
    void startUp() throws Exception {
        final var configPath = Paths.get(Objects.requireNonNull(this.getClass()
                .getResource("/openapi_config.json")).toURI());
        rncModule = new RncLightyModule(RncLightyModuleConfigUtils.loadConfigFromFile(configPath));
        assertTrue(rncModule.initModules());
    }

    @AfterClass
    void tearDown() {
        assertTrue(rncModule.close());
    }

    @Test(dataProvider = "openApiUris")
    void testOpenApiGet(final String uri) throws Exception {
        final var response = getRequest(uri, null, null);
        assertNotNull(response, String.format("OpenApi response for url [%s] is empty", uri));
        assertEquals(response.statusCode(), 200,
                String.format("OpenApi response for url [%s] is not 200 but [%s]", uri, response.statusCode()));
    }

    @Test
    void testOpenApiGetWithQuery() throws Exception {
        final var apidoc = getRequest(APIDOC_INDEX, PRIMARY_NAME, "Controller resources - RestConf RFC 8040");
        assertNotNull(apidoc);
        assertEquals(apidoc.statusCode(), 200);
    }

    private HttpResponse<String> getRequest(final String uri, final String queryKey, final String queryValue)
            throws Exception {
        final var requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + uri))
                .header("Content-Type", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(5));
        if (queryKey != null && queryValue != null) {
            requestBuilder.header(queryKey, queryValue);
        }
        return HTTP_CLIENT.send(requestBuilder.build(), BodyHandlers.ofString());
    }
}
