/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.restconfapp;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.modules.southbound.netconf.impl.NetconfTopologyPluginBuilder;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private ShutdownHook shutdownHook;

    public void start() {
        start(new String[] {}, false);
    }

    public void start(String[] args, boolean registerShutdownHook) {
        long startTime = System.nanoTime();
        LOG.info(".__  .__       .__     __              .__           _________________    _______");
        LOG.info("|  | |__| ____ |  |___/  |_ ___.__.    |__| ____    /   _____/\\______ \\   \\      \\");
        LOG.info("|  | |  |/ ___\\|  |  \\   __<   |  |    |  |/  _ \\   \\_____  \\  |    |  \\  /   |   \\");
        LOG.info("|  |_|  / /_/  >   Y  \\  |  \\___  |    |  (  <_> )  /        \\ |    `   \\/    |    \\");
        LOG.info("|____/__\\___  /|___|  /__|  / ____| /\\ |__|\\____/  /_______  //_______  /\\____|__  /");
        LOG.info("        /_____/     \\/      \\/      \\/                     \\/         \\/         \\/");
        LOG.info("Starting lighty.io RESTCONF-NETCONF example application ...");
        LOG.info("https://lighty.io/");
        LOG.info("https://github.com/PantheonTechnologies/lighty-core");
        try {
            if (args.length > 0) {
                Path configPath = Paths.get(args[0]);
                LOG.info("using configuration from file {} ...", configPath);
                //1. get controller configuration
                ControllerConfiguration singleNodeConfiguration =
                        ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
                //2. get RESTCONF NBP configuration
                RestConfConfiguration restConfConfiguration = RestConfConfigUtils
                        .getRestConfConfiguration(Files.newInputStream(configPath));
                //3. NETCONF SBP configuration
                NetconfConfiguration netconfSBPConfiguration
                        = NetconfConfigUtils.createNetconfConfiguration(Files.newInputStream(configPath));
                startLighty(singleNodeConfiguration, restConfConfiguration, netconfSBPConfiguration, registerShutdownHook);
            } else {
                LOG.info("using default configuration ...");
                Set<YangModuleInfo> modelPaths = Stream.concat(RestConfConfigUtils.YANG_MODELS.stream(),
                        NetconfConfigUtils.NETCONF_TOPOLOGY_MODELS.stream()).collect(Collectors.toSet());
                //1. get controller configuration
                ControllerConfiguration defaultSingleNodeConfiguration =
                        ControllerConfigUtils.getDefaultSingleNodeConfiguration(modelPaths);
                //2. get RESTCONF NBP configuration
                RestConfConfiguration restConfConfig =
                        RestConfConfigUtils.getDefaultRestConfConfiguration();
                //3. NETCONF SBP configuration
                NetconfConfiguration netconfSBPConfig = NetconfConfigUtils.createDefaultNetconfConfiguration();
                startLighty(defaultSingleNodeConfiguration, restConfConfig, netconfSBPConfig, registerShutdownHook);
            }
            float duration = (System.nanoTime() - startTime)/1_000_000f;
            LOG.info("lighty.io and RESTCONF-NETCONF started in {}ms", duration);
        } catch (Exception e) {
            LOG.error("Main RESTCONF-NETCONF application exception: ", e);
        }
    }

    private void startLighty(ControllerConfiguration controllerConfiguration,
                             RestConfConfiguration restConfConfiguration,
                             NetconfConfiguration netconfSBPConfiguration, boolean registerShutdownHook)
            throws ConfigurationException, ExecutionException, InterruptedException {

        //1. initialize and start Lighty controller (MD-SAL, Controller, YangTools, Akka)
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        LightyController lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
        lightyController.start().get();

        //2. start RestConf server
        CommunityRestConfBuilder communityRestConfBuilder = new CommunityRestConfBuilder();
        CommunityRestConf communityRestConf = communityRestConfBuilder.from(RestConfConfigUtils
                .getRestConfConfiguration(restConfConfiguration, lightyController.getServices()))
                .build();
        communityRestConf.start().get();

        //3. start NetConf SBP
        LightyModule netconfSouthboundPlugin;
        netconfSBPConfiguration = NetconfConfigUtils.injectServicesToTopologyConfig(
                netconfSBPConfiguration, lightyController.getServices());
        NetconfTopologyPluginBuilder netconfSBPBuilder = new NetconfTopologyPluginBuilder();
        netconfSouthboundPlugin = netconfSBPBuilder
                .from(netconfSBPConfiguration, lightyController.getServices())
                .build();
        netconfSouthboundPlugin.start().get();

        //4. Register shutdown hook for graceful shutdown.
        shutdownHook = new ShutdownHook(lightyController, communityRestConf, netconfSouthboundPlugin);
        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    public void shutdown() {
        shutdownHook.run();
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.start(args, true);
    }

    private static class ShutdownHook extends Thread {

        private static final Logger LOG = LoggerFactory.getLogger(ShutdownHook.class);
        private final LightyController lightyController;
        private final CommunityRestConf communityRestConf;
        private final LightyModule netconfSouthboundPlugin;

        ShutdownHook(LightyController lightyController, CommunityRestConf communityRestConf,
                LightyModule netconfSouthboundPlugin) {
            this.lightyController = lightyController;
            this.communityRestConf = communityRestConf;
            this.netconfSouthboundPlugin = netconfSouthboundPlugin;
        }

        @Override
        public void run() {
            LOG.info("lighty.io and RESTCONF-NETCONF shutting down ...");
            long startTime = System.nanoTime();
            try {
                communityRestConf.shutdown().get();
            } catch (Exception e) {
                LOG.error("Exception while shutting down RESTCONF:", e);
            }
            try {
                netconfSouthboundPlugin.shutdown().get();
            } catch (Exception e) {
                LOG.error("Exception while shutting down NETCONF:", e);
            }
            try {
                lightyController.shutdown().get();
            } catch (Exception e) {
                LOG.error("Exception while shutting down lighty.io controller:", e);
            }
            float duration = (System.nanoTime() - startTime)/1_000_000f;
            LOG.info("lighty.io and RESTCONF-NETCONF stopped in {}ms", duration);
        }

    }

}
