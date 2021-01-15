/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.rncapp;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.node.ArrayNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.core.common.models.YangModuleUtils;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.rnc.module.RncLightyModule;
import io.lighty.rnc.module.config.RncLightyModuleConfigUtils;
import io.lighty.rnc.module.config.RncLightyModuleConfiguration;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final String UNABLE_TO_START_APPLICATION = "Unable to start lighty.io application!";

    // Using args is safe as we need only a configuration file location here
    @SuppressWarnings("squid:S4823")
    public static void main(String[] args) {
        Main app = new Main();
        app.start(args);
    }

    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    public void start(String[] args) {
        final long startTime = System.nanoTime();
        LOG.info(".__  .__       .__     __              .__           ");
        LOG.info("|  | |__| ____ |  |___/  |_ ___.__.    |__| ____     ");
        LOG.info("|  | |  |/ ___\\|  |  \\   __<   |  |    |  |/  _ \\ ");
        LOG.info("|  |_|  / /_/  >   Y  \\  |  \\___  |    |  (  <_> ) ");
        LOG.info("|____/__\\___  /|___|  /__|  / ____| /\\ |__|\\____/ ");
        LOG.info("        /_____/     \\/      \\/      \\/            ");
        LOG.info("Starting RNC lighty.io application...");

        RncLightyModuleConfiguration rncModuleConfig;
        // Parse args
        Arguments arguments = new Arguments();
        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(args);

        if (arguments.getLoggerPath() != null) {
            LOG.debug("Argument for custom logging settings path is present: {} ", arguments.getLoggerPath());
            PropertyConfigurator.configure(arguments.getLoggerPath());
            LOG.info("Custom logger properties loaded successfully");
        }

        try {
            if (arguments.getConfigPath() != null) {
                Path configPath = Paths.get(arguments.getConfigPath());
                rncModuleConfig = RncLightyModuleConfigUtils.loadConfigFromFile(configPath);
            } else {
                rncModuleConfig = RncLightyModuleConfigUtils.loadDefaultConfig();
            }
        } catch (ConfigurationException e) {
            LOG.error("Unable to load configuration for RNC lighty.io application!", e);
            return;
        }

        // print YANG modules
        ArrayNode arrayNode = YangModuleUtils.generateJSONModelSetConfiguration(
                rncModuleConfig.getControllerConfig().getSchemaServiceConfig().getModels());
        LOG.info("Loaded YANG modules: {}", arrayNode);

        RncLightyModule rncLightyModule = createRncLightyModule(rncModuleConfig);
        try {
            rncLightyModule.start().get();
        } catch (ExecutionException e) {
            LOG.error(UNABLE_TO_START_APPLICATION, e);
            return;
        } catch (InterruptedException e) {
            LOG.error(UNABLE_TO_START_APPLICATION, e);
            Thread.currentThread().interrupt();
        }

        // Register shutdown hook for graceful shutdown
        LOG.info("Registering ShutdownHook to gracefully shutdown application");
        registerShutdownHook(rncLightyModule);
        float duration = (System.nanoTime() - startTime) / 1_000_000f;
        LOG.info("RNC lighty.io application started in {}ms", duration);
    }

    public RncLightyModule createRncLightyModule(RncLightyModuleConfiguration rncModuleConfig) {
        return new RncLightyModule(rncModuleConfig);
    }

    private void registerShutdownHook(AbstractLightyModule application) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                LOG.info("ShutdownHook triggered. Shutting down RNC lighty.io application...");
                application.shutdown().get();
                LOG.info("RNC lighty.io application was shut down!");
            } catch (ExecutionException e) {
                LOG.error(UNABLE_TO_START_APPLICATION, e);
            } catch (InterruptedException e) {
                LOG.error("Unable to shut down RNC lighty.io application! Exception was thrown!", e);
                Thread.currentThread().interrupt();
            }
        }));
    }
}
