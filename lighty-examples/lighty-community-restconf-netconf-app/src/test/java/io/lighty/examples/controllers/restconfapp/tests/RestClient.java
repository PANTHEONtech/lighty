/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.examples.controllers.restconfapp.tests;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("MethodName")
public class RestClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);
    private static final Duration REQUEST_TIMEOUT_DURATION = Duration.ofMillis(10_000L);

    private String baseUrl;
    private HttpClient httpClient;
    private static ExecutorService httpClientExecutor;

    @SuppressWarnings("checkstyle:illegalCatch")
    public RestClient(String baseUrl) {
        try {
            this.baseUrl = baseUrl;
            LOG.info("initializing HTTP client");
            httpClientExecutor = Executors.newSingleThreadExecutor();
            this.httpClient = HttpClient.newBuilder().executor(httpClientExecutor).build();
        } catch (Exception e) {
            LOG.error("RestClient init ERROR: ", e);
        }
    }

    protected HttpResponse<String> sendPatchRequestJSON(final String path, final String payload)
            throws InterruptedException, IOException {
        LOG.info("Sending PATCH request to path: {}", path);
        final HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(path))
                .header("Content-Type", "application/json")
                .method("PATCH", BodyPublishers.ofString(payload))
                .timeout(REQUEST_TIMEOUT_DURATION)
                .build();
        return httpClient.send(getRequest, BodyHandlers.ofString());
    }

    public HttpResponse<String> GET(String uri) throws InterruptedException, IOException {
        final HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + uri))
                .header("Content-Type", "application/json")
                .GET()
                .timeout(REQUEST_TIMEOUT_DURATION)
                .build();
        return httpClient.send(getRequest, BodyHandlers.ofString());
    }

    public HttpResponse<String> POST(String uri, String data) throws InterruptedException, IOException {
        final HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + uri))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(data))
                .timeout(REQUEST_TIMEOUT_DURATION)
                .build();
        return httpClient.send(postRequest, BodyHandlers.ofString());
    }

    public  HttpResponse<String> PUT(String uri, String data) throws InterruptedException, IOException {
        final HttpRequest putRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + uri))
                .header("Content-Type", "application/json")
                .PUT(BodyPublishers.ofString(data))
                .timeout(REQUEST_TIMEOUT_DURATION)
                .build();
        return httpClient.send(putRequest, BodyHandlers.ofString());
    }

    @SuppressWarnings("AbbreviationAsWordInName")
    public HttpResponse<String> DELETE(String uri) throws InterruptedException, IOException {
        final HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + uri))
                .header("Content-Type", "application/json")
                .DELETE()
                .timeout(REQUEST_TIMEOUT_DURATION)
                .build();
        return httpClient.send(deleteRequest, BodyHandlers.ofString());
    }

    @Override
    public void close() throws Exception {
        httpClientExecutor.shutdownNow();
    }
}
