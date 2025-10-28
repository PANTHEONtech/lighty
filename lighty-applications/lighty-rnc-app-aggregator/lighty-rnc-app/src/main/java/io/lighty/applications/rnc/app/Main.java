/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.app;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Stopwatch;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.applications.rnc.module.RncLightyModule;
import io.lighty.applications.rnc.module.config.RncLightyModuleConfigUtils;
import io.lighty.applications.rnc.module.config.RncLightyModuleConfiguration;
import io.lighty.core.common.models.YangModuleUtils;
import io.lighty.core.controller.impl.config.ConfigurationException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    // Using args is safe as we need only a configuration file location here
    @SuppressWarnings("squid:S4823")
    public static void main(final String[] args) {
        Main app = new Main();
        app.start(args);
    }

    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    public void start(final String[] args) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
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

        final RncLightyModule rncLightyModule = createRncLightyModule(rncModuleConfig);
        // Initialize RNC modules
        if (rncLightyModule.initModules()) {
            LOG.info("Registering ShutdownHook to gracefully shutdown application");
            Runtime.getRuntime().addShutdownHook(new Thread(rncLightyModule::close));
            LOG.info("RNC lighty.io application started in {}", stopwatch.stop());
        } else {
            LOG.error("Failed to initialize RNC app. Closing application.");
            rncLightyModule.close();
        }
    }

    public RncLightyModule createRncLightyModule(final RncLightyModuleConfiguration rncModuleConfig) {
        return new RncLightyModule(rncModuleConfig);
    }
}
