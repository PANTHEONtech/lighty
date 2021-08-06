/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.openflow.tests;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.modules.southbound.openflow.impl.OpenflowSouthboundPlugin;
import io.lighty.modules.southbound.openflow.impl.OpenflowSouthboundPluginBuilder;
import io.lighty.modules.southbound.openflow.impl.util.OpenflowConfigUtils;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public abstract class OpenflowSouthboundPluginTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(OpenflowSouthboundPluginTestBase.class);
    public static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000;
    public static final long SLEEP_AFTER_SHUTDOWN_TIMEOUT_MILLIS = 10_000;

    private LightyController lightyController;
    private CommunityRestConf communityRestConf;
    private OpenflowSouthboundPlugin ofplugin;

    @BeforeClass(timeOut = 180_000)
    public void startOpenflowPlugin() throws Exception {
        LOG.info("Building LightyController");
        final Set<YangModuleInfo> models = new HashSet<>();
        models.addAll(RestConfConfigUtils.YANG_MODELS);
        models.addAll(OpenflowConfigUtils.OFP_MODELS);
        final ControllerConfiguration defaultSingleNodeConfiguration
                = ControllerConfigUtils.getDefaultSingleNodeConfiguration(models);
        defaultSingleNodeConfiguration.setModulesConfig("configuration/initial/modules.conf");
        defaultSingleNodeConfiguration.setModuleShardsConfig("configuration/initial/module-shards.conf");
        this.lightyController = new LightyControllerBuilder()
                .from(defaultSingleNodeConfiguration)
                .build();

        LOG.info("Starting LightyController (waiting 10s after start)");
        final ListenableFuture<Boolean> started = this.lightyController.start();
        started.get();
        LOG.info("LightyController started");

        LOG.info("Building CommunityRestConf");
        this.communityRestConf = CommunityRestConfBuilder
                .from(RestConfConfigUtils.getDefaultRestConfConfiguration(this.lightyController.getServices()))
                .build();


        LOG.info("Starting CommunityRestConf (waiting 10s after start)");
        final ListenableFuture<Boolean> restconfStart = this.communityRestConf.start();
        restconfStart.get();
        LOG.info("CommunityRestConf started");

        LOG.info("Starting openflow...");

        this.ofplugin = OpenflowSouthboundPluginBuilder
                .from(OpenflowConfigUtils.getDefaultOfpConfiguration(), this.lightyController.getServices())
                .build();

        final ListenableFuture<Boolean> ofpStarted = this.ofplugin.start();
        ofpStarted.get();
        LOG.info("Openflow started");
    }

    @BeforeMethod
    public void handleTestMethodName(final Method method) {
        final String testName = method.getName();
        LOG.info("Running test {}", testName);
    }

    @AfterMethod
    public void afterTest(final ITestResult result) {
        LOG.info("Test {} completed and resulted in {}, with throwables {}",
                result.getName(), parseTestNGStatus(result.getStatus()), result.getThrowable());
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    @AfterClass
    public void shutdownLighty() {
        if (this.ofplugin != null) {
            LOG.info("Shutting down openflow");
            try {
                this.ofplugin.shutdown().get(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOG.error("Interrupted while shutting down openflow", e);
            } catch (TimeoutException e) {
                LOG.error("Timeout while shutting down openflow", e);
            } catch (ExecutionException e) {
                LOG.error("Execution of openflow shutdown failed", e);
            }
        }
        if (this.communityRestConf != null) {
            LOG.info("Shutting down CommunityRestConf");
            try {
                this.communityRestConf.shutdown().get(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                Thread.sleep(SLEEP_AFTER_SHUTDOWN_TIMEOUT_MILLIS);
            } catch (InterruptedException e) {
                LOG.error("Interrupted while shutting down CommunityRestConf", e);
            } catch (TimeoutException e) {
                LOG.error("Timeout while shutting down CommunityRestConf", e);
            } catch (ExecutionException e) {
                LOG.error("Execution of CommunityRestConf shutdown failed", e);
            }
        }
        if (this.lightyController != null) {
            LOG.info("Shutting down LightyController");
            try {
                this.lightyController.shutdown().get(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                Thread.sleep(SLEEP_AFTER_SHUTDOWN_TIMEOUT_MILLIS);
            } catch (Exception e) {
                LOG.error("Shutdown of LightyController failed", e);
            }
        }
    }

    private String parseTestNGStatus(final int testResultStatus) {
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

    public LightyController getLightyController() {
        return this.lightyController;
    }

    public CommunityRestConf getCommunityRestConf() {
        return this.communityRestConf;
    }

    public OpenflowSouthboundPlugin getOpenflowSouthboundPlugin() {
        return this.ofplugin;
    }
}