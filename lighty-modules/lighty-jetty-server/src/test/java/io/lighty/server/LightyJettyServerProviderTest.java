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
import org.opendaylight.aaa.filterchain.configuration.impl.CustomFilterAdapterConfigurationImpl;
import org.opendaylight.aaa.filterchain.filters.CustomFilterAdapter;
import org.opendaylight.aaa.web.FilterDetails;
import org.opendaylight.aaa.web.WebContext;
import org.testng.annotations.Test;

public class LightyJettyServerProviderTest {

    private static final int PORT = 8080;
    private static final String HTTP2_CONFIG = "/http2Config.json";
    private static final String HTTPS_CONFIG = "/httpsConfig.json";

    @Test
    public void testServerBuilder() {
        final var serverBuilder = new LightyJettyServerProvider(new InetSocketAddress(PORT));
        final var server = initLightyServer(serverBuilder);
        assertNotNull(server);
    }

    @Test
    public void testHttpsDefaultServerBuilder() throws Exception {
        final var lightyServerConfig = LightyServerConfigUtils.getDefaultLightyServerConfig();
        final var serverBuilder = new HttpsLightyServerProvider(new InetSocketAddress(PORT),
            lightyServerConfig.getSecurityConfig());
        final var server = initLightyServer(serverBuilder);
        assertNotNull(server);
    }

    @Test
    public void testHttp2DefaultServerBuilder() throws Exception {
        final var lightyServerConfig = LightyServerConfigUtils.getDefaultLightyServerConfig();
        final var serverBuilder = new Http2LightyServerProvider(new InetSocketAddress(PORT),
            lightyServerConfig.getSecurityConfig());
        final var server = initLightyServer(serverBuilder);
        assertNotNull(server);
    }

    @Test
    public void testHttp2CustomServerBuilder() throws Exception {
        final var resourceAsStream = LightyJettyServerProviderTest.class.getResourceAsStream(HTTP2_CONFIG);
        final var lightyServerConfig = LightyServerConfigUtils.getServerConfiguration(resourceAsStream);
        final var serverBuilder = new Http2LightyServerProvider(new InetSocketAddress(PORT),
            lightyServerConfig.getSecurityConfig());
        final var server = initLightyServer(serverBuilder);
        assertNotNull(server);
    }

    @Test
    public void testHttpsCustomServerBuilder() throws Exception {
        final var resourceAsStream = LightyJettyServerProviderTest.class.getResourceAsStream(HTTPS_CONFIG);
        final var lightyServerConfig = LightyServerConfigUtils.getServerConfiguration(resourceAsStream);
        final var serverBuilder = new HttpsLightyServerProvider(new InetSocketAddress(PORT),
            lightyServerConfig.getSecurityConfig());
        final var server = initLightyServer(serverBuilder);
        assertNotNull(server);
    }

    private static LightyJettyServerProvider initLightyServer(final LightyJettyServerProvider serverProvider) {
        final WebContext webContext = WebContext.builder()
            .name("ExampleContext")
            .contextPath("/path")
            .addFilter(FilterDetails.builder()
                .filter(new CustomFilterAdapter(new CustomFilterAdapterConfigurationImpl()))
                .addUrlPattern("/*")
                .build())
            .putContextParam("key", "value")
            .build();
        serverProvider.addContextHandler(webContext);
        return serverProvider;
    }
}
