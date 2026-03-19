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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.opendaylight.restconf.openapi.jaxrs.JaxRsOpenApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for lighty-openApi tests handlin starting and shutting-down of lighty with restConf and openApi module.
 */
abstract class OpenApiLightyTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(OpenApiLightyTestBase.class);
    public static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000;

    private static LightyController lightyController;
    private static OpenApiLighty openApiModule;
    private static CommunityRestConf communityRestConf;
    private static JaxRsOpenApi jaxRsOpenApi;

    @RegisterExtension
    final TestWatcher resultLogger = new TestWatcher() {
        @Override
        public void testSuccessful(ExtensionContext context) {
            LOG.info("Test {} completed and resulted in SUCCESS, with throwables null",
                context.getRequiredTestMethod().getName());
        }

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            LOG.info("Test {} completed and resulted in FAILURE, with throwables {}",
                context.getRequiredTestMethod().getName(), cause);
        }

        @Override
        public void testAborted(ExtensionContext context, Throwable cause) {
            LOG.info("Test {} completed and resulted in SKIP/ABORTED, with throwables {}",
                context.getRequiredTestMethod().getName(), cause);
        }

        @Override
        public void testDisabled(ExtensionContext context, Optional<String> reason) {
            LOG.info("Test {} completed and resulted in DISABLED",
                context.getRequiredTestMethod().getName());
        }
    };

    @BeforeAll
    @Timeout(value = 60_000, unit = TimeUnit.MILLISECONDS)
    static void startControllerAndRestConf() throws Exception {
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
        LOG.info("Lighty OpenApi started");
        jaxRsOpenApi = new JaxRsOpenApi(openApiModule.getjaxRsOpenApi());
    }

    @BeforeEach
    void handleTestMethodName(TestInfo testInfo) {
        String testName = testInfo.getTestMethod().map(Method::getName).orElse(testInfo.getDisplayName());
        LOG.info("Running test {}", testName);
    }

    @AfterAll
    static void shutdownLighty() {
        if (openApiModule != null) {
            openApiModule.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
        if (lightyController != null) {
            lightyController.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    LightyController getLightyController() {
        return lightyController;
    }

    JaxRsOpenApi getJaxRsOpenapi() {
        return jaxRsOpenApi;
    }
}