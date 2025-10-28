/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.applications.rcgnmi.app;

import com.beust.jcommander.JCommander;
import com.google.common.base.Stopwatch;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.applications.rcgnmi.module.RcGnmiAppConfiguration;
import io.lighty.applications.rcgnmi.module.RcGnmiAppModule;
import io.lighty.applications.rcgnmi.module.RcGnmiAppModuleConfigUtils;
import io.lighty.core.common.models.YangModuleUtils;
import io.lighty.core.controller.impl.config.ConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RCgNMIApp {
    private static final Logger LOG = LoggerFactory.getLogger(RCgNMIApp.class);
    private RcGnmiAppModule rcgnmiLightyModule;

    // Using args is safe as we need only a configuration file location here
    @SuppressWarnings("squid:S4823")
    public static void main(final String[] args) {
        final RCgNMIApp app = new RCgNMIApp();
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
        LOG.info("Starting RESTCONF-gNMI lighty.io application...");

        final RcGnmiAppConfiguration rgnmiModuleConfig;
        // Parse args
        final Arguments arguments = new Arguments();
        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(args);
        try {
            if (arguments.getConfigPath() != null) {
                final Path configPath = Paths.get(arguments.getConfigPath());
                rgnmiModuleConfig = RcGnmiAppModuleConfigUtils.loadConfiguration(configPath);
            } else {
                rgnmiModuleConfig = RcGnmiAppModuleConfigUtils.loadDefaultConfig();
            }
        } catch (ConfigurationException e) {
            LOG.error("Unable to load configuration for RCgNMI lighty.io application!", e);
            return;
        } catch (IOException e) {
            LOG.error("Unable to read configuration file for RCgNMI lighty.io application!", e);
            return;
        }

        // print yang modules loaded into controller
        LOG.info("Loaded YANG modules: {}", YangModuleUtils.generateJSONModelSetConfiguration(rgnmiModuleConfig
                .getControllerConfig().getSchemaServiceConfig().getModels()));
        final ExecutorService executorService = SpecialExecutors.newBoundedCachedThreadPool(10,
                100, "gnmi_executor", Logger.class);
        rcgnmiLightyModule = createRgnmiAppModule(rgnmiModuleConfig, executorService, null);

        // Initialize RcGNMI modules
        if (rcgnmiLightyModule.initModules()) {
            // Register shutdown hook for graceful shutdown
            LOG.info("Registering ShutdownHook to gracefully shutdown application");
            Runtime.getRuntime().addShutdownHook(new Thread(rcgnmiLightyModule::close));
            LOG.info("RCgNMI lighty.io application started in {}", stopwatch.stop());
        } else {
            LOG.error("Failed to initialize RcGNMI app. Closing application.");
            stop();
        }

    }

    public RcGnmiAppModule createRgnmiAppModule(final RcGnmiAppConfiguration rcGnmiAppConfiguration,
                                                final ExecutorService gnmiExecutorService,
                                                @Nullable final CrossSourceStatementReactor customReactor) {
        return new RcGnmiAppModule(rcGnmiAppConfiguration, gnmiExecutorService, customReactor);
    }

    public void stop() {
        if (rcgnmiLightyModule != null) {
            LOG.info("Shutting down RcgNMI application!");
            rcgnmiLightyModule.close();
            LOG.info("RcgNMI application stopped!");
        }
    }
}
