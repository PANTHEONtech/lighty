/*
 * Copyright (c) 2020 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.cluster;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Stopwatch;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.core.common.exceptions.ModuleStartupException;
import io.lighty.core.common.models.YangModuleUtils;
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
import io.lighty.modules.southbound.netconf.impl.NetconfSBPlugin;
import io.lighty.modules.southbound.netconf.impl.NetconfTopologyPluginBuilder;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import io.lighty.server.LightyServerBuilder;
import io.lighty.swagger.SwaggerLighty;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    private LightyController lightyController;
    private SwaggerLighty swagger;
    private CommunityRestConf restconf;
    private NetconfSBPlugin netconfSBPlugin;

    public static void main(String[] args) {
        Main app = new Main();
        app.start(args, true);
    }

    public void start() {
        start(new String[] {}, false);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    public void start(String[] args, boolean registerShutdownHook) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.info(".__  .__       .__     __              .__           _________________    _______");
        LOG.info("|  | |__| ____ |  |___/  |_ ___.__.    |__| ____    /   _____/\\______ \\   \\      \\");
        LOG.info("|  | |  |/ ___\\|  |  \\   __<   |  |    |  |/  _ \\   \\_____  \\  |    |  \\  /   |   \\");
        LOG.info("|  |_|  / /_/  >   Y  \\  |  \\___  |    |  (  <_> )  /        \\ |    `   \\/    |    \\");
        LOG.info("|____/__\\___  /|___|  /__|  / ____| /\\ |__|\\____/  /_______  //_______  /\\____|__  /");
        LOG.info("        /_____/     \\/      \\/      \\/                     \\/         \\/         \\/");
        LOG.info("Starting lighty.io RESTCONF-NETCONF example application ...");
        LOG.info("https://lighty.io/");
        LOG.info("https://github.com/PANTHEONtech/lighty-core");

        Arguments arguments = new Arguments();
        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(args);

        LOG.info("Cluster member ordinal: {}", arguments.getMemberOrdinal());
        LOG.info("k8s deployment: {}", arguments.getKubernetesDeployment());

        try {
            final ControllerConfiguration controllerConfiguration;
            final RestConfConfiguration restconfConfiguration;
            final NetconfConfiguration netconfSBPConfiguration;
            if (arguments.getConfigPath() != null && !arguments.getConfigPath().isEmpty()) {
                Path configPath = Paths.get(args[0]);
                LOG.info("using configuration from file {} ...", configPath);
                //1. get controller configuration
                controllerConfiguration = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
                //2. get RESTCONF NBP configuration
                restconfConfiguration = RestConfConfigUtils.getRestConfConfiguration(Files.newInputStream(configPath));
                //3. NETCONF SBP configuration
                netconfSBPConfiguration =
                    NetconfConfigUtils.createNetconfConfiguration(Files.newInputStream(configPath));
            } else {
                LOG.info("using default configuration ...");
                Set<YangModuleInfo> modelPaths = Stream.concat(RestConfConfigUtils.YANG_MODELS.stream(),
                        NetconfConfigUtils.NETCONF_TOPOLOGY_MODELS.stream()).collect(Collectors.toSet());
                ArrayNode arrayNode = YangModuleUtils
                        .generateJSONModelSetConfiguration(
                                Stream.concat(ControllerConfigUtils.YANG_MODELS.stream(), modelPaths.stream())
                                        .collect(Collectors.toSet())
                        );
                //0. print the list of schema context models
                LOG.info("JSON model config snippet: {}", arrayNode);
                //1. get controller configuration
                controllerConfiguration = ControllerConfigUtils.getDefaultSingleNodeConfiguration(modelPaths);

                Config akkaConfig = null;
                if (arguments.getKubernetesDeployment()) {
                    LOG.info("Loading k8s akka config.");
                    akkaConfig = createAkkaConfiguration("cluster/akka-node-k8s.conf",
                            "cluster/factory-akka-default.conf", true);
                    controllerConfiguration.getActorSystemConfig()
                            .setAkkaConfigPath("cluster/akka-node-k8s.conf");
                } else {
                    LOG.info("Loading akka config for node {}.", arguments.getMemberOrdinal());
                    akkaConfig = createAkkaConfiguration("cluster/akka-node-0" + arguments.getMemberOrdinal() + ".conf",
                            "cluster/factory-akka-default.conf", false);
                    controllerConfiguration.getActorSystemConfig().setAkkaConfigPath("cluster/akka-node-0"
                            + arguments.getMemberOrdinal() + ".conf");
                }
                akkaConfig = akkaConfig.resolve();
                controllerConfiguration.getActorSystemConfig().setConfig(akkaConfig);

                //2. get RESTCONF NBP configuration
                restconfConfiguration = RestConfConfigUtils.getDefaultRestConfConfiguration();
                restconfConfiguration.setWebSocketPort(restconfConfiguration.getWebSocketPort()
                    + arguments.getMemberOrdinal());
                restconfConfiguration.setHttpPort(restconfConfiguration.getHttpPort() + arguments.getMemberOrdinal());
                restconfConfiguration.setInetAddress(InetAddress.getByName("0.0.0.0"));
                //3. NETCONF SBP configuration
                netconfSBPConfiguration = NetconfConfigUtils.createDefaultNetconfConfiguration();
            }
            //Register shutdown hook for graceful shutdown.
            if (registerShutdownHook) {
                Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            }
            startLighty(controllerConfiguration, restconfConfiguration, netconfSBPConfiguration);
            LOG.info("Lighty.io and RESTCONF-NETCONF started in {}", stopwatch.stop());
        } catch (Throwable cause) {
            LOG.error("Main RESTCONF-NETCONF application exception: ", cause);
            shutdown();
        }
    }

    private void startLighty(final ControllerConfiguration controllerConfiguration,
                             final RestConfConfiguration restconfConfiguration,
                             NetconfConfiguration netconfSBPConfiguration)
        throws ConfigurationException, ExecutionException, InterruptedException, TimeoutException,
               ModuleStartupException {

        //1. initialize and start Lighty controller (MD-SAL, Controller, YangTools, Akka)
        LOG.info("Trying to start LightyController ...");
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        this.lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
        final boolean controllerStartOk = this.lightyController.start().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!controllerStartOk) {
            throw new ModuleStartupException("Lighty.io Controller startup failed!");
        }

        //2. start RestConf server
        LightyServerBuilder jettyServerBuilder = new LightyServerBuilder(new InetSocketAddress(
                restconfConfiguration.getInetAddress(), restconfConfiguration.getHttpPort()));
        this.restconf = CommunityRestConfBuilder.from(RestConfConfigUtils
                .getRestConfConfiguration(restconfConfiguration, this.lightyController.getServices()))
                .withLightyServer(jettyServerBuilder)
                .build();

        //3. start swagger
        this.swagger =
            new SwaggerLighty(restconfConfiguration, jettyServerBuilder, this.lightyController.getServices());
        final boolean swaggerStartOk = this.swagger.start().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!swaggerStartOk) {
            throw new ModuleStartupException("Lighty.io Swagger startup failed!");
        }
        final boolean restconfStartOk = this.restconf.start().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!restconfStartOk) {
            throw new ModuleStartupException("Community Restconf startup failed!");
        }
        this.restconf.startServer();

        //4. start NetConf SBP
        netconfSBPConfiguration = NetconfConfigUtils.injectServicesToTopologyConfig(
                netconfSBPConfiguration, this.lightyController.getServices());
        this.netconfSBPlugin = NetconfTopologyPluginBuilder
                .from(netconfSBPConfiguration, this.lightyController.getServices())
                .build();
        final boolean netconfSBPStartOk = this.netconfSBPlugin.start().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!netconfSBPStartOk) {
            throw new ModuleStartupException("NetconfSB Plugin startup failed!");
        }
    }

    @SuppressWarnings("IllegalCatch")
    private void closeLightyModule(LightyModule module) {
        if (module != null) {
            try {
                module.shutdown().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (final Exception e) {
                LOG.error("Exception while shutting down {} module: ", module.getClass().getSimpleName(), e);
            }
        }
    }

    public void shutdown() {
        LOG.info("lighty.io and RESTCONF-NETCONF shutting down ...");
        final Stopwatch stopwatch = Stopwatch.createStarted();
        closeLightyModule(this.netconfSBPlugin);
        closeLightyModule(this.restconf);
        closeLightyModule(this.swagger);
        closeLightyModule(this.lightyController);
        LOG.info("lighty.io and RESTCONF-NETCONF stopped in {}ms", stopwatch.stop());
    }

    /**
     * In case this is kubernetes deployment replace the member-role from akka-config with randomly generated name
     * in format member-[random].
     *
     * @param akkaConfigPath         - path to the akka-config file
     * @param factoryAkkaConfigPath  - path to tke factory-akka-config file
     * @param generateRandomMemberId - specifies whether to generate random member name or use the one from akka-config
     */
    private static Config createAkkaConfiguration(String akkaConfigPath, String factoryAkkaConfigPath,
            boolean generateRandomMemberId) throws ConfigurationException {
        Config akkaConfig = ControllerConfigUtils.getAkkaConfigFromPath(akkaConfigPath);
        if (generateRandomMemberId) {
            String randomMemberId = "member-" + RandomStringUtils.random(6, true, true);
            LOG.info("Generating new Member role - {}", randomMemberId);
            akkaConfig = akkaConfig.withValue("akka.cluster.roles", ConfigValueFactory.fromIterable(
                    Collections.singleton(randomMemberId)));
        }
        Config factoryAkkaConfig = ControllerConfigUtils.getAkkaConfigFromPath(factoryAkkaConfigPath);
        return akkaConfig.withFallback(factoryAkkaConfig);
    }
}
