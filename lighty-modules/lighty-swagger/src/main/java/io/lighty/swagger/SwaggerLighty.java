/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.swagger;

import com.google.common.annotations.VisibleForTesting;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.server.LightyServerBuilder;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.opendaylight.netconf.sal.rest.doc.api.ApiDocService;
import org.opendaylight.netconf.sal.rest.doc.impl.AllModulesDocGenerator;
import org.opendaylight.netconf.sal.rest.doc.impl.ApiDocGeneratorDraftO2;
import org.opendaylight.netconf.sal.rest.doc.impl.ApiDocGeneratorRFC8040;
import org.opendaylight.netconf.sal.rest.doc.impl.ApiDocServiceImpl;
import org.opendaylight.netconf.sal.rest.doc.impl.MountPointSwaggerGeneratorDraft02;
import org.opendaylight.netconf.sal.rest.doc.impl.MountPointSwaggerGeneratorRFC8040;
import org.opendaylight.netconf.sal.rest.doc.jaxrs.ApiDocApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Swagger initializer for lighty.io.
 * @author juraj.veverka
 */
public class SwaggerLighty extends AbstractLightyModule {

    private static final Logger LOG = LoggerFactory.getLogger(SwaggerLighty.class);
    private static final String APIDOC_PATH = "/apidoc";
    private static final String TRUE = "true";

    private final RestConfConfiguration restConfConfiguration;
    private final LightyServerBuilder jettyServerBuilder;
    private final LightyServices lightyServices;

    private MountPointSwaggerGeneratorDraft02 mountPointSwaggerGeneratorDraft02;
    private MountPointSwaggerGeneratorRFC8040 mountPointSwaggerGeneratorRFC8040;
    private ApiDocService apiDocService;

    public SwaggerLighty(RestConfConfiguration restConfConfiguration,
                         LightyServerBuilder jettyServerBuilder, LightyServices lightyServices) {
        this.restConfConfiguration = restConfConfiguration;
        this.jettyServerBuilder = jettyServerBuilder;
        this.lightyServices = lightyServices;
    }

    @Override
    protected boolean initProcedure() {
        LOG.info("initializing swagger {}", restConfConfiguration.getJsonRestconfServiceType());

        //replace all slash characters from the beginning of the string
        String basePathString = restConfConfiguration.getRestconfServletContextPath().replaceAll("^/+", "");
        LOG.info("basePath: {}", basePathString);

        this.mountPointSwaggerGeneratorDraft02 = new MountPointSwaggerGeneratorDraft02(
                lightyServices.getDOMSchemaService(), lightyServices.getDOMMountPointService(), basePathString);
        this.mountPointSwaggerGeneratorRFC8040 = new MountPointSwaggerGeneratorRFC8040(
                lightyServices.getDOMSchemaService(), lightyServices.getDOMMountPointService(),basePathString);

        ApiDocGeneratorDraftO2 apiDocGeneratorDraftO2 = new ApiDocGeneratorDraftO2(
                lightyServices.getDOMSchemaService(), basePathString);
        ApiDocGeneratorRFC8040 apiDocGeneratorRFC8040 = new ApiDocGeneratorRFC8040(
                lightyServices.getDOMSchemaService(), basePathString);
        AllModulesDocGenerator allModulesDocGenerator =
                new AllModulesDocGenerator(apiDocGeneratorDraftO2, apiDocGeneratorRFC8040);

        this.apiDocService = new ApiDocServiceImpl(mountPointSwaggerGeneratorDraft02, mountPointSwaggerGeneratorRFC8040,
                apiDocGeneratorDraftO2, apiDocGeneratorRFC8040, allModulesDocGenerator);

        ApiDocApplication apiDocApplication = new ApiDocApplication(apiDocService);

        ServletContainer restServletContainer = new ServletContainer(ResourceConfig.forApplication(apiDocApplication));
        ServletHolder restServletHolder = new ServletHolder(restServletContainer);

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        ServletContextHandler mainHandler =   new ServletContextHandler(contexts, APIDOC_PATH, true, false);
        mainHandler.addServlet(restServletHolder, "/swagger2/apis/*");
        mainHandler.addServlet(restServletHolder, "/openapi3/apis/*");
        mainHandler.addServlet(restServletHolder, "/swagger2/18/apis/*");
        mainHandler.addServlet(restServletHolder, "/openapi3/18/apis/*");

        addStaticResources(mainHandler, "/explorer", "static-content");

        LOG.info("adding context handler ...");
        jettyServerBuilder.addContextHandler(contexts);
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        LOG.info("shutting down swagger ...");
        if (mountPointSwaggerGeneratorDraft02 != null) {
            mountPointSwaggerGeneratorDraft02.close();
        }
        if (mountPointSwaggerGeneratorRFC8040 != null) {
            mountPointSwaggerGeneratorRFC8040.close();
        }
        return true;
    }

    private void addStaticResources(ServletContextHandler mainHandler, String path, String servletName) {
        LOG.info("initializing swagger UI at: http(s)://{hostname:port}{}{}/index.html", APIDOC_PATH, path);
        String externalResource = SwaggerLighty.class.getResource(path).toExternalForm();
        LOG.info("externalResource: {}", externalResource);
        DefaultServlet defaultServlet = new DefaultServlet();
        ServletHolder holderPwd = new ServletHolder(servletName, defaultServlet);
        holderPwd.setInitParameter("resourceBase", externalResource);
        holderPwd.setInitParameter("dirAllowed", TRUE);
        holderPwd.setInitParameter("pathInfoOnly", TRUE);
        mainHandler.addServlet(holderPwd, path + "/*");
    }

    @VisibleForTesting
    ApiDocService getApiDocService() {
        return apiDocService;
    }
}
