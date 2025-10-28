/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controller.actions;

import static java.net.http.HttpRequest.BodyPublishers;
import static java.net.http.HttpRequest.newBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
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

    public HttpResponse<String> GET(String uri) throws InterruptedException, IOException {
        final HttpRequest getRequest = newBuilder()
                .uri(URI.create(baseUrl + uri))
                .header("Content-Type", "application/json")
                .GET()
                .timeout(REQUEST_TIMEOUT_DURATION)
                .build();
        return httpClient.send(getRequest, BodyHandlers.ofString());
    }

    public HttpResponse<String> POST(String uri, String data) throws InterruptedException, IOException {
        final HttpRequest postRequest = newBuilder()
                .uri(URI.create(baseUrl + uri))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(data))
                .timeout(REQUEST_TIMEOUT_DURATION)
                .build();
        return httpClient.send(postRequest, BodyHandlers.ofString());
    }

    @Override
    public void close() throws Exception {
        httpClientExecutor.shutdownNow();
    }
}
