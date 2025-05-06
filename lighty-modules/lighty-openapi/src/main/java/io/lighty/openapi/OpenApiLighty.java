/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.openapi;

import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.google.common.annotations.VisibleForTesting;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.server.LightyJettyServerProvider;
import java.util.Set;
import javax.ws.rs.core.Application;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.opendaylight.restconf.openapi.impl.OpenApiServiceImpl;
import org.opendaylight.restconf.openapi.jaxrs.JaxRsOpenApi;
import org.opendaylight.restconf.openapi.jaxrs.OpenApiBodyWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenApi initializer for lighty.io.
 * @author juraj.veverka
 */
public class OpenApiLighty extends AbstractLightyModule {

    private static final Logger LOG = LoggerFactory.getLogger(OpenApiLighty.class);
    private static final String OPENAPI_PATH = "/openapi";
    private static final String TRUE = "true";

    private final RestConfConfiguration restConfConfiguration;
    private final LightyJettyServerProvider jettyServerBuilder;
    private final LightyServices lightyServices;

    private JaxRsOpenApi jaxRsOpenApi;

    public OpenApiLighty(RestConfConfiguration restConfConfiguration,
                         LightyJettyServerProvider jettyServerBuilder, LightyServices lightyServices) {
        this.restConfConfiguration = restConfConfiguration;
        this.jettyServerBuilder = jettyServerBuilder;
        this.lightyServices = lightyServices;
    }

    @Override
    protected boolean initProcedure() {
        LOG.info("initializing openapi");

        //replace all slash characters from the beginning of the string
        String basePathString = restConfConfiguration.getRestconfServletContextPath().replaceAll("^/+", "");
        LOG.info("basePath: {}", basePathString);

        final var openApiService = new OpenApiServiceImpl(lightyServices.getDOMSchemaService(),
            lightyServices.getDOMMountPointService(), lightyServices.getJaxRsEndpoint());

        this.jaxRsOpenApi = new JaxRsOpenApi(openApiService);

        final ServletContainer restServletContainer = new ServletContainer(ResourceConfig
                .forApplication((new Application() {
                    @Override
                    public Set<Object> getSingletons() {
                        return Set.of(new JaxRsOpenApi(openApiService),
                            new OpenApiBodyWriter(new JsonFactoryBuilder().build()));
                    }
                })));

        ServletHolder restServletHolder = new ServletHolder(restServletContainer);

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        ServletContextHandler mainHandler = new ServletContextHandler(contexts, OPENAPI_PATH, true, false);
        mainHandler.addServlet(restServletHolder, "/api/v3/*");

        addStaticResources(mainHandler, "/explorer", "static-content");

        LOG.info("adding context handler ...");
        jettyServerBuilder.addContextHandler(contexts);
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        LOG.info("shutting down openapi ...");
        return true;
    }

    private void addStaticResources(ServletContextHandler mainHandler, String path, String servletName) {
        LOG.info("initializing openapi UI at: http(s)://{hostname:port}{}{}/index.html", OPENAPI_PATH, path);
        String externalResource = OpenApiLighty.class.getResource(path).toExternalForm();
        LOG.info("externalResource: {}", externalResource);
        DefaultServlet defaultServlet = new DefaultServlet();
        ServletHolder holderPwd = new ServletHolder(servletName, defaultServlet);
        holderPwd.setInitParameter("resourceBase", externalResource);
        holderPwd.setInitParameter("dirAllowed", TRUE);
        holderPwd.setInitParameter("pathInfoOnly", TRUE);
        mainHandler.addServlet(holderPwd, path + "/*");
    }

    @VisibleForTesting
    JaxRsOpenApi getJaxRsOpenApi() {
        return jaxRsOpenApi;
    }
}
