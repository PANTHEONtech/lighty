package io.lighty.examples.controllers.restconf.ofpovsdb;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.southbound.openflow.impl.OpenflowSouthboundPlugin;
import io.lighty.modules.southbound.openflow.impl.OpenflowSouthboundPluginBuilder;
import io.lighty.modules.southbound.openflow.impl.config.OpenflowpluginConfiguration;
import io.lighty.modules.southbound.openflow.impl.util.OpenflowConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.modules.southbound.ovsdb.OvsdbSouthboundPlugin;
import io.lighty.modules.southbound.ovsdb.config.OvsdbSouthboundConfigUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
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

    private static final String MODULE_ID_RESTCONF = "RESTCONF";
    private static final String MODULE_ID_OFP = "OFP-SB";
    private static final String MODULE_ID_OVSDB = "OVSDB-SB";
    private static final ControllerModulesState ctrlModulesState =
            new ControllerModulesState(MODULE_ID_RESTCONF, MODULE_ID_OFP, MODULE_ID_OVSDB);

    public static void main(final String[] args) throws Exception {
        final long startTime = System.nanoTime();
        try {
            final ControllerConfiguration controllerConfiguration;
            final RestConfConfiguration restConfConfiguration;
            final OpenflowpluginConfiguration openflowpluginConfiguration;

            if (args.length > 0) {
                final Path configPath = Paths.get(args[0]);
                LOG.info("Lighty OFP and OVSDB starting, using configuration from file {} ...", configPath);
                controllerConfiguration = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
                restConfConfiguration = RestConfConfigUtils
                        .getRestConfConfiguration(Files.newInputStream(configPath));
                openflowpluginConfiguration = OpenflowConfigUtils.getOfpConfiguration(Files.newInputStream(configPath));
            } else {
                LOG.info("Lighty OFP and OVSDB starting, using default configuration ...");
                final Set<YangModuleInfo> modelPaths = Stream.of(
                            RestConfConfigUtils.YANG_MODELS.stream(),
                            OpenflowConfigUtils.OFP_MODELS.stream(),
                            OvsdbSouthboundConfigUtils.YANG_MODELS.stream())
                        .reduce(Stream::concat)
                        .orElseGet(Stream::empty)
                        .collect(Collectors.toSet());
                controllerConfiguration = ControllerConfigUtils.getDefaultSingleNodeConfiguration(modelPaths);
                restConfConfiguration =  RestConfConfigUtils.getDefaultRestConfConfiguration();
                openflowpluginConfiguration = OpenflowConfigUtils.getDefaultOfpConfiguration();
            }

            // start controller at last
            startLighty(
                    controllerConfiguration,
                    restConfConfiguration,
                    openflowpluginConfiguration
            );
            final float duration = (System.nanoTime() - startTime) / 1_000_000f;
            LOG.info("Lighty and OFP started in {}ms", duration);
        } catch (final Exception e) {
            LOG.error("Main OFP application exception: ", e);
        }
    }

    /**
     * Stores state of all modules being started by this SDN controller.
     */
    private static class ControllerModulesState {
        private final Map<String,Boolean> modulesStates = new HashMap<>();

        /**
         * Initializes state of all modules identified by unique IDs.
         * @param modules
         */
        public ControllerModulesState(String ... modules) {
            for (String moduleId : modules) {
                modulesStates.put(moduleId, false);
            }
        }

        /**
         * Checks whether all modules of SDN controller are ready.
         * @return True when all modules are ready, false otherwise
         */
        public synchronized boolean allReady() {
            for (Boolean b : modulesStates.values()) {
                if (!b) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Set the state of the module as ready.
         * @param moduleId Unique ID of the module
         * @return True when all modules are ready after the state change of the module,
         *         false otherwise.
         */
        public synchronized boolean setReady(String moduleId) {
            if (modulesStates.containsKey(moduleId)) {
                LOG.info("Module ready: {}", moduleId);
                modulesStates.put(moduleId, true);
            } else {
                LOG.error("Unknown module: {}", moduleId);
                return false;
            }

            return allReady();
        }
    }

    /**
     * Sets callbacks on results of module start. Checks whether all modules of this
     * SDN controller are ready and calls system ready service.
     * @param lightyController lighty.io based controller instance
     * @param starter Future object returned by start method of module
     * @param moduleId Unique ID of the module which is used to change state of the module
     */
    private static void setSystemReadyMonitor(final LightyController lightyController,
                                              final ListenableFuture<Boolean> starter,
                                              final String moduleId) {
        //Set SystemReadyMonitor listeners to active state
        Futures.addCallback(starter, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    // Set the module as ready and check if the whole controller is ready
                    if (ctrlModulesState.setReady(moduleId)) {
                        lightyController.getServices().getLightySystemReadyService().onSystemBootReady();
                        LOG.info("All controller modules started");
                    }
                } else {
                    LOG.error("{} wasn unable to start correctly", moduleId);
                    throw new RuntimeException("Unexcepted result of " + moduleId + " initialization");
                }
            }
            @Override
            public void onFailure(Throwable t) {
                LOG.error("Exception while initializing {}", moduleId, t);
            }
        }, Executors.newSingleThreadExecutor());
    }

    /**
     * Starts controller base services and all SB and NB modules used by this controller
     */
    private static void startLighty(final ControllerConfiguration controllerConfiguration,
                                    final RestConfConfiguration restConfConfiguration,
                                    final OpenflowpluginConfiguration configuration)
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
        final ListenableFuture<Boolean> restconfStarter = communityRestConf.start();
        setSystemReadyMonitor(lightyController, restconfStarter, MODULE_ID_RESTCONF);

        //3. start OpenFlow SBP
        final OpenflowSouthboundPlugin ofpPlugin;
        ofpPlugin = new OpenflowSouthboundPluginBuilder()
                .from(configuration, lightyController.getServices())
                .build();
        ListenableFuture<Boolean> ofpStarter = ofpPlugin.start();
        setSystemReadyMonitor(lightyController, ofpStarter, MODULE_ID_OFP);

        //4. start OVSDB
        final LightyModule ovsdbSouthboundPlugin = new OvsdbSouthboundPlugin(lightyController.getServices());
        final ListenableFuture<Boolean> ovsdbStarter = ovsdbSouthboundPlugin.start();
        setSystemReadyMonitor(lightyController, ovsdbStarter, MODULE_ID_OVSDB);

        // Setup a shutdown hook
        shutdownHook = new ShutdownHook(lightyController, communityRestConf, ofpPlugin, ovsdbSouthboundPlugin);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Implements a thread which is executed during shutdown of this controller
     */
    private static class ShutdownHook extends Thread {

        private static final Logger LOG = LoggerFactory.getLogger(ShutdownHook.class);
        private final LightyController lightyController;
        private final LightyModule restconf;
        private final LightyModule opennflowPlugin;
        private final LightyModule ovsdbPlugin;

        ShutdownHook(final LightyController lightyController, final LightyModule restconf,
                     final LightyModule ofPlugin, final LightyModule ovsdbPlugin) {
            this.lightyController = lightyController;
            this.restconf = restconf;
            this.opennflowPlugin = ofPlugin;
            this.ovsdbPlugin=ovsdbPlugin;
        }

        @Override
        public void run() {
            LOG.info("SDN controller (powered by lighty.io) shutting down ...");
            final long startTime = System.nanoTime();
            try {
                this.ovsdbPlugin.shutdown();
            } catch (final Exception e) {
                LOG.error("OVSDB Application shutdown failed: ", e);
            }
            try {
                this.opennflowPlugin.shutdown();
            } catch (final Exception e) {
                LOG.error("OFP Application shutdown failed: ", e);
            }
            try {
                this.restconf.shutdown();
            } catch (final Exception e) {
                LOG.error("RESTCONF NBP shutdown failed:", e);
            }
            try {
                this.lightyController.shutdown();
            } catch (final Exception e) {
                LOG.error("Exception while shutting down Lighty controller:", e);
            }
            final float duration = (System.nanoTime() - startTime) / 1_000_000f;
            LOG.info("Lighty and OFP stopped in {}ms", duration);
        }
    }
}