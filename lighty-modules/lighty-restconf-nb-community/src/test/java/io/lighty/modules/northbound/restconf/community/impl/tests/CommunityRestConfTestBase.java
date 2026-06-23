/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.restconf.community.impl.tests;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * author: vincent on 15.8.2017.
 */
abstract class CommunityRestConfTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(CommunityRestConfTestBase.class);
    public static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000;

    private static LightyController lightyController;
    private static CommunityRestConf communityRestConf;

    @BeforeAll
    static void startControllerAndRestConf() throws Exception {

        final Set<YangModuleInfo> moduleInfos = new java.util.HashSet<>(RestConfConfigUtils.YANG_MODELS);
        moduleInfos.add(org.opendaylight.yang.svc.v1.instance.identifier.patch.module.rev151121
            .YangModuleInfoImpl.INSTANCE);

        LOG.info("Building LightyController");
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        lightyController = lightyControllerBuilder.from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(
                moduleInfos)).build();

        LOG.info("Starting LightyController (waiting 10s after start)");
        ListenableFuture<Boolean> started = lightyController.start();
        started.get();
        LOG.info("LightyController started");

        LOG.info("Building CommunityRestConf");
        CommunityRestConfBuilder builder = CommunityRestConfBuilder.from(
                RestConfConfigUtils.getDefaultRestConfConfiguration(lightyController.getServices()));
        communityRestConf = builder.build();

        LOG.info("Starting CommunityRestConf (waiting 10s after start)");
        communityRestConf.start().get(10_000, TimeUnit.MILLISECONDS);
        LOG.info("CommunityRestConf started");
    }

    @BeforeEach
    void handleTestMethodName(final TestInfo testInfo) {
        String testName = testInfo.getTestMethod()
            .map(Method::getName)
            .orElse("unknown");

        LOG.info("Running test {}", testName);
    }

    @AfterEach
    void afterTest(final TestInfo testInfo) {
        String testName = testInfo.getTestMethod()
            .map(Method::getName)
            .orElse("unknown");

        LOG.info("Test {} completed", testName);
    }

    @AfterAll
    static void shutdownLighty() {
        if (communityRestConf != null) {
            communityRestConf.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }

        if (lightyController != null) {
            lightyController.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    LightyController getLightyController() {
        return lightyController;
    }

    CommunityRestConf getCommunityRestConf() {
        return communityRestConf;
    }
}
