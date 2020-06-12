/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.swagger;

import com.sun.jersey.spi.container.servlet.ServletContainer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.server.LightyServerBuilder;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwaggerLighty extends AbstractLightyModule {

    private static final Logger LOG = LoggerFactory.getLogger(SwaggerLighty.class);

    private final LightyServerBuilder jettyServerBuilder;
    private final LightyServices lightyServices;

    public SwaggerLighty(LightyServerBuilder jettyServerBuilder, LightyServices lightyServices) {
        this.jettyServerBuilder = jettyServerBuilder;
        this.lightyServices = lightyServices;
    }

    /**
     * This will register swagger servlet and static ui documentation APIs in {@link LightyServerBuilder}.
     * Swagger URL: http(s)://{hostname:port}/apidoc/apis
     * Swagger  UI: http(s)://{hostname:port}/apidoc/explorer/index.html
     *
     * @return true if swagger initialization was successful.
     */
    @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "Refactored in newer releases.")
    @Override
    protected boolean initProcedure() {
        LOG.info("initializing swagger ...");
        DocProvider docProvider =
                new DocProvider(lightyServices.getSchemaService(), lightyServices.getDOMMountPointService());

        LOG.info("initializing swagger doc generator at http(s)://{hostname:port}/apidoc/apis");
        final ServletHolder jaxrs = new ServletHolder(ServletContainer.class);
        jaxrs.setInitParameter("javax.ws.rs.Application", "io.lighty.swagger.jaxrs.ApiDocApplication");
        final ContextHandlerCollection contexts = new ContextHandlerCollection();
        final ServletContextHandler mainHandler =
                new ServletContextHandler(contexts, "/apidoc", true, false);
        mainHandler.addServlet(jaxrs, "/apis/*"); // http://localhost:8888/apidoc/apis

        LOG.info("initializing swagger UI at: http(s)://{hostname:port}/apidoc/explorer/index.html");
        String externalResource = SwaggerLighty.class.getResource("/18/explorer").toExternalForm();
        DefaultServlet defaultServlet = new DefaultServlet();
        ServletHolder holderPwd = new ServletHolder("static-content", defaultServlet);
        holderPwd.setInitParameter("resourceBase", externalResource);
        holderPwd.setInitParameter("dirAllowed", "true");  //these setting resolved showing stuff in browser
        holderPwd.setInitParameter("pathInfoOnly", "true");
        holderPwd.setInitParameter("redirectWelcome", "true");
        mainHandler.addServlet(holderPwd, "/explorer/*");  // http://localhost:8888/apidoc/explorer/index.html

        LOG.info("adding context handler ...");
        jettyServerBuilder.addContextHandler(contexts);
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        LOG.info("shutting down swagger ...");
        return true;
    }

}
