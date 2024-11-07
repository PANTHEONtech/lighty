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
import javax.ws.rs.core.Application;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.opendaylight.aaa.web.ResourceDetails;
import org.opendaylight.aaa.web.ServletDetails;
import org.opendaylight.aaa.web.WebContext;
import org.opendaylight.aaa.web.servlet.jersey2.JerseyServletSupport;
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
                lightyServices.getDOMMountPointService(), lightyServices.getJaxRsEndpoint());

        final var webContextBuilder = WebContext.builder()
            .name("OpenAPI")
            .contextPath("/openapi")
            .supportsSessions(true)
            .addServlet(ServletDetails.builder()
                .servlet(new JerseyServletSupport().createHttpServletBuilder(new Application() {
                    @Override
                    public Set<Object> getSingletons() {
                        return Set.of(apiDocService, new JacksonJaxbJsonProvider());
                    }
                }).build())
                .addUrlPattern("/api/v3/*")
                .build())
            .addResource(ResourceDetails.builder().name("/explorer").build());

        LOG.info("Adding web context handler...");
        WebContext staticResourceContext = addStaticResources("/explorer", "static-content");
        jettyServerBuilder.addContextHandler(staticResourceContext);
        jettyServerBuilder.addContextHandler(webContextBuilder.build());
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        LOG.info("shutting down openapi ...");
        return true;
    }

    private WebContext addStaticResources(String path, String servletName) {
        LOG.info("initializing openapi UI at: http(s)://{hostname:port}{}{}/index.html", OPENAPI_PATH, path);
        String externalResource = OpenApiLighty.class.getResource(path).toExternalForm();
        LOG.info("externalResource: {}", externalResource);

        ServletDetails servletDetails = ServletDetails.builder()
            .servlet(new DefaultServlet())
            .name(servletName)
            .addUrlPattern(path + "/*")
            .putInitParam("resourceBase", externalResource)
            .putInitParam("dirAllowed", "true")
            .putInitParam("pathInfoOnly", "true")
            .build();

        return WebContext.builder()
            .name(servletName)
            .contextPath(OPENAPI_PATH)
            .addServlet(servletDetails)
            .build();
    }

    @VisibleForTesting
    OpenApiService getApiDocService() {
        return apiDocService;
    }
}
