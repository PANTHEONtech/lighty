/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.controllers.nettyrestconfapp;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Stopwatch;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.aaa.config.AAAConfiguration;
import io.lighty.aaa.config.CertificateManagerConfig;
import io.lighty.aaa.util.AAAConfigUtils;
import io.lighty.applications.util.ModulesConfig;
import io.lighty.core.common.exceptions.ModuleStartupException;
import io.lighty.core.common.models.YangModuleUtils;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.netty.restconf.community.impl.NettyRestConf;
import io.lighty.modules.northbound.netty.restconf.community.impl.NettyRestConfBuilder;
import io.lighty.modules.northbound.netty.restconf.community.impl.config.NettyRestConfConfiguration;
import io.lighty.modules.northbound.netty.restconf.community.impl.util.NettyRestConfUtils;
import io.lighty.modules.southbound.netconf.impl.NetconfTopologyPluginBuilder;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private LightyController lightyController;
    private NettyRestConf restconf;
    private LightyModule netconfSBPlugin;
    private ModulesConfig modulesConfig = ModulesConfig.getDefaultModulesConfig();

    public static void main(final String[] args) {
        Main app = new Main();
        app.start(args, true);
    }

    public void start() {
        start(new String[] {}, false);
    }

    @SuppressWarnings("IllegalCatch")
    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    public void start(final String[] args, final boolean registerShutdownHook) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.info(".__  .__       .__     __              .__           _________________    _______");
        LOG.info("|  | |__| ____ |  |___/  |_ ___.__.    |__| ____    /   _____/\\______ \\   \\      \\");
        LOG.info("|  | |  |/ ___\\|  |  \\   __<   |  |    |  |/  _ \\   \\_____  \\  |    |  \\  /   |   \\");
        LOG.info("|  |_|  / /_/  >   Y  \\  |  \\___  |    |  (  <_> )  /        \\ |    `   \\/    |    \\");
        LOG.info("|____/__\\___  /|___|  /__|  / ____| /\\ |__|\\____/  /_______  //_______  /\\____|__  /");
        LOG.info("        /_____/     \\/      \\/      \\/                     \\/         \\/         \\/");
        LOG.info("Starting lighty.io RESTCONF-NETCONF example application ...");
        LOG.info("https://lighty.io/");
        LOG.info("https://github.com/PANTHEONtech/lighty");
        try {
            final ControllerConfiguration singleNodeConfiguration;
            final NettyRestConfConfiguration restconfConfiguration;
            final AAAConfiguration aaaConfiguration;
            final NetconfConfiguration netconfSBPConfiguration;
            if (args.length > 0) {
                Path configPath = Path.of(args[0]);
                LOG.info("using configuration from file {} ...", configPath);
                //1. get controller configuration
                singleNodeConfiguration = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
                //2. get RESTCONF NBP configuration
                restconfConfiguration = NettyRestConfUtils.getNettyRestConfConfiguration(
                    Files.newInputStream(configPath));
                //3. get AAA config
                aaaConfiguration = AAAConfigUtils.getAAAConfiguration(Files.newInputStream(configPath));
                if (!aaaConfiguration.isEnableAAA()) {
                    throw new ConfigurationException("Netty application requires AAA to be enabled!");
                }
                //4. Load modules app configuration
                modulesConfig = ModulesConfig.getModulesConfig(Files.newInputStream(configPath));
                //5. Network topology plugin
                netconfSBPConfiguration =
                    NetconfConfigUtils.createNetconfConfiguration(Files.newInputStream(configPath));
            } else {
                LOG.info("using default configuration ...");
                Set<YangModuleInfo> modelPaths = Stream.concat(NettyRestConfUtils.YANG_MODELS.stream(),
                    NetconfConfigUtils.NETCONF_TOPOLOGY_MODELS.stream()).collect(Collectors.toSet());
                modelPaths.add(org.opendaylight.yang.svc.v1.urn.opendaylight.yang.aaa.cert.mdsal.rev160321
                        .YangModuleInfoImpl.getInstance());
                ArrayNode arrayNode = YangModuleUtils
                    .generateJSONModelSetConfiguration(
                        Stream.concat(ControllerConfigUtils.YANG_MODELS.stream(), modelPaths.stream())
                            .collect(Collectors.toSet())
                    );
                //0. print the list of schema context models
                LOG.info("JSON model config snippet: {}", arrayNode.toString());
                //1. get controller configuration
                singleNodeConfiguration = ControllerConfigUtils.getDefaultSingleNodeConfiguration(modelPaths);
                //2. get RESTCONF NBP configuration
                restconfConfiguration = NettyRestConfUtils.getDefaultNettyRestConfConfiguration();
                //3. AAA configuration
                aaaConfiguration = AAAConfigUtils.createDefaultAAAConfiguration();
                //4. NETCONF SBP configuration
                netconfSBPConfiguration = NetconfConfigUtils.createDefaultNetconfConfiguration();
            }
            //Register shutdown hook for graceful shutdown.
            if (registerShutdownHook) {
                Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            }
            startLighty(singleNodeConfiguration, restconfConfiguration, aaaConfiguration, netconfSBPConfiguration);
            LOG.info("lighty.io and RESTCONF-NETCONF started in {}", stopwatch.stop());
        } catch (IOException e) {
            LOG.error("Main RESTCONF-NETCONF application - error reading config file: ", e);
            shutdown();
        } catch (Exception e) {
            LOG.error("Main RESTCONF-NETCONF application exception: ", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            shutdown();
        }
    }

    private void startLighty(final ControllerConfiguration controllerConfiguration,
        final NettyRestConfConfiguration restconfConfiguration,
        final AAAConfiguration aaaConfiguration,
        final NetconfConfiguration netconfSBPConfiguration)
        throws ConfigurationException, ExecutionException, InterruptedException, TimeoutException,
        ModuleStartupException {

        // initialize and start Lighty controller
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        this.lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
        final boolean controllerStartOk = this.lightyController.start()
            .get(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
        if (!controllerStartOk) {
            throw new ModuleStartupException("Lighty.io Controller startup failed!");
        }

        aaaConfiguration.setCertificateManager(
            CertificateManagerConfig.getDefault(lightyController.getServices().getBindingDataBroker(),
                lightyController.getServices().getRpcProviderService()));

        this.restconf = NettyRestConfBuilder
            .from(NettyRestConfUtils.getNettyRestConfConfiguration(restconfConfiguration,
                this.lightyController.getServices()))
            .withWebEnvironment(NettyRestConfUtils.getAaaWebEnvironment(
                lightyController.getServices().getBindingDataBroker(),
                lightyController.getServices().getRpcProviderService(),
                aaaConfiguration))
            .build();

        // start RestConf server
        final boolean restconfStartOk = this.restconf.start()
            .get(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
        if (!restconfStartOk) {
            throw new ModuleStartupException("Community Restconf startup failed!");
        }

        // start NetConf SBP
        this.netconfSBPlugin = NetconfTopologyPluginBuilder
            .from(NetconfConfigUtils.injectServicesToTopologyConfig(
                netconfSBPConfiguration, this.lightyController.getServices()), this.lightyController.getServices())
            .build();
        final boolean netconfSBPStartOk = this.netconfSBPlugin.start()
            .get(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
        if (!netconfSBPStartOk) {
            throw new ModuleStartupException("NetconfSB plugin startup failed!");
        }
    }

    private void closeLightyModule(final LightyModule module) {
        if (module != null) {
            module.shutdown(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
        }
    }

    public void shutdown() {
        LOG.info("Lighty.io and RESTCONF-NETCONF shutting down ...");
        final Stopwatch stopwatch = Stopwatch.createStarted();
        closeLightyModule(this.netconfSBPlugin);
        closeLightyModule(this.restconf);
        closeLightyModule(this.lightyController);
        LOG.info("Lighty.io and RESTCONF-NETCONF stopped in {}", stopwatch.stop());
    }

}
