/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server.test;

import io.lighty.server.LightyServerBuilder;
import java.net.InetSocketAddress;
import java.util.EventListener;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LightyServerBuilderTest {

    @Test
    public void testServerBuilder() {

        FilterHolder filterHolder = new FilterHolder();
        ContextHandlerCollection contexts = new ContextHandlerCollection();

        LightyServerBuilder serverBuilder = new LightyServerBuilder(new InetSocketAddress(8080));
        serverBuilder.addCommonEventListener(new EventListener(){});
        serverBuilder.addCommonFilter(filterHolder, "/path");
        serverBuilder.addCommonInitParameter("key", "value");
        serverBuilder.addContextHandler(contexts);
        Server server = serverBuilder.build();
        Assert.assertNotNull(server);
    }
}
