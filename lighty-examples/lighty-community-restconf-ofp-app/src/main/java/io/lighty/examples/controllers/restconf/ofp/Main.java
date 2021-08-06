package io.lighty.examples.controllers.restconf.ofp;

import com.google.common.base.Stopwatch;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    private LightyController lightyController;
    private OpenflowSouthboundPlugin openflowSBPlugin;
    private CommunityRestConf restconf;

    public static void main(String[] args) {
        Main main = new Main();
        main.start(args, true);
    }

    @SuppressWarnings("IllegalCatch")
    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    public void start(final String[] args, boolean registerShutdownHook) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.info(".__  .__       .__     __              .__         ________  ___________");
        LOG.info("|  | |__| ____ |  |___/  |_ ___.__.    |__| ____   \\_____  \\ \\_   _____/");
        LOG.info("|  | |  |/ ___\\|  |  \\   __<   |  |    |  |/  _ \\   /   |   \\ |    __)  ");
        LOG.info("|  |_|  / /_/  >   Y  \\  |  \\___  |    |  (  <_> ) /    |    \\|     \\   ");
        LOG.info("|____/__\\___  /|___|  /__|  / ____| /\\ |__|\\____/  \\_______  /\\___  /   ");
        LOG.info("       /_____/      \\/      \\/      \\/                     \\/     \\/    ");
        LOG.info("Starting lighty.io RESTCONF-OPENFLOW example application ...");
        LOG.info("https://lighty.io/");
        LOG.info("https://github.com/PANTHEONtech/lighty");

        try {
            final ControllerConfiguration controllerConfiguration;
            final RestConfConfiguration restconfConfiguration;
            final OpenflowpluginConfiguration openflowConfiguration;

            if (args.length > 0) {
                final Path configPath = Paths.get(args[0]);
                LOG.info("Lighty and OFP starting, using configuration from file {} ...", configPath);
                controllerConfiguration = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
                restconfConfiguration = RestConfConfigUtils.getRestConfConfiguration(Files.newInputStream(configPath));
                openflowConfiguration = OpenflowConfigUtils.getOfpConfiguration(Files.newInputStream(configPath));
            } else {
                LOG.info("Lighty and OFP starting, using default configuration ...");
                final Set<YangModuleInfo> modelPaths = Stream.concat(RestConfConfigUtils.YANG_MODELS.stream(),
                        OpenflowConfigUtils.OFP_MODELS.stream())
                        .collect(Collectors.toSet());
                controllerConfiguration = ControllerConfigUtils.getDefaultSingleNodeConfiguration(modelPaths);
                restconfConfiguration =  RestConfConfigUtils.getDefaultRestConfConfiguration();
                openflowConfiguration = OpenflowConfigUtils.getDefaultOfpConfiguration();
            }
            //Register shutdown hook for graceful shutdown.
            if (registerShutdownHook) {
                Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            }
            // start controller at last
            startLighty(controllerConfiguration, restconfConfiguration, openflowConfiguration);
            LOG.info("Lighty.io and OFP started in {}", stopwatch.stop());
        } catch (IOException e) {
            LOG.error("Main OFP application - could not read configuration: ", e);
            shutdown();
        } catch (Exception e) {
            LOG.error("Main OFP application exception: ", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            shutdown();
        }
    }

    private void startLighty(final ControllerConfiguration controllerConfiguration,
                                final RestConfConfiguration restconfConfiguration,
                                final OpenflowpluginConfiguration ofpConfiguration)
        throws ConfigurationException, ExecutionException, InterruptedException, TimeoutException,
               ModuleStartupException {

        //1. initialize and start Lighty controller (MD-SAL, Controller, YangTools, Akka)
        final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        this.lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
        final boolean controllerStartOk = this.lightyController.start().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!controllerStartOk) {
            throw new ModuleStartupException("Lighty.io Controller startup failed!");
        }

        //2. start RESTCONF server
        this.restconf = CommunityRestConfBuilder
                .from(RestConfConfigUtils.getRestConfConfiguration(restconfConfiguration,
                    this.lightyController.getServices()))
                .build();
        final boolean restconfStartOk = this.restconf.start().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!restconfStartOk) {
            throw new ModuleStartupException("Community Restconf startup failed!");
        }

        //3. start OpenFlow SBP and set SystemReadyMonitor listeners to active state
        this.openflowSBPlugin = OpenflowSouthboundPluginBuilder
                .from(ofpConfiguration, this.lightyController.getServices())
                .withPacketListener(new PacketInListener())
                .build();
        final boolean ofpStartOk = this.openflowSBPlugin.start().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!ofpStartOk) {
            throw new ModuleStartupException("OpenflowSB Plugin startup failed - shutting down ...");
        } else {
            this.lightyController.getServices().getLightySystemReadyService().onSystemBootReady();
        }
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
        LOG.info("Lighty and OFP shutting down ...");
        final Stopwatch stopwatch = Stopwatch.createStarted();
        closeLightyModule(this.openflowSBPlugin);
        closeLightyModule(this.restconf);
        closeLightyModule(this.lightyController);
        LOG.info("Lighty and OFP stopped in {}", stopwatch.stop());
    }

    private Main() {
        // Hide constructor
    }
}
