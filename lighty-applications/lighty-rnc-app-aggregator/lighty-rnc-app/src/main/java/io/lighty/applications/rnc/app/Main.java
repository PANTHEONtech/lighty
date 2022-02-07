/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.app;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Stopwatch;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.applications.rnc.module.RncLightyModule;
import io.lighty.applications.rnc.module.config.RncLightyModuleConfigUtils;
import io.lighty.applications.rnc.module.config.RncLightyModuleConfiguration;
import io.lighty.applications.rnc.module.exception.RncLightyAppStartException;
import io.lighty.core.common.models.YangModuleUtils;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.impl.config.ConfigurationException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.jmx.HierarchyDynamicMBean;
import org.apache.log4j.spi.LoggerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
    private static final String UNABLE_TO_START_APPLICATION = "Unable to start RNC lighty.io application!";
    private static final String UNABLE_TO_STOP_APPLICATION
            = "Exception was thrown while shutting down RNC lighty.io application!";

    private AbstractLightyModule rncLightyModule;
    private Integer lightyModuleTimeout;

    // Using args is safe as we need only a configuration file location here
    @SuppressWarnings("squid:S4823")
    public static void main(final String[] args) {
        Main app = new Main();
        try {
            app.start(args);
        } catch (RncLightyAppStartException e) {
            LOG.error("Failed to initialize RNC app. Closing application.", e);
            app.stop();
        }
    }

    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    public void start(final String[] args) throws RncLightyAppStartException {
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
        lightyModuleTimeout = arguments.getApplicationTimeout();
        if (arguments.getLoggerPath() != null) {
            LOG.debug("Argument for custom logging settings path is present: {} ", arguments.getLoggerPath());
            PropertyConfigurator.configure(arguments.getLoggerPath());
            LOG.info("Custom logger properties loaded successfully");
        }
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            registerLoggerMBeans(mbs);
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException
                | NotCompliantMBeanException | MalformedObjectNameException e) {
            LOG.warn("Exception while initializing JMX with MBeans classes", e);
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

        rncLightyModule = createRncLightyModule(rncModuleConfig).setRncModuleTimeout(lightyModuleTimeout);
        try {
            Boolean hasStarted = rncLightyModule.start().get(lightyModuleTimeout, DEFAULT_TIME_UNIT);
            if (hasStarted) {
                LOG.info("Registering ShutdownHook to gracefully shutdown application");
                registerShutdownHook(rncLightyModule);
                LOG.info("RNC lighty.io application started in {}", stopwatch.stop());
            } else {
                throw new RncLightyAppStartException(UNABLE_TO_START_APPLICATION);
            }
        } catch (ExecutionException | TimeoutException e) {
            throw new RncLightyAppStartException(UNABLE_TO_START_APPLICATION, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RncLightyAppStartException(UNABLE_TO_START_APPLICATION, e);
        }
    }

    public RncLightyModule createRncLightyModule(final RncLightyModuleConfiguration rncModuleConfig) {
        return new RncLightyModule(rncModuleConfig);
    }

    private void registerShutdownHook(final AbstractLightyModule application) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownModule(application);
        }));
    }

    private void shutdownModule(final AbstractLightyModule module) {
        if (module != null) {
            try {
                LOG.info("ShutdownHook triggered. Shutting down RNC lighty.io application...");
                module.shutdown().get(lightyModuleTimeout, DEFAULT_TIME_UNIT);
                LOG.info("RNC lighty.io application was shut down!");
            } catch (ExecutionException | TimeoutException e) {
                LOG.error(UNABLE_TO_STOP_APPLICATION, e);
            } catch (InterruptedException e) {
                LOG.error(UNABLE_TO_STOP_APPLICATION, e);
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        LOG.info("Shutting down RNC application!");
        shutdownModule(this.rncLightyModule);
        LOG.info("RNC application stopped!");
    }

    /**
     * Registers necessary log4j MBeans in JMX.
     * @param server MBeanServer
     * @throws MalformedObjectNameException wrong formatted ObjectName
     * @throws NotCompliantMBeanException not compliant MBean
     * @throws InstanceAlreadyExistsException if MBean is already registered
     * @throws MBeanRegistrationException if MBean cant be registered
     */
    private void registerLoggerMBeans(final MBeanServer server) throws MalformedObjectNameException,
            NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {

        final HierarchyDynamicMBean hierarchyDynamicMBean = new HierarchyDynamicMBean();
        final ObjectName mbo = new ObjectName("log4j:hierarchy=LoggerHierarchy");
        server.registerMBean(hierarchyDynamicMBean, mbo);

        final org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
        hierarchyDynamicMBean.addLoggerMBean(rootLogger.getName());
        final LoggerRepository loggersRepo = LogManager.getLoggerRepository();
        final Enumeration loggersEnumer = loggersRepo.getCurrentLoggers();
        while (loggersEnumer.hasMoreElements()) {
            final org.apache.log4j.Logger logger = (org.apache.log4j.Logger) loggersEnumer.nextElement();
            hierarchyDynamicMBean.addLoggerMBean(logger.getName());
        }
    }
}
