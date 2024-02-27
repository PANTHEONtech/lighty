/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.openapi;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.annotations.VisibleForTesting;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.server.LightyServerBuilder;
import java.util.Set;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.core.Application;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.opendaylight.restconf.nb.rfc8040.streams.RestconfStreamServletFactory;
import org.opendaylight.restconf.openapi.api.OpenApiService;
import org.opendaylight.restconf.openapi.impl.OpenApiServiceImpl;
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
    private final LightyServerBuilder jettyServerBuilder;
    private final LightyServices lightyServices;

    private OpenApiService apiDocService;

    public OpenApiLighty(RestConfConfiguration restConfConfiguration,
                         LightyServerBuilder jettyServerBuilder, LightyServices lightyServices) {
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

        this.apiDocService = new OpenApiServiceImpl(lightyServices.getDOMSchemaService(),
                lightyServices.getDOMMountPointService(), new RestconfStreamServletFactory() {
                    @Override
                    public @NonNull String restconf() {
                        return basePathString;
                    }

                    @Override
                    public @NonNull HttpServlet newStreamServlet() {
                        return new DefaultServlet();
                    }
                });

        final ServletContainer restServletContainer = new ServletContainer(ResourceConfig
                .forApplication((new Application() {
                    @Override
                    public Set<Object> getSingletons() {
                        return Set.of(apiDocService, new JacksonJaxbJsonProvider());
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
    OpenApiService getApiDocService() {
        return apiDocService;
    }
}
