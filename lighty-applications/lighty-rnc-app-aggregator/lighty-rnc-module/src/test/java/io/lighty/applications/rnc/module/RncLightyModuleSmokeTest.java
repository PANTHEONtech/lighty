/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import io.lighty.applications.rnc.module.config.RncLightyModuleConfigUtils;
import io.lighty.core.controller.impl.config.ConfigurationException;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import org.eclipse.jetty.http.HttpStatus;
import org.testng.annotations.Test;

class RncLightyModuleSmokeTest {

    private static final String HTTPS_URI = "https://127.0.0.1:8888";
    private static final String HTTP_URI = "http://127.0.0.1:8888";
    private static final String TOPOLOGY_PATH = "/restconf/data/network-topology:network-topology";

    @Test
    void rncLightyModuleDefaultConfigTest() throws Exception {
        final var rncModule = new RncLightyModule(RncLightyModuleConfigUtils.loadDefaultConfig());
        assertTrue(rncModule.initModules());
        final var httpResponse = HttpClient.newHttpClient()
                .send(createGetRequest(HTTP_URI + TOPOLOGY_PATH), BodyHandlers.ofString());
        assertEquals(HttpStatus.OK_200, httpResponse.statusCode());
        assertTrue(rncModule.close());
    }

    @Test
    void rncLightyModuleHttpsTest() throws Exception {
        final var resource = RncLightyModuleSmokeTest.class.getResource("/httpsConfig.json");
        final var rncConfig = RncLightyModuleConfigUtils.loadConfigFromFile(Paths.get(resource.getPath()));
        final var rncModule = new RncLightyModule(rncConfig);
        assertTrue(rncModule.initModules());
        final var sslClient = getHttpClientWithSsl();
        final var httpResponse = sslClient.send(createGetRequest(HTTPS_URI + TOPOLOGY_PATH), BodyHandlers.ofString());
        assertEquals(HttpStatus.OK_200, httpResponse.statusCode());
        assertTrue(rncModule.close());
    }

    @Test
    void rncLightyModuleHttp2Test() throws Exception {
        final var resource = RncLightyModuleSmokeTest.class.getResource("/http2Config.json");
        final var rncConfig = RncLightyModuleConfigUtils.loadConfigFromFile(Paths.get(resource.getPath()));
        final var rncModule = new RncLightyModule(rncConfig);
        assertTrue(rncModule.initModules());
        final var sslClient = getHttpClientWithSsl();
        final var httpResponse = sslClient.send(createGetRequest(HTTPS_URI + TOPOLOGY_PATH), BodyHandlers.ofString());
        assertEquals(HttpStatus.OK_200, httpResponse.statusCode());
        assertTrue(rncModule.close());
    }

    @Test
    void rncLightyModuleStartFailed() throws ConfigurationException {
        final var config = spy(RncLightyModuleConfigUtils.loadDefaultConfig());
        when(config.getControllerConfig()).thenReturn(null);
        final var rncModule = new RncLightyModule(config);
        final var isStarted = rncModule.initModules();
        final var isClose = rncModule.close();

        assertFalse(isStarted);
        assertTrue(isClose);
    }

    private static HttpClient getHttpClientWithSsl() throws Exception {
        final var ssl = SSLContext.getInstance("SSL");
        ssl.init(null, getTrustedAll(), new SecureRandom());
        return HttpClient.newBuilder()
                .sslContext(ssl)
                .build();
    }

    private static HttpRequest createGetRequest(final String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(path))
                .timeout(Duration.ofMinutes(1))
                .build();
    }

    private static TrustManager[] getTrustedAll() {
        return new TrustManager[]{
            new X509ExtendedTrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String data, Socket socket) {
                    // Trust all client certificates
                }

                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String data, SSLEngine sslEngine) {
                    // Trust all client certificates
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, final String authType) {
                    // Trust all client certificates
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String data, Socket socket) {
                    // Trust all server certificates
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String data, SSLEngine sslEngine) {
                    // Trust all server certificates
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, final String authType) {
                    // Trust all server certificates
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }
        };
    }
}
