/*
   Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.

   This program and the accompanying materials are made available under the
   terms of the Eclipse Public License v1.0 which accompanies this distribution,
   and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server;

import static org.testng.AssertJUnit.assertNotNull;

import io.lighty.server.util.LightyServerConfigUtils;
import java.io.IOException;
import java.net.InetSocketAddress;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.eclipse.jetty.servlet.FilterHolder;
import org.opendaylight.aaa.web.FilterDetails;
import org.opendaylight.aaa.web.WebContext;
import org.opendaylight.aaa.web.jetty.JettyWebServer;
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

    private static JettyWebServer initLightyServer(final LightyServerBuilder serverBuilder) {
        final var filterHolder = new FilterHolder();
        WebContext.Builder builder = WebContext.builder().name("name").contextPath("/.").addListener(
            new ServletContextListener() {
            @Override
            public void contextInitialized(ServletContextEvent servletContextEvent) {
            }
            @Override
            public void contextDestroyed(ServletContextEvent servletContextEvent) {
            }
        }).addFilter(FilterDetails.builder().filter(
            new Filter() {
                @Override
                public void init(FilterConfig filterConfig) throws ServletException {
                }

                @Override
                public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
                }

                @Override
                public void destroy() {
                }
            }).addUrlPattern("/path")
            .putInitParam("key", "value").build());
        serverBuilder.addContextHandler(builder.build());
        return serverBuilder.build();
    }
}
