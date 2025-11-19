/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.guice;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FluentFuture;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.guice.LightyControllerModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.examples.controllers.guice.modules.ApplicationModule;
import io.lighty.examples.controllers.guice.service.DataStoreService;
import io.lighty.examples.controllers.guice.service.DataStoreServiceImpl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    private LightyController controller;

    public static void main(final String[] args) {
        final Main app = new Main();
        app.start(args, true);
    }

    @SuppressWarnings("IllegalCatch")
    public void start(final String[] args, final boolean registerShutdownHook) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            final ControllerConfiguration singleNodeConfiguration;
            if (args.length > 0) {
                final Path configPath = Paths.get(args[0]);
                LOG.info("Lighty starting, using configuration file {} ...", configPath);
                singleNodeConfiguration = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
            } else {
                LOG.info("Lighty starting, using default configuration ...");
                singleNodeConfiguration =
                        ControllerConfigUtils.getDefaultSingleNodeConfiguration(ControllerConfigUtils.YANG_MODELS);
            }
            //Register shutdown hook for graceful shutdown.
            if (registerShutdownHook) {
                Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            }
            startLighty(singleNodeConfiguration);
            LOG.info("Lighty started in {}", stopwatch.stop());
        } catch (final IOException e) {
            LOG.error("Lighty Guice application - error reading config file: ", e);
            shutdown();
        } catch (final Exception e) {
            LOG.error("Lighty Guice application exception: ", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            shutdown();
        }
    }

    private void startLighty(final ControllerConfiguration controllerConfiguration)
            throws ConfigurationException, InterruptedException, ExecutionException, TimeoutException {
        //1. Initialize and start Lighty controller (MD-SAL, Controller, YangTools, Pekko)
        final LightyControllerBuilder controllerBuilder = new LightyControllerBuilder();
        controller = controllerBuilder.from(controllerConfiguration).build();
        controller.start().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        //2. Initialize Lighty Controller Module custom application beans
        final LightyControllerModule controllerModule = new LightyControllerModule(controller.getServices());
        final ApplicationModule applicationModule = new ApplicationModule();
        final Injector injector = Guice.createInjector(controllerModule, applicationModule);
        final DataStoreService service = injector.getInstance(DataStoreServiceImpl.class);

        //3. Write initial configuration
        writeInitData(service).get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    FluentFuture<? extends CommitInfo> writeInitData(final DataStoreService service) {
        final TopologyId topologyId = new TopologyId("InitialTopology");
        final NetworkTopology networkTopology = new NetworkTopologyBuilder()
                .setTopology(Collections.singletonMap(new TopologyKey(topologyId), new TopologyBuilder()
                        .setTopologyId(topologyId)
                        .build()))
                .build();
        return service.writeData(DataObjectIdentifier.builder(NetworkTopology.class).build(), networkTopology);
    }

    private void closeLightyModule(final LightyModule module) {
        if (module != null) {
            module.shutdown(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    public void shutdown() {
        LOG.info("Lighty shutting down ...");
        final Stopwatch stopwatch = Stopwatch.createStarted();
        closeLightyModule(controller);
        LOG.info("Lighty stopped in {}", stopwatch.stop());
    }
}
