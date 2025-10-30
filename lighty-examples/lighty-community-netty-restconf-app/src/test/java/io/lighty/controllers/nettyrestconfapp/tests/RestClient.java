/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.controllers.nettyrestconfapp.tests;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.aaa.api.PasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("MethodName")
public class RestClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);
    private static final Duration REQUEST_TIMEOUT_DURATION = Duration.ofMillis(10_000L);

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ExecutorService httpClientExecutor;

    @SuppressWarnings("checkstyle:illegalCatch")
    public RestClient(String baseUrl) {
        this.baseUrl = baseUrl;
        try {
            LOG.info("initializing HTTP client");
            httpClientExecutor = Executors.newSingleThreadExecutor();
            this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .executor(httpClientExecutor)
                .build();
        } catch (Exception e) {
            LOG.error("RestClient init ERROR: ", e);
            throw new RuntimeException("Failed to initialize RestClient", e);
        }
    }

    private String buildAuthHeader(PasswordCredentials credentials) {
        String userPass = credentials.username() + ":" + credentials.password();
        return "Basic " + Base64.getEncoder().encodeToString(userPass.getBytes(StandardCharsets.UTF_8));
    }

    protected HttpResponse<String> sendPatchRequestJSON(
        final String path,
        final String payload,
        final PasswordCredentials credentials)
        throws InterruptedException, IOException {

        LOG.info("Sending PATCH request to path: {}", path);
        final HttpRequest patchRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("Authorization", buildAuthHeader(credentials))
            .method("PATCH", BodyPublishers.ofString(payload))
            .timeout(REQUEST_TIMEOUT_DURATION)
            .build();
        return httpClient.send(patchRequest, BodyHandlers.ofString());
    }

    public HttpResponse<String> GET(String uri, PasswordCredentials credentials)
        throws InterruptedException, IOException {

        final HttpRequest getRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + uri))
            .header("Content-Type", "application/json")
            .header("Authorization", buildAuthHeader(credentials))
            .GET()
            .timeout(REQUEST_TIMEOUT_DURATION)
            .build();
        return httpClient.send(getRequest, BodyHandlers.ofString());
    }

    public HttpResponse<String> POST(String uri, String data, PasswordCredentials credentials)
        throws InterruptedException, IOException {

        final HttpRequest postRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + uri))
            .header("Content-Type", "application/json")
            .header("Authorization", buildAuthHeader(credentials))
            .POST(BodyPublishers.ofString(data))
            .timeout(REQUEST_TIMEOUT_DURATION)
            .build();
        return httpClient.send(postRequest, BodyHandlers.ofString());
    }

    public HttpResponse<String> PUT(String uri, String data, PasswordCredentials credentials)
        throws InterruptedException, IOException {

        final HttpRequest putRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + uri))
            .header("Content-Type", "application/json")
            .header("Authorization", buildAuthHeader(credentials))
            .PUT(BodyPublishers.ofString(data))
            .timeout(REQUEST_TIMEOUT_DURATION)
            .build();
        return httpClient.send(putRequest, BodyHandlers.ofString());
    }

    @SuppressWarnings("AbbreviationAsWordInName")
    public HttpResponse<String> DELETE(String uri, PasswordCredentials credentials)
        throws InterruptedException, IOException {

        final HttpRequest deleteRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + uri))
            .header("Content-Type", "application/json")
            .header("Authorization", buildAuthHeader(credentials))
            .DELETE()
            .timeout(REQUEST_TIMEOUT_DURATION)
            .build();
        return httpClient.send(deleteRequest, BodyHandlers.ofString());
    }

    @Override
    public void close() {
        httpClientExecutor.shutdownNow();
    }
}