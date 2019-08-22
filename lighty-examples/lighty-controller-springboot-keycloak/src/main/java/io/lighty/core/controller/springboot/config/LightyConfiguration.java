/*
 * Copyright (c) 2019 Pantheon Tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.config;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.core.controller.spring.LightyCoreSpringConfiguration;
import io.lighty.modules.southbound.netconf.impl.NetconfSBPlugin;
import io.lighty.modules.southbound.netconf.impl.NetconfTopologyPluginBuilder;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.$YangModuleInfoImpl;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Configuration
public class LightyConfiguration extends LightyCoreSpringConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(LightyConfiguration.class);

    @Bean
    LightyController initLightyController() throws Exception {
        LOG.info("Building LightyController Core");
        final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        final Set<YangModuleInfo> mavenModelPaths = new HashSet<>();
        mavenModelPaths.addAll(NetconfConfigUtils.NETCONF_TOPOLOGY_MODELS);
        mavenModelPaths.add($YangModuleInfoImpl.getInstance());
        final LightyController lightyController = lightyControllerBuilder
            .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(mavenModelPaths))
            .build();
        LOG.info("Starting LightyController (waiting 10s after start)");
        final ListenableFuture<Boolean> started = lightyController.start();
        started.get();
        LOG.info("LightyController Core started");

        return lightyController;
    }

    @Bean
    NetconfSBPlugin initNetconfSBP(LightyController lightyController)
            throws ExecutionException, InterruptedException, ConfigurationException {
        final NetconfConfiguration netconfSBPConfiguration = NetconfConfigUtils.injectServicesToTopologyConfig(
            NetconfConfigUtils.createDefaultNetconfConfiguration(), lightyController.getServices());
        NetconfTopologyPluginBuilder netconfSBPBuilder = new NetconfTopologyPluginBuilder();
        final NetconfSBPlugin netconfSouthboundPlugin = netconfSBPBuilder
            .from(netconfSBPConfiguration, lightyController.getServices())
            .build();
        netconfSouthboundPlugin.start().get();

        Runtime.getRuntime().addShutdownHook(new LightyModuleShutdownHook(lightyController, netconfSouthboundPlugin));

        return netconfSouthboundPlugin;
    }

    static class LightyModuleShutdownHook extends Thread {

        private static final Logger LOG = LoggerFactory.getLogger(LightyModuleShutdownHook.class);

        private final LightyController lightyController;
        private final NetconfSBPlugin netconfSouthboundPlugin;

        public LightyModuleShutdownHook(LightyController lightyController, NetconfSBPlugin netconfSouthboundPlugin) {
            this.lightyController = lightyController;
            this.netconfSouthboundPlugin = netconfSouthboundPlugin;
        }

        @Override
        public void run() {
            long startTime = System.nanoTime();
            try {
                LOG.info("Lighty module {} shutting down NETCONF ...");
                netconfSouthboundPlugin.shutdown();
            } catch (Exception e) {
                LOG.error("Exception while shutting NETCONF module: {} :", e);
            }
            LOG.info("Lighty module {} shutting down ...");
            try {
                LOG.info("Lighty module {} shutting down LightyController ...");
                lightyController.shutdown();
            } catch (Exception e) {
                LOG.error("Exception while shutting LightyController module: {} :", e);
            }
            float duration = (System.nanoTime() - startTime)/1_000_000f;
            LOG.info("Lighty module {} stopped in {}ms", duration);
        }

    }
}
