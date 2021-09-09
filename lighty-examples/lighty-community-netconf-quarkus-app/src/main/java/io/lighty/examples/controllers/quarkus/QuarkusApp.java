/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.quarkus;

import com.google.common.base.Stopwatch;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.southbound.netconf.impl.NetconfTopologyPluginBuilder;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import java.util.concurrent.ExecutionException;


@ApplicationScoped
public class QuarkusApp {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusApp.class);

    private LightyController lightyController;
    private LightyModule netconfSouthboundPlugin;

    public void onStart(@Observes StartupEvent event) {
        start();
    }

    public void onStop(@Observes ShutdownEvent event) {
        shutdown();
    }

    public void start() {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.info(".__  .__       .__     __              .__           _________________    _______");
        LOG.info("|  | |__| ____ |  |___/  |_ ___.__.    |__| ____    /   _____/\\______ \\   \\      \\");
        LOG.info("|  | |  |/ ___\\|  |  \\   __<   |  |    |  |/  _ \\   \\_____  \\  |    |  \\  /   |   \\");
        LOG.info("|  |_|  / /_/  >   Y  \\  |  \\___  |    |  (  <_> )  /        \\ |    `   \\/    |    \\");
        LOG.info("|____/__\\___  /|___|  /__|  / ____| /\\ |__|\\____/  /_______  //_______  /\\____|__  /");
        LOG.info("        /_____/     \\/      \\/      \\/                     \\/         \\/         \\/");
        LOG.info("Starting lighty.io quarkus.io-NETCONF example application ...");
        LOG.info("https://lighty.io/");
        LOG.info("https://github.com/PANTHEONtech/lighty");
        try {
            LOG.info("using default configuration ...");
            //1. get controller configuration
            ControllerConfiguration defaultSingleNodeConfiguration =
                   ControllerConfigUtils.getDefaultSingleNodeConfiguration(NetconfConfigUtils.NETCONF_TOPOLOGY_MODELS);
            //2. NETCONF SBP configuration
            NetconfConfiguration netconfSBPConfig = NetconfConfigUtils.createDefaultNetconfConfiguration();
            startLighty(defaultSingleNodeConfiguration, netconfSBPConfig);
            LOG.info("lighty.io and quarkus.io-NETCONF started in {}", stopwatch.stop());
        } catch (Exception e) {
            LOG.error("Main quarkus.io-NETCONF application exception: ", e);
        }
    }

    private void startLighty(ControllerConfiguration controllerConfiguration,
                             NetconfConfiguration netconfSBPConfiguration)
            throws ConfigurationException, ExecutionException, InterruptedException {

        //1. initialize and start Lighty controller (MD-SAL, Controller, YangTools, Akka)
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
        lightyController.start().get();
        LOG.info("lighy-core started");

        //2. start NETCONF SBP
        netconfSBPConfiguration = NetconfConfigUtils.injectServicesToTopologyConfig(
                netconfSBPConfiguration, lightyController.getServices());
        NetconfTopologyPluginBuilder netconfSBPBuilder = new NetconfTopologyPluginBuilder();
        netconfSouthboundPlugin = netconfSBPBuilder
                .from(netconfSBPConfiguration, lightyController.getServices())
                .build();
        netconfSouthboundPlugin.start().get();
        LOG.info("NETCONF started");
    }


    public void shutdown() {
        LOG.info("lighty.io and quarkus.io-NETCONF shutting down ...");
        final Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            netconfSouthboundPlugin.shutdown().get();
        } catch (Exception e) {
            LOG.error("Exception while shutting down NETCONF:", e);
        }
        try {
            lightyController.shutdown().get();
        } catch (Exception e) {
            LOG.error("Exception while shutting down lighty.io controller:", e);
        }
        LOG.info("lighty.io and quarkus.io-NETCONF stopped in {}", stopwatch.stop());
    }

    @Produces
    public DataBroker getDataBroker() {
        return lightyController.getServices().getBindingDataBroker();
    }

    @Produces
    public MountPointService getMountPointService() {
        return lightyController.getServices().getBindingMountPointService();
    }

}
