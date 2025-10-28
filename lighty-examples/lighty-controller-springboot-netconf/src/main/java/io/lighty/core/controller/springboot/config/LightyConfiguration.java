/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.config;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.core.controller.spring.LightyCoreSpringConfiguration;
import io.lighty.core.controller.spring.LightyLaunchException;
import io.lighty.modules.southbound.netconf.impl.NetconfSBPlugin;
import io.lighty.modules.southbound.netconf.impl.NetconfTopologyPluginBuilder;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yang.svc.v1.http.netconfcentral.org.ns.toaster.rev091120.YangModuleInfoImpl;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LightyConfiguration extends LightyCoreSpringConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(LightyConfiguration.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private NetconfSBPlugin netconfSBPlugin;

    @Override
    protected LightyController initLightyController() throws LightyLaunchException, InterruptedException {
        try {
            LOG.info("Building LightyController Core");
            final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
            final Set<YangModuleInfo> mavenModelPaths = new HashSet<>();
            mavenModelPaths.addAll(NetconfConfigUtils.NETCONF_TOPOLOGY_MODELS);
            mavenModelPaths.add(YangModuleInfoImpl.getInstance());
            final LightyController lightyController = lightyControllerBuilder
                    .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(mavenModelPaths))
                    .build();
            LOG.info("Starting LightyController (waiting 10s after start)");
            final boolean controllerStartOk = lightyController
                .start().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!controllerStartOk) {
                shutdownLightyController(lightyController);
                throw new LightyLaunchException("Could not init LightyController");
            }
            LOG.info("LightyController Core started");

            return lightyController;
        } catch (ConfigurationException | ExecutionException | TimeoutException e) {
            throw new LightyLaunchException("Could not init LightyController", e);
        }
    }

    @Override
    protected void shutdownLightyController(LightyController lightyController) throws LightyLaunchException {
        try {
            LOG.info("Shutting down LightyController ...");
            lightyController.shutdown();
        } catch (Exception e) {
            throw new LightyLaunchException("Could not shutdown LightyController", e);
        }
    }

    @Bean
    NetconfSBPlugin initNetconfSBP(LightyController lightyController)
        throws ConfigurationException, LightyLaunchException {
        final NetconfConfiguration netconfSBPConfiguration = NetconfConfigUtils.injectServicesToTopologyConfig(
            NetconfConfigUtils.createDefaultNetconfConfiguration(), lightyController.getServices());
        this.netconfSBPlugin = NetconfTopologyPluginBuilder
            .from(netconfSBPConfiguration, lightyController.getServices())
            .build();
        boolean netconfSBPStartOk;
        try {
            netconfSBPStartOk = this.netconfSBPlugin.start().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            netconfSBPStartOk = false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            netconfSBPStartOk = false;
        }
        if (!netconfSBPStartOk) {
            shutdown();
            throw new LightyLaunchException("Could not init NetconfSB Plugin");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        return this.netconfSBPlugin;
    }

    void shutdown() {
        closeLightyModule(this.netconfSBPlugin);
        closeLightyModule(lightyController());
    }

    private static void closeLightyModule(final LightyModule lightyModule) {
        if (lightyModule != null) {
            final Stopwatch stopwatch = Stopwatch.createStarted();
            LOG.info("Lighty module {} shutting down ...", lightyModule);
            try {
                final ListenableFuture<Boolean> future = lightyModule.shutdown();
                Futures.addCallback(future, new FutureCallback<>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (result) {
                            LOG.info("Lighty module {} stopped in {}", lightyModule, stopwatch.stop());
                        } else {
                            LOG.info("Lighty module {} failed to stop after {}", lightyModule, stopwatch.stop());
                        }
                    }
                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.error("Exception while shutting module: {} :", lightyModule, throwable);
                    }
                }, MoreExecutors.directExecutor());
            } catch (Exception e) {
                LOG.error("Exception while shutting module: {} :", lightyModule, e);
            }
        }
    }

}
