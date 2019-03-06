package io.lighty.kit.examples.community.restconf.ovsdb;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.southbound.ovsdb.OvsdbSouthboundPlugin;
import io.lighty.modules.southbound.ovsdb.config.OvsdbSouthboundConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static ShutdownHook shutdownHook;

    public static void main(final String[] args) throws Exception {
        final long startTime = System.nanoTime();
        try {
            final ControllerConfiguration controllerConfiguration;
            final RestConfConfiguration restConfConfiguration;
            if (args.length > 0) {
                final Path configPath = Paths.get(args[0]);
                LOG.info("Lighty and OVSDB starting, using configuration from file {} ...", configPath);
                controllerConfiguration = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
                restConfConfiguration = RestConfConfigUtils
                        .getRestConfConfiguration(Files.newInputStream(configPath));
            } else {
                LOG.info("Lighty and OVSDB starting, using default configuration ...");
                final Set<YangModuleInfo> modelPaths
                        = Stream.concat(RestConfConfigUtils.YANG_MODELS.stream(),
                        OvsdbSouthboundConfigUtils.YANG_MODELS.stream()).collect(Collectors.toSet());

                controllerConfiguration = ControllerConfigUtils.getDefaultSingleNodeConfiguration(modelPaths);
                restConfConfiguration = RestConfConfigUtils.getDefaultRestConfConfiguration();
            }

            // start controller at last
            startLighty(
                    controllerConfiguration,
                    restConfConfiguration
            );
            final float duration = (System.nanoTime() - startTime) / 1_000_000f;
            LOG.info("Lighty and OVSDB started in {}ms", duration);
        } catch (final Exception e) {
            LOG.error("Main OVSDB application exception: ", e);
        }
    }

    private static void startLighty(final ControllerConfiguration controllerConfiguration,
                                    final RestConfConfiguration restConfConfiguration)
            throws ConfigurationException, ExecutionException, InterruptedException {

        //1. initialize and start Lighty controller (MD-SAL, Controller, YangTools, Akka)
        final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        final LightyController lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
        lightyController.start().get();

        //2. start Restconf server
        final CommunityRestConfBuilder communityRestConfBuilder = new CommunityRestConfBuilder();
        final CommunityRestConf communityRestConf = communityRestConfBuilder.from(RestConfConfigUtils
                .getRestConfConfiguration(restConfConfiguration, lightyController.getServices()))
                .build();
        communityRestConf.start();

        //3. start OVSDB
        final LightyModule ovsdbSouthboundPlugin = new OvsdbSouthboundPlugin(lightyController.getServices());
        final ListenableFuture<Boolean> ovsdbStarter = ovsdbSouthboundPlugin.start();

        //Set SystemReadyMonitor listeners to active state
        Futures.addCallback(ovsdbStarter, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {
                if (result) {
                    lightyController.getServices().getLightySystemReadyService()
                            .onSystemBootReady();
                } else {
                    LOG.error("OVSDB was unable to start correctly");
                    throw new RuntimeException("Unexcepted result of OVSDB initialization");
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Exception while initializing OVSDB", t);
            }
        }, Executors.newSingleThreadExecutor());


        shutdownHook = new ShutdownHook(lightyController, null, ovsdbSouthboundPlugin);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }


    private static class ShutdownHook extends Thread {

        private static final Logger LOG = LoggerFactory.getLogger(ShutdownHook.class);
        private final LightyController lightyController;
        private final LightyModule restconf;
        private final LightyModule ovsdbSouthboundPlugin;

        ShutdownHook(final LightyController lightyController, final LightyModule restconf,
                     final LightyModule ovsdbSouthboundPlugin) {
            this.lightyController = lightyController;
            this.restconf = restconf;
            this.ovsdbSouthboundPlugin = ovsdbSouthboundPlugin;
        }

        @Override
        public void run() {
            LOG.info("Lighty and OVSDB shutting down ...");
            final long startTime = System.nanoTime();
            try {
                this.ovsdbSouthboundPlugin.shutdown();
            } catch (final Exception e) {
                LOG.error("OVSDB shutdown failed:", e);
            }
            try {
                this.restconf.shutdown();
            } catch (final Exception e) {
                LOG.error("Restconf NBP shutdown failed:", e);
            }
            try {
                this.lightyController.shutdown();
            } catch (final Exception e) {
                LOG.error("Exception while shutting down Lighty controller:", e);
            }
            final float duration = (System.nanoTime() - startTime) / 1_000_000f;
            LOG.info("Lighty and OVSDB stopped in {}ms", duration);
        }
    }
}