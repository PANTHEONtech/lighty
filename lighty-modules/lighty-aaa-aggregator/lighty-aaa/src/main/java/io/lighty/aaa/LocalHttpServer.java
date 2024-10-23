/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa;

import io.lighty.server.LightyServerBuilder;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Servlet;
import org.opendaylight.aaa.web.ServletDetails;
import org.opendaylight.aaa.web.WebContext;

final class LocalHttpServer {
    private final LightyServerBuilder server;
    private final Map<String, WebContext> handlers;

    LocalHttpServer(final LightyServerBuilder server) {
        this.server = server;
        this.handlers = new HashMap<>();
    }

    @SuppressWarnings("rawtypes")
    public void registerServlet(final String alias, final Servlet servlet, final Dictionary<String, String> initParam) {
        WebContext webContext = WebContext.builder()
            .name("name")
            .contextPath(alias)
            .supportsSessions(true)
            .putContextParam("exampleKey", "exampleValue")
            .addServlet(ServletDetails.builder().servlet(servlet).addUrlPattern("/*").build())
            .build();

        this.server.addContextHandler(webContext);
        this.handlers.put(alias, webContext);
    }

    public void unregister(final String alias) {
    }
}
