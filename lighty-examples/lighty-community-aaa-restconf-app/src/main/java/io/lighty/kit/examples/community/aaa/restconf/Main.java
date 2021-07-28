/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.kit.examples.community.aaa.restconf;

import com.google.common.base.Stopwatch;
import io.lighty.aaa.AAALighty;
import io.lighty.aaa.config.CertificateManagerConfig;
import io.lighty.aaa.config.DatastoreConfigurationConfig;
import io.lighty.aaa.config.ShiroConfigurationConfig;
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
    private static final String PASS = "bar";
    private static final String USER = "foo";
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    private LightyController lightyController;
    private AAALighty aaaLighty;
    private CommunityRestConf restconf;

    public static void main(String[] args) {
        Main app = new Main();
        app.start(args, true);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    public void start(final String[] args, final boolean registerShutdownHook) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            final ControllerConfiguration singleNodeConfiguration;
            final RestConfConfiguration restconfConfiguration;
            if (args.length > 0) {
                final Path configPath = Paths.get(args[0]);
                LOG.info("Lighty and Restconf starting, using configuration from file {} ...", configPath);
                singleNodeConfiguration = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
                restconfConfiguration = RestConfConfigUtils.getRestConfConfiguration(Files.newInputStream(configPath));
            } else {
                LOG.info("Lighty and Restconf starting, using default configuration ...");
                final Set<YangModuleInfo> modelPaths = Stream.concat(RestConfConfigUtils.YANG_MODELS.stream(),
                        AAALighty.YANG_MODELS.stream()).collect(Collectors.toSet());
                singleNodeConfiguration = ControllerConfigUtils.getDefaultSingleNodeConfiguration(modelPaths);
                restconfConfiguration = RestConfConfigUtils.getDefaultRestConfConfiguration();
            }
            //Register shutdown hook for graceful shutdown.
            if (registerShutdownHook) {
                Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            }
            startLighty(singleNodeConfiguration, restconfConfiguration);
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
                             final RestConfConfiguration restconfConfiguration)
        throws ConfigurationException, ExecutionException, InterruptedException, TimeoutException,
               ModuleStartupException {

        //1. initialize and start Lighty controller (MD-SAL, Controller, YangTools, Akka)
        final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        this.lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
        final boolean controllerStartOk = this.lightyController.start().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!controllerStartOk) {
            throw new ModuleStartupException("Lighty.io Controller startup failed!");
        }

        // 2. start Restconf server
        final LightyServerBuilder jettyServerBuilder = new LightyServerBuilder(new InetSocketAddress(
                restconfConfiguration.getInetAddress(), restconfConfiguration.getHttpPort()));
        this.restconf = CommunityRestConfBuilder
                .from(RestConfConfigUtils.getRestConfConfiguration(restconfConfiguration,
                    this.lightyController.getServices()))
                .withLightyServer(jettyServerBuilder)
                .build();
        final boolean restconfStartOk = this.restconf.start().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!restconfStartOk) {
            throw new ModuleStartupException("Community Restconf startup failed!");
        }
        Security.addProvider(new BouncyCastleProvider());

        final DataBroker bindingDataBroker = this.lightyController.getServices().getBindingDataBroker();
        final String moonEndpointPath = "/moon";
        // this is example only real application should not use hardcoded credentials.
        this.aaaLighty = new AAALighty(bindingDataBroker, CertificateManagerConfig.getDefault(bindingDataBroker),
                null, ShiroConfigurationConfig.getDefault(), moonEndpointPath,
                DatastoreConfigurationConfig.getDefault(), USER, PASS, jettyServerBuilder);
        final boolean aaaLightyStartOk = this.aaaLighty.start().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!aaaLightyStartOk) {
            throw new ModuleStartupException("AAA module startup failed!");
        }

        this.restconf.startServer();
    }

    @SuppressWarnings("IllegalCatch")
    private void closeLightyModule(LightyModule module) {
        if (module != null) {
            try {
                module.shutdown().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (final Exception e) {
                LOG.error("Exception while shutting down {} module: ", module.getClass().getSimpleName(), e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
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
