/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.bgpapp;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.applications.util.ModulesConfig;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.bgp.config.BgpConfigUtils;
import io.lighty.modules.bgp.deployer.BgpModule;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final int APP_FAILED_TO_START_SC = 500;

    private LightyController controller;
    private CommunityRestConf restconf;
    private LightyModule bgpModule;
    private ModulesConfig modulesConfig = ModulesConfig.getDefaultModulesConfig();
    private boolean running = false;


    public static void main(String[] args) {
        final Main instance = new Main();
        try {
            LOG.info("Registering shutdown hook for graceful shutdown");
            Runtime.getRuntime().addShutdownHook(new Thread(instance::stop));
            instance.start(args);
        } catch (IllegalStateException | ConfigurationException | ExecutionException | TimeoutException
                | IOException e) {
            LOG.error("Failed to start lighty BGP application, exiting", e);
            Runtime.getRuntime().exit(APP_FAILED_TO_START_SC);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while starting lighty BGP application", e);
            Thread.currentThread().interrupt();
        }
    }

    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    public synchronized void start(String[] args) throws InterruptedException, ExecutionException, TimeoutException,
            ConfigurationException, IOException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.info(".__  .__       .__     __              .__           ");
        LOG.info("|  | |__| ____ |  |___/  |_ ___.__.    |__| ____     ");
        LOG.info("|  | |  |/ ___\\|  |  \\   __<   |  |    |  |/  _ \\ ");
        LOG.info("|  |_|  / /_/  >   Y  \\  |  \\___  |    |  (  <_> ) ");
        LOG.info("|____/__\\___  /|___|  /__|  / ____| /\\ |__|\\____/ ");
        LOG.info("        /_____/     \\/      \\/      \\/            ");
        LOG.info("Starting BGP lighty.io application...");

        final ControllerConfiguration controllerConfiguration;
        final RestConfConfiguration restConfConfiguration;
        final Optional<MainArgs> mArgs = MainArgs.parse(args);

        final Set<YangModuleInfo> minimalModelSet =
                Sets.newHashSet(Iterables.concat(ControllerConfigUtils.YANG_MODELS, BgpConfigUtils.ALL_BGP_MODELS,
                        RestConfConfigUtils.YANG_MODELS));

        if (mArgs.isPresent()) {
            final Path configPath = Paths.get(mArgs.get().getConfigPath());
            LOG.info("Using configuration from file {}", configPath);
            controllerConfiguration = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));

            // Inject minimal required model set, in cases when user does not list them all in the config
            final ControllerConfiguration.SchemaServiceConfig schemaServiceConfig = controllerConfiguration
                    .getSchemaServiceConfig();
            schemaServiceConfig.setModels(Sets.newHashSet(
                    Iterables.concat(schemaServiceConfig.getModels(), minimalModelSet)));

            restConfConfiguration = RestConfConfigUtils.getRestConfConfiguration(Files.newInputStream(configPath));
            modulesConfig = ModulesConfig.getModulesConfig(Files.newInputStream(configPath));
        } else {
            LOG.info("Using default configuration");
            controllerConfiguration = ControllerConfigUtils.getDefaultSingleNodeConfiguration(minimalModelSet);
            restConfConfiguration = RestConfConfigUtils.getDefaultRestConfConfiguration();
        }

        controller = new LightyControllerBuilder()
                .from(controllerConfiguration)
                .build();
        Preconditions.checkState(startLightyModule(controller, modulesConfig.getModuleTimeoutSeconds()),
                "Unable to start controller");

        restconf = CommunityRestConfBuilder
                .from(RestConfConfigUtils.getRestConfConfiguration(restConfConfiguration, controller.getServices()))
                .build();
        Preconditions.checkState(startLightyModule(restconf,  modulesConfig.getModuleTimeoutSeconds()),
                "Unable to start restconf module");
        restconf.startServer();

        bgpModule = new BgpModule(controller.getServices());
        Preconditions.checkState(startLightyModule(bgpModule,  modulesConfig.getModuleTimeoutSeconds()),
                "Unable to start BGP module");

        running = true;
        LOG.info("BGP lighty.io application started in {}", stopwatch.stop());
    }

    public synchronized void stop() {
        LOG.info("Shutting down BGP application ...");
        if (restconf != null) {
            restconf.shutdown(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
        }
        if (bgpModule != null) {
            bgpModule.shutdown(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
        }
        if (controller != null) {
            controller.shutdown(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
        }

        running = false;
    }

    private static boolean startLightyModule(final LightyModule lightyModule, final long timeoutSeconds)
            throws InterruptedException, ExecutionException, TimeoutException {
        LOG.info("Starting lighty module {}", lightyModule.getClass().getName());
        return lightyModule.start().get(timeoutSeconds, TimeUnit.SECONDS);
    }

    public synchronized boolean isRunning() {
        return running;
    }

}
