package io.lighty.examples.controllers.restconf.ofp;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import io.lighty.modules.southbound.openflow.impl.OpenflowSouthboundPlugin;
import io.lighty.modules.southbound.openflow.impl.OpenflowSouthboundPluginBuilder;
import io.lighty.modules.southbound.openflow.impl.config.OpenflowpluginConfiguration;
import io.lighty.modules.southbound.openflow.impl.util.OpenflowConfigUtils;
import java.io.IOException;
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

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static ShutdownHook shutdownHook;

    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    public static void main(final String[] args) throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.info(".__  .__       .__     __              .__         ________  ___________");
        LOG.info("|  | |__| ____ |  |___/  |_ ___.__.    |__| ____   \\_____  \\ \\_   _____/");
        LOG.info("|  | |  |/ ___\\|  |  \\   __<   |  |    |  |/  _ \\   /   |   \\ |    __)  ");
        LOG.info("|  |_|  / /_/  >   Y  \\  |  \\___  |    |  (  <_> ) /    |    \\|     \\   ");
        LOG.info("|____/__\\___  /|___|  /__|  / ____| /\\ |__|\\____/  \\_______  /\\___  /   ");
        LOG.info("       /_____/      \\/      \\/      \\/                     \\/     \\/    ");
        LOG.info("Starting lighty.io RESTCONF-OPENFLOW example application ...");
        LOG.info("https://lighty.io/");
        LOG.info("https://github.com/PANTHEONtech/lighty-core");
        try {
            final ControllerConfiguration controllerConfiguration;
            final RestConfConfiguration restConfConfiguration;
            final OpenflowpluginConfiguration openflowpluginConfiguration;

            if (args.length > 0) {
                final Path configPath = Paths.get(args[0]);
                LOG.info("Lighty and OFP starting, using configuration from file {} ...", configPath);
                controllerConfiguration = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
                restConfConfiguration = RestConfConfigUtils
                        .getRestConfConfiguration(Files.newInputStream(configPath));
                openflowpluginConfiguration = OpenflowConfigUtils.getOfpConfiguration(Files.newInputStream(configPath));
            } else {
                LOG.info("Lighty and OFP starting, using default configuration ...");
                final Set<YangModuleInfo> modelPaths = Stream.concat(RestConfConfigUtils.YANG_MODELS.stream(),
                        OpenflowConfigUtils.OFP_MODELS.stream())
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
            LOG.info("Lighty and OFP started in {}", stopwatch.stop());
        } catch (IOException cause) {
            LOG.error("Main OFP application - could not read configuration: ", cause);
        } catch (ConfigurationException | ExecutionException | InterruptedException cause) {
            LOG.error("Main OFP application exception: ", cause);
        }
    }

    public static void start() throws Exception {
        main(new String[]{});
    }

    public static void shutdown() {
        if (shutdownHook != null) {
            shutdownHook.execute();
        }
    }

    private static void startLighty(final ControllerConfiguration controllerConfiguration,
                                    final RestConfConfiguration restConfConfiguration,
                                    OpenflowpluginConfiguration configuration)
            throws ConfigurationException, ExecutionException, InterruptedException {

        //1. initialize and start Lighty controller (MD-SAL, Controller, YangTools, Akka)
        final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        final LightyController lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
        lightyController.start().get();

        //2. start RESTCONF server
        final CommunityRestConf communityRestConf = CommunityRestConfBuilder
                .from(RestConfConfigUtils.getRestConfConfiguration(restConfConfiguration,
                        lightyController.getServices()))
                .build();
        communityRestConf.start();

        //3. start OpenFlow SBP
        final OpenflowSouthboundPlugin plugin;
        plugin = OpenflowSouthboundPluginBuilder
                .from(configuration, lightyController.getServices())
                .withPacketListener(new PacketInListener())
                .build();
        ListenableFuture<Boolean> start = plugin.start();

        //Set SystemReadyMonitor listeners to active state
        Futures.addCallback(start, new FutureCallback<>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result != null && result) {
                    lightyController.getServices().getLightySystemReadyService().onSystemBootReady();
                } else {
                    LOG.error("OFP wasn unable to start correctly");
                    throw new RuntimeException("Unexcepted result of OFP initialization");
                }
            }

            @Override
            public void onFailure(Throwable cause) {
                LOG.error("Exception while initializing OFP", cause);
            }
        }, Executors.newSingleThreadExecutor());

        shutdownHook = new ShutdownHook(lightyController, communityRestConf, plugin);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private static class ShutdownHook extends Thread {

        private static final Logger LOG = LoggerFactory.getLogger(ShutdownHook.class);
        private final LightyController lightyController;
        private final LightyModule restconf;
        private final LightyModule opennflowPlugin;

        ShutdownHook(final LightyController lightyController, final LightyModule restconf,
                     final LightyModule ofPlugin) {
            this.lightyController = lightyController;
            this.restconf = restconf;
            this.opennflowPlugin = ofPlugin;
        }

        @Override
        public void run() {
            this.execute();
        }

        @SuppressWarnings("checkstyle:illegalCatch")
        public void execute() {
            LOG.info("Lighty and OFP shutting down ...");
            Stopwatch stopwatch = Stopwatch.createStarted();
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
            LOG.info("Lighty and OFP stopped in {}", stopwatch.stop());
        }
    }

    private Main() {
        // Hide constructor
    }
}
