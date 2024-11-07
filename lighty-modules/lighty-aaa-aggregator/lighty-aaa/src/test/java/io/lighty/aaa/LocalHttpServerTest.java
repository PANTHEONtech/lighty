/*
 * Copyright (c) 2019 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa;

import io.lighty.server.LightyServerBuilder;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.servlet.Servlet;
import org.eclipse.jetty.server.Server;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.aaa.web.jetty.JettyWebServer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class LocalHttpServerTest {

    private static final String TEST_SERVLET = "/TestServlet";

    @Mock
    Servlet servlet;

    @BeforeClass
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void initLocalHttpServerTest() throws Exception {
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 8888);
        LightyServerBuilder serverBuilder = new LightyServerBuilder(socketAddress);
        JettyWebServer server = serverBuilder.build();
        Server jettyServer = null;
        try {
            // Use AccessController.doPrivileged to allow access to the private field
            Field serverField = AccessController.doPrivileged((PrivilegedAction<Field>) () -> {
                try {
                    Field field = JettyWebServer.class.getDeclaredField("server");
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Field not found", e);
                }
            });

            jettyServer = (Server) serverField.get(server);

            // Only set the handler if no handler is already set by JaxRsEndpoint

        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set handler on JettyWebServer", e);
        }
        LocalHttpServer localHttpServer = new LocalHttpServer(serverBuilder);
        localHttpServer.registerServlet(TEST_SERVLET, servlet, null);

        server.start();
        Assert.assertTrue(jettyServer.isStarted());
        Assert.assertTrue(jettyServer.isRunning());
        server.stop();
        Assert.assertFalse(jettyServer.isStarted());
        Assert.assertFalse(jettyServer.isRunning());
    }
}
