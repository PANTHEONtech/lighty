/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.actions;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Stopwatch;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.applications.util.ModulesConfig;
import io.lighty.core.common.exceptions.ModuleStartupException;
import io.lighty.core.common.models.YangModuleUtils;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.examples.controllers.actions.binding.ServerResetRegistrationUtil;
import io.lighty.examples.controllers.actions.dom.DeviceStartRegistrationUtil;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.modules.southbound.netconf.impl.NetconfTopologyPluginBuilder;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import io.lighty.openapi.OpenApiLighty;
import io.lighty.server.LightyServerBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.dom.api.DOMActionImplementation;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.server.Reset;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private LightyController lightyController;
    private OpenApiLighty openApi;
    private CommunityRestConf restconf;
    private LightyModule netconfSBPlugin;
    private ModulesConfig modulesConfig = ModulesConfig.getDefaultModulesConfig();
    private ObjectRegistration<DOMActionImplementation> domActionRegistration;
    private ObjectRegistration<Reset> bindingActionRegistration;

    public static void main(final String[] args) {
        final Main app = new Main();
        app.start(args, true);
    }

    public void start() {
        start(new String[] {}, false);
    }

    @SuppressWarnings("IllegalCatch")
    @SuppressFBWarnings({"SLF4J_SIGN_ONLY_FORMAT", "REC_CATCH_EXCEPTION"})
    public void start(String[] args, boolean registerShutdownHook) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.info(".__  .__       .__     __              .__           _________________    _______");
        LOG.info("|  | |__| ____ |  |___/  |_ ___.__.    |__| ____    /   _____/\\______ \\   \\      \\");
        LOG.info("|  | |  |/ ___\\|  |  \\   __<   |  |    |  |/  _ \\   \\_____  \\  |    |  \\  /   |   \\");
        LOG.info("|  |_|  / /_/  >   Y  \\  |  \\___  |    |  (  <_> )  /        \\ |    `   \\/    |    \\");
        LOG.info("|____/__\\___  /|___|  /__|  / ____| /\\ |__|\\____/  /_______  //_______  /\\____|__  /");
        LOG.info("        /_____/     \\/      \\/      \\/                     \\/         \\/         \\/");
        LOG.info("Starting Lighty.io RESTCONF-ACTIONS example application ...");
        LOG.info("https://lighty.io/");
        LOG.info("https://github.com/PANTHEONtech/lighty");
        try {
            final ControllerConfiguration singleNodeConfiguration;
            final RestConfConfiguration restconfConfiguration;
            final NetconfConfiguration netconfSBPConfiguration;
            if (args.length > 0) {
                final Path configPath = Paths.get(args[0]);
                LOG.info("using configuration from file {} ...", configPath);
                //1. get controller configuration
                singleNodeConfiguration = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
                //2. get RESTCONF NBP configuration
                restconfConfiguration = RestConfConfigUtils.getRestConfConfiguration(Files.newInputStream(configPath));
                //3. NETCONF SBP configuration
                netconfSBPConfiguration =
                    NetconfConfigUtils.createNetconfConfiguration(Files.newInputStream(configPath));
                //4. Load modules app configuration
                modulesConfig = ModulesConfig.getModulesConfig(Files.newInputStream(configPath));
            } else {
                LOG.info("using default configuration ...");
                final Set<YangModuleInfo> modelPaths = new HashSet<>();
                modelPaths.addAll(ControllerConfigUtils.YANG_MODELS);
                modelPaths.addAll(RestConfConfigUtils.YANG_MODELS);
                modelPaths.addAll(NetconfConfigUtils.NETCONF_TOPOLOGY_MODELS);
                modelPaths.add(org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807
                        .YangModuleInfoImpl.getInstance());
                final ArrayNode arrayNode = YangModuleUtils.generateJSONModelSetConfiguration(modelPaths);
                //0. print the list of schema context models
                LOG.info("JSON model config snippet: {}", arrayNode.toString());
                //1. get controller configuration
                singleNodeConfiguration = ControllerConfigUtils.getDefaultSingleNodeConfiguration(modelPaths);
                //2. get RESTCONF NBP configuration
                restconfConfiguration = RestConfConfigUtils.getDefaultRestConfConfiguration();
                //3. NETCONF SBP configuration
                netconfSBPConfiguration = NetconfConfigUtils.createDefaultNetconfConfiguration();
            }
            // Register shutdown hook for graceful shutdown.
            if (registerShutdownHook) {
                Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            }
            startLighty(singleNodeConfiguration, restconfConfiguration, netconfSBPConfiguration);
            LOG.info("Lighty.io and RESTCONF-ACTIONS started in {}", stopwatch.stop());
            // Register example DOM action
            domActionRegistration = DeviceStartRegistrationUtil.registerDOMAction(lightyController);
            LOG.info("Example DOM action implementation registered: {}", domActionRegistration.getInstance());
            // Register example binding action
            bindingActionRegistration = ServerResetRegistrationUtil.registerBindingAction(lightyController);
            LOG.info("Example binding action implementation registered: {}", bindingActionRegistration.getInstance());
        } catch (IOException e) {
            LOG.error("Main RESTCONF-ACTIONS application - error reading config file: ", e);
            shutdown();
        } catch (Exception e) {
            LOG.error("Main RESTCONF-ACTIONS application exception: ", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            shutdown();
        }
    }

    private void startLighty(final ControllerConfiguration controllerConfiguration,
            final RestConfConfiguration restconfConfiguration, NetconfConfiguration netconfSBPConfiguration)
        throws ConfigurationException, ExecutionException, InterruptedException, TimeoutException,
               ModuleStartupException {
        //1. initialize and start Lighty controller (MD-SAL, Controller, YangTools, Akka)
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        this.lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
        final boolean controllerStartOk = this.lightyController.start()
                .get(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
        if (!controllerStartOk) {
            throw new ModuleStartupException("Lighty.io Controller startup failed!");
        }

        //2. build RestConf server
        LightyServerBuilder jettyServerBuilder = new LightyServerBuilder(new InetSocketAddress(
                restconfConfiguration.getInetAddress(), restconfConfiguration.getHttpPort()));
        this.restconf = CommunityRestConfBuilder
                .from(RestConfConfigUtils.getRestConfConfiguration(restconfConfiguration,
                    this.lightyController.getServices()))
                .withLightyServer(jettyServerBuilder)
                .withScheduledThreadPool(lightyController.getServices().getScheduledThreadPool())
                .build();

        //3. start openApi and RestConf server
        this.openApi =
            new OpenApiLighty(restconfConfiguration, jettyServerBuilder, this.lightyController.getServices());
        final boolean openApiStartOk = this.openApi.start()
                .get(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
        if (!openApiStartOk) {
            throw new ModuleStartupException("Lighty.io OpenApi startup failed!");
        }
        final boolean restconfStartOk = this.restconf.start()
                .get(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
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
        LOG.info("Lighty.io and RESTCONF-ACTIONS shutting down ...");
        final Stopwatch stopwatch = Stopwatch.createStarted();
        if (domActionRegistration != null) {
            domActionRegistration.close();
        }
        if (bindingActionRegistration != null) {
            bindingActionRegistration.close();
        }
        closeLightyModule(this.netconfSBPlugin);
        closeLightyModule(this.restconf);
        closeLightyModule(this.openApi);
        closeLightyModule(this.lightyController);
        LOG.info("Lighty.io and RESTCONF-ACTIONS stopped in {}", stopwatch.stop());
    }
}
