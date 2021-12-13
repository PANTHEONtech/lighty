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
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.bgp.config.BgpConfigUtils;
import io.lighty.modules.bgp.deployer.BgpModule;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final int APP_FAILED_TO_START_SC = 500;
    private static final long MODULE_STARTUP_WAIT_TIME = 20_000;

    private LightyController controller;
    private LightyModule restconf;
    private LightyModule bgpModule;
    private boolean running = false;


    public static void main(String[] args) {
        final Main instance = new Main();
        try {
            LOG.info("Registering shutdown hook for graceful shutdown");
            Runtime.getRuntime().addShutdownHook(new Thread(instance::stop));
            instance.start(args);
        } catch (IllegalStateException | ConfigurationException | ExecutionException | TimeoutException e) {
            LOG.error("Failed to start lighty BGP application, exiting", e);
            Runtime.getRuntime().exit(APP_FAILED_TO_START_SC);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while starting lighty BGP application", e);
            Thread.currentThread().interrupt();
        }
    }

    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    public synchronized void start(String[] args) throws InterruptedException, ExecutionException, TimeoutException,
            ConfigurationException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.info(".__  .__       .__     __              .__           ");
        LOG.info("|  | |__| ____ |  |___/  |_ ___.__.    |__| ____     ");
        LOG.info("|  | |  |/ ___\\|  |  \\   __<   |  |    |  |/  _ \\ ");
        LOG.info("|  |_|  / /_/  >   Y  \\  |  \\___  |    |  (  <_> ) ");
        LOG.info("|____/__\\___  /|___|  /__|  / ____| /\\ |__|\\____/ ");
        LOG.info("        /_____/     \\/      \\/      \\/            ");
        LOG.info("Starting BGP lighty.io application...");


        final Set<YangModuleInfo> additionalModules =
                Sets.newHashSet(Iterables.concat(BgpConfigUtils.ALL_BGP_MODELS, RestConfConfigUtils.YANG_MODELS));

        controller = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(additionalModules))
                .build();
        Preconditions.checkState(startLightyModule(controller), "Unable to start controller");

        restconf = CommunityRestConfBuilder
                .from(RestConfConfigUtils.getDefaultRestConfConfiguration(controller.getServices()))
                .build();
        Preconditions.checkState(startLightyModule(restconf), "Unable to start restconf module");

        bgpModule = new BgpModule(controller.getServices());
        Preconditions.checkState(startLightyModule(bgpModule), "Unable to start BGP module");

        running = true;
        LOG.info("BGP lighty.io application started in {}", stopwatch.stop());
    }

    @SuppressWarnings({"checkstyle:illegalCatch"})
    public synchronized void stop() {
        LOG.info("Shutting down BGP application ...");
        try {
            if (restconf != null) {
                restconf.shutdown().get();
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted while shutting down RESTCONF:", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("Exception while shutting down RESTCONF:", e);
        }

        try {
            if (bgpModule != null) {
                bgpModule.shutdown().get();
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted while shutting down BGP module:", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("Exception while shutting down BGP module:", e);
        }

        try {
            if (controller != null) {
                controller.shutdown().get();
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted while shutting down controller:", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("Exception while shutting down controller:", e);
        }

        running = false;
    }

    private static boolean startLightyModule(final LightyModule lightyModule) throws InterruptedException,
            ExecutionException, TimeoutException {
        LOG.info("Starting lighty module {}", lightyModule.getClass().getName());
        return lightyModule.start().get(MODULE_STARTUP_WAIT_TIME, TimeUnit.MILLISECONDS);
    }

    public synchronized boolean isRunning() {
        return running;
    }

}
