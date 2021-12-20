/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.Futures;
import io.lighty.applications.rnc.module.RncLightyModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MainTest {

    private static final Duration REQUEST_TIMEOUT_DURATION = Duration.ofMillis(10_000L);

    @Test
    public void testStartWithDefaultConfiguration() {
        Main app = spy(new Main());
        RncLightyModule lighty = mock(RncLightyModule.class);
        doReturn(Futures.immediateFuture(true)).when(lighty).start();
        doReturn(Futures.immediateFuture(true)).when(lighty).shutdown();
        doReturn(lighty).when(app).createRncLightyModule(any());
        app.start(new String[] {});
    }

    @Test
    public void testStartWithConfigFile() {
        Main app = spy(new Main());
        RncLightyModule lighty = mock(RncLightyModule.class);
        doReturn(Futures.immediateFuture(true)).when(lighty).start();
        doReturn(Futures.immediateFuture(true)).when(lighty).shutdown();
        doReturn(lighty).when(app).createRncLightyModule(any());
        app.start(new String[] {"-c","src/main/resources/configuration.json"});
    }

    @Test
    public void testStartWithConfigFileNoSuchFile() {
        Main app = spy(new Main());
        verify(app, never()).createRncLightyModule(any());
        app.start(new String[] {"-c","no_config.json"});
    }

    @Test
    public void swaggerURLsTest() throws IOException, InterruptedException {
        Main app = new Main();
        app.start(new String[] {});
        HttpResponse<String> operations;
        operations = GET("http://localhost:8888/","apidoc/openapi3/18/apis/single");
        Assert.assertEquals(operations.statusCode(), 200);
        operations = GET("http://localhost:8888/","apidoc/explorer/index.html");
        Assert.assertEquals(operations.statusCode(), 200);
    }

    //used for testing swagger for HTTP GET response
    public HttpResponse<String> GET(String baseUrl,String uri) throws InterruptedException, IOException {

        ExecutorService httpClientExecutor = Executors.newSingleThreadExecutor();
        HttpClient httpClient = HttpClient.newBuilder().executor(httpClientExecutor).build();

        final HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + uri))
                .header("Content-Type", "application/json")
                .GET()
                .timeout(REQUEST_TIMEOUT_DURATION)
                .build();
        return httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
    }
}
