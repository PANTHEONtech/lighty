/*
 * Copyright (c) 2019 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa;

import io.lighty.server.LightyServerBuilder;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import javax.servlet.Servlet;
import org.eclipse.jetty.server.Server;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class LocalHttpServerTest {

    private static final String TEST_SERVLET = "TestServlet";

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
        Server server = serverBuilder.build();
        LocalHttpServer localHttpServer = new LocalHttpServer(serverBuilder);
        localHttpServer.registerServlet(TEST_SERVLET, servlet, null);
        localHttpServer.unregister(TEST_SERVLET);

        server.start();
        Assert.assertTrue(server.isStarted());
        Assert.assertTrue(server.isRunning());
        server.stop();
        Assert.assertFalse(server.isStarted());
        Assert.assertFalse(server.isRunning());
    }
}
