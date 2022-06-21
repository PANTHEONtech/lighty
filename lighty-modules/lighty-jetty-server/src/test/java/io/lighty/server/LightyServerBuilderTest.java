/*
   Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.

   This program and the accompanying materials are made available under the
   terms of the Eclipse Public License v1.0 which accompanies this distribution,
   and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server;

import static org.testng.AssertJUnit.assertNotNull;

import io.lighty.server.util.LightyServerConfigUtils;
import java.net.InetSocketAddress;
import java.util.EventListener;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.testng.annotations.Test;

public class LightyServerBuilderTest {

    private static final int PORT = 8080;
    private static final String HTTP2_CONFIG = "/http2Config.json";
    private static final String HTTPS_CONFIG = "/httpsConfig.json";

    @Test
    public void testServerBuilder() {
        final var serverBuilder = new LightyServerBuilder(new InetSocketAddress(PORT));
        final var server = initLightyServer(serverBuilder);
        assertNotNull(server);
    }

    @Test
    public void testHttpsDefaultServerBuilder() throws Exception {
        final var lightyServerConfig = LightyServerConfigUtils.getDefaultLightyServerConfig();
        final var serverBuilder = new HttpsLightyServerBuilder(new InetSocketAddress(PORT),
                lightyServerConfig.getSecurityConfig());
        final var server = initLightyServer(serverBuilder);
        assertNotNull(server);
    }

    @Test
    public void testHttp2DefaultServerBuilder() throws Exception {
        final var lightyServerConfig = LightyServerConfigUtils.getDefaultLightyServerConfig();
        final var serverBuilder = new Http2LightyServerBuilder(new InetSocketAddress(PORT),
                lightyServerConfig.getSecurityConfig());
        final var server = initLightyServer(serverBuilder);
        assertNotNull(server);
    }

    @Test
    public void testHttp2CustomServerBuilder() throws Exception {
        final var resourceAsStream = LightyServerBuilderTest.class.getResourceAsStream(HTTP2_CONFIG);
        final var lightyServerConfig = LightyServerConfigUtils.getServerConfiguration(resourceAsStream);
        final var serverBuilder = new Http2LightyServerBuilder(new InetSocketAddress(PORT),
                lightyServerConfig.getSecurityConfig());
        final var server = initLightyServer(serverBuilder);
        assertNotNull(server);
    }

    @Test
    public void testHttpsCustomServerBuilder() throws Exception {
        final var resourceAsStream = LightyServerBuilderTest.class.getResourceAsStream(HTTPS_CONFIG);
        final var lightyServerConfig = LightyServerConfigUtils.getServerConfiguration(resourceAsStream);
        final var serverBuilder = new HttpsLightyServerBuilder(new InetSocketAddress(PORT),
                lightyServerConfig.getSecurityConfig());
        final var server = initLightyServer(serverBuilder);
        assertNotNull(server);
    }

    private static Server initLightyServer(final LightyServerBuilder serverBuilder) {
        final var filterHolder = new FilterHolder();
        final var contexts = new ContextHandlerCollection();
        serverBuilder.addCommonEventListener(new EventListener(){});
        serverBuilder.addCommonFilter(filterHolder, "/path");
        serverBuilder.addCommonInitParameter("key", "value");
        serverBuilder.addContextHandler(contexts);
        return serverBuilder.build();
    }
}
