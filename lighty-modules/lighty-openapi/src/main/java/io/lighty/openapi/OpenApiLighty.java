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
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.server.LightyJettyServerProvider;
import java.util.Set;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.opendaylight.aaa.web.ServletDetails;
import org.opendaylight.aaa.web.WebContext;
import org.opendaylight.aaa.web.WebContextSecurer;
import org.opendaylight.aaa.web.servlet.jersey2.JerseyServletSupport;
import org.opendaylight.restconf.openapi.api.OpenApiService;
import org.opendaylight.restconf.openapi.impl.OpenApiServiceImpl;
import org.opendaylight.restconf.openapi.jaxrs.JaxRsOpenApi;
import org.opendaylight.restconf.openapi.jaxrs.OpenApiBodyWriter;
import org.opendaylight.yangtools.concepts.Registration;
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
    private final WebContextSecurer webContextSecurer;
    private Registration registration;
    private OpenApiService openApiService;

    public OpenApiLighty(final RestConfConfiguration restConfConfiguration,
        final LightyJettyServerProvider jettyServerBuilder,
        final LightyServices lightyServices, final @Nullable WebContextSecurer webContextSecurer) {
        this.restConfConfiguration = restConfConfiguration;
        this.jettyServerBuilder = jettyServerBuilder;
        this.lightyServices = lightyServices;
        this.registration = null;
        this.webContextSecurer = (webContextSecurer == null)
            ? new CommunityRestConf.LightyWebContextSecurer() : webContextSecurer;
    }

    @Override
    protected boolean initProcedure() {
        LOG.info("initializing openapi");
        this.openApiService = new OpenApiServiceImpl(lightyServices.getDOMSchemaService(),
            lightyServices.getDOMMountPointService(), lightyServices.getJaxRsEndpoint());

        final var webContextBuilder = WebContext.builder()
            .name("OpenAPI")
            .contextPath(OPENAPI_PATH)
            .supportsSessions(true)
            .addServlet(ServletDetails.builder()
                .servlet(new JerseyServletSupport().createHttpServletBuilder(new Application() {
                    @Override
                    public Set<Object> getSingletons() {
                        return Set.of(new JaxRsOpenApi(openApiService),
                            new OpenApiBodyWriter(new JsonFactoryBuilder().build()));
                    }
                }).build())
                .addUrlPattern("/api/v3/*")
                .build())
            .addServlet(addStaticResources("/explorer", "OpenApiStaticServlet"));

        webContextSecurer.requireAuthentication(webContextBuilder, "/*");

        try {
            registration = jettyServerBuilder.getServer().registerWebContext(webContextBuilder.build());
        } catch (ServletException e) {
            LOG.error("Failed to register OpenApi web context: {}!", jettyServerBuilder.getClass(), e);
            return false;
        }
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        LOG.info("shutting down openapi ...");
        this.registration.close();
        return true;
    }

    private ServletDetails addStaticResources(String path, String servletName) {
        final String externalResource = OpenApiLighty.class.getResource("/explorer").toExternalForm();
        LOG.info("externalResource: {}", externalResource);

        return ServletDetails.builder()
            .servlet(new DefaultServlet())
            .name(servletName)
            .addUrlPattern(path + "/*")
            .putInitParam("resourceBase", externalResource)
            .putInitParam("dirAllowed", TRUE)
            .putInitParam("pathInfoOnly", TRUE)
            .build();
    }

    @VisibleForTesting
    OpenApiService getjaxRsOpenApi() {
        return this.openApiService;
    }
}
