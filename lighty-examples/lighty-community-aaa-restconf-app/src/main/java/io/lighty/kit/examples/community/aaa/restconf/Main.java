/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.kit.examples.community.aaa.restconf;

import com.google.common.base.Stopwatch;
import io.lighty.aaa.AAALighty;
import io.lighty.aaa.config.AAAConfiguration;
import io.lighty.aaa.config.CertificateManagerConfig;
import io.lighty.aaa.util.AAAConfigUtils;
import io.lighty.applications.util.ModulesConfig;
import io.lighty.core.common.exceptions.ModuleStartupException;
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
import io.lighty.server.LightyServerBuilder;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private LightyController lightyController;
    private AAALighty aaaLighty;
    private CommunityRestConf restconf;
    private ModulesConfig modulesConfig = ModulesConfig.getDefaultModulesConfig();

    public static void main(final String[] args) {
        Main app = new Main();
        app.start(args, true);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    public void start(final String[] args, final boolean registerShutdownHook) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            final ControllerConfiguration singleNodeConfiguration;
            final RestConfConfiguration restconfConfiguration;
            final AAAConfiguration aaaConfiguration;
            if (args.length > 0) {
                final Path configPath = Paths.get(args[0]);
                LOG.info("Lighty and Restconf starting, using configuration from file {} ...", configPath);
                singleNodeConfiguration = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
                restconfConfiguration = RestConfConfigUtils.getRestConfConfiguration(Files.newInputStream(configPath));
                aaaConfiguration = AAAConfigUtils.getAAAConfiguration(Files.newInputStream(configPath));
                modulesConfig = ModulesConfig.getModulesConfig(Files.newInputStream(configPath));
            } else {
                LOG.info("Lighty and Restconf starting, using default configuration ...");
                final Set<YangModuleInfo> modelPaths = Stream.concat(RestConfConfigUtils.YANG_MODELS.stream(),
                        AAAConfigUtils.YANG_MODELS.stream()).collect(Collectors.toSet());
                singleNodeConfiguration = ControllerConfigUtils.getDefaultSingleNodeConfiguration(modelPaths);
                restconfConfiguration = RestConfConfigUtils.getDefaultRestConfConfiguration();
                aaaConfiguration = AAAConfigUtils.createDefaultAAAConfiguration();
            }
            //Register shutdown hook for graceful shutdown.
            if (registerShutdownHook) {
                Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            }
            startLighty(singleNodeConfiguration, restconfConfiguration, aaaConfiguration);
            LOG.info("Lighty.io, Restconf and AAA module started in {}", stopwatch.stop());
        } catch (final Throwable cause) {
            LOG.error("Lighty.io, Restconf and AAA module main application exception: ", cause);
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            shutdown();
        }
    }

    private void startLighty(final ControllerConfiguration controllerConfiguration,
            final RestConfConfiguration restconfConfiguration, final AAAConfiguration aaaConfiguration)
        throws ConfigurationException, ExecutionException, InterruptedException, TimeoutException,
               ModuleStartupException {

        //1. Initialize and start Lighty controller (MD-SAL, Controller, YangTools, Akka)
        final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        this.lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
        final boolean controllerStartOk = this.lightyController.start()
                .get(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
        if (!controllerStartOk) {
            throw new ModuleStartupException("Lighty.io Controller startup failed!");
        }

        // 2. Initialize and start Restconf server
        final LightyServerBuilder jettyServerBuilder = new LightyServerBuilder(new InetSocketAddress(
                restconfConfiguration.getInetAddress(), restconfConfiguration.getHttpPort()));
        this.restconf = CommunityRestConfBuilder
                .from(RestConfConfigUtils.getRestConfConfiguration(restconfConfiguration,
                    this.lightyController.getServices()))
                .withLightyServer(jettyServerBuilder)
                .build();
        final boolean restconfStartOk = this.restconf.start()
                .get(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
        if (!restconfStartOk) {
            throw new ModuleStartupException("Community Restconf startup failed!");
        }

        // 3. Initialize and start Lighty AAA
        final DataBroker bindingDataBroker = this.lightyController.getServices().getBindingDataBroker();
        Security.addProvider(new BouncyCastleProvider());
        aaaConfiguration.setCertificateManager(
                CertificateManagerConfig.getDefault(bindingDataBroker, lightyController.getServices()
                        .getRpcProviderService()));
        this.aaaLighty = new AAALighty(bindingDataBroker,null, jettyServerBuilder, aaaConfiguration);
        final boolean aaaLightyStartOk = this.aaaLighty.start().get(modulesConfig.getModuleTimeoutSeconds(),
                TimeUnit.SECONDS);
        if (!aaaLightyStartOk) {
            throw new ModuleStartupException("AAA module startup failed!");
        }

        // 4. Start Lighty jetty server
        this.restconf.startServer();
    }

    private void closeLightyModule(final LightyModule module) {
        if (module != null) {
            module.shutdown(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
        }
    }

    public void shutdown() {
        LOG.info("Lighty.io, Restconf and AAA module shutting down ...");
        final Stopwatch stopwatch = Stopwatch.createStarted();
        closeLightyModule(this.aaaLighty);
        closeLightyModule(this.restconf);
        closeLightyModule(this.lightyController);
        LOG.info("Lighty.io, Restconf and AAA module stopped in {}", stopwatch.stop());
    }
}
