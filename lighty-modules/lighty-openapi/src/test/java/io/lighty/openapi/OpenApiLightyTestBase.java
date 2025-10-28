/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.openapi;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.server.LightyJettyServerProvider;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.opendaylight.restconf.openapi.jaxrs.JaxRsOpenApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for lighty-openApi tests handlin starting and shutting-down of lighty with restConf and openApi module.
 */
public abstract class OpenApiLightyTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(OpenApiLightyTestBase.class);
    public static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000;

    private LightyController lightyController;
    private OpenApiLighty openApiModule;
    private CommunityRestConf communityRestConf;
    private JaxRsOpenApi jaxRsOpenApi;

    @BeforeClass(timeOut = 60_000)
    public void startControllerAndRestConf() throws Exception {
        LOG.info("Building LightyController");
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        lightyController = lightyControllerBuilder.from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(
                RestConfConfigUtils.YANG_MODELS)).build();

        LOG.info("Starting LightyController (waiting 10s after start)");
        ListenableFuture<Boolean> started = lightyController.start();
        started.get();
        LOG.info("LightyController started");

        final RestConfConfiguration restConfConfiguration = RestConfConfigUtils
                .getDefaultRestConfConfiguration(lightyController.getServices());
        communityRestConf = CommunityRestConfBuilder.from(restConfConfiguration).build();
        communityRestConf.start().get();
        lightyController.getServices().withJaxRsEndpoint(communityRestConf.getJaxRsEndpoint());


        final LightyJettyServerProvider jettyServerBuilder = new LightyJettyServerProvider(new InetSocketAddress(
                restConfConfiguration.getInetAddress(), restConfConfiguration.getHttpPort()));

        openApiModule = new OpenApiLighty(restConfConfiguration, jettyServerBuilder,
                lightyController.getServices(), null);
        LOG.info("Starting Lighty OpenApi");
        openApiModule.start().get();
        communityRestConf.startServer();
        LOG.info("Lighty OpenApi started");
        jaxRsOpenApi = new JaxRsOpenApi(openApiModule.getjaxRsOpenApi());
    }

    @BeforeMethod
    public void handleTestMethodName(Method method) {
        String testName = method.getName();
        LOG.info("Running test {}", testName);
    }

    @AfterMethod
    public void afterTest(ITestResult result) {
        LOG.info("Test {} completed and resulted in {}, with throwables {}",
                result.getName(), parseTestNGStatus(result.getStatus()), result.getThrowable());
    }

    @AfterClass
    public void shutdownLighty() {
        if (openApiModule != null) {
            openApiModule.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
        if (lightyController != null) {
            lightyController.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    private String parseTestNGStatus(int testResultStatus) {
        switch (testResultStatus) {
            case -1:
                return "CREATED";
            case 1:
                return "SUCCESS";
            case 2:
                return "FAILURE";
            case 3:
                return "SKIP";
            case 4:
                return "SUCCESS_PERCENTAGE_FAILURE";
            case 16:
                return "STARTED";
            default:
                return "N/A";
        }
    }

    LightyController getLightyController() {
        return lightyController;
    }

    JaxRsOpenApi getJaxRsOpenapi() {
        return jaxRsOpenApi;
    }
}
