/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.core.controller.spring.LightyCoreSpringConfiguration;
import io.lighty.modules.southbound.netconf.impl.NetconfSBPlugin;
import io.lighty.modules.southbound.netconf.impl.NetconfTopologyPluginBuilder;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.$YangModuleInfoImpl;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LightyConfiguration extends LightyCoreSpringConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(LightyConfiguration.class);

    @Bean
    LightyController initLightyController() throws Exception {
        LOG.info("Building LightyController Core");
        final Set<YangModuleInfo> mavenModelPaths = new HashSet<>();
        mavenModelPaths.addAll(NetconfConfigUtils.NETCONF_TOPOLOGY_MODELS);
        mavenModelPaths.add($YangModuleInfoImpl.getInstance());
        final LightyController lightyController = LightyControllerBuilder
            .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(mavenModelPaths))
            .build();
        LOG.info("Starting LightyController (waiting 10s after start)");
        final ListenableFuture<Boolean> started = lightyController.start();
        started.get();
        LOG.info("LightyController Core started");

        Runtime.getRuntime().addShutdownHook(new LightyModuleShutdownHook(lightyController));

        return lightyController;
    }

    @Bean
    NetconfSBPlugin initNetconfSBP(LightyController lightyController) throws ExecutionException, InterruptedException {
        final NetconfConfiguration netconfSBPConfiguration = NetconfConfigUtils.injectServicesToTopologyConfig(
            NetconfConfigUtils.createDefaultNetconfConfiguration(), lightyController.getServices());
        final NetconfSBPlugin netconfSouthboundPlugin = NetconfTopologyPluginBuilder
                .from(netconfSBPConfiguration, lightyController.getServices())
                .build();
        netconfSouthboundPlugin.start().get();

        Runtime.getRuntime().addShutdownHook(new LightyModuleShutdownHook(netconfSouthboundPlugin));

        return netconfSouthboundPlugin;
    }

    static class LightyModuleShutdownHook extends Thread {

        private static final Logger LOG = LoggerFactory.getLogger(LightyModuleShutdownHook.class);

        private final LightyModule lightyModule;

        public LightyModuleShutdownHook(LightyModule lightyModule) {
            this.lightyModule = lightyModule;
        }

        @Override
        public void run() {
            LOG.info("Lighty module {} shutting down ...", lightyModule);
            long startTime = System.nanoTime();
            try {
                lightyModule.shutdown();
            } catch (Exception e) {
                LOG.error("Exception while shutting lighty module: {} :", lightyModule, e);
            }
            float duration = (System.nanoTime() - startTime)/1_000_000f;
            LOG.info("Lighty module {} stopped in {}ms", lightyModule, duration);
        }

    }
}
