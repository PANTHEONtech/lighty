package io.lighty.examples.controllers.restapp;

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
import io.lighty.server.LightyServerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev210321.HelloService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    private LightyController lightyController;
    private CommunityRestConf restconf;
    private ObjectRegistration<HelloService> rpcRegistration;

    public static void main(final String[] args) {
        final Main app = new Main();
        app.start(args, true);
    }

    public void start() {
        start(new String[]{}, false);
    }

    @SuppressWarnings("IllegalCatch")
    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    public void start(String[] args, boolean registerShutdownHook) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.info(".__  .__       .__     __              .__           _________________    _______");
        LOG.info("|  | |__| ____ |  |___/  |_ ___.__.    |__| ____    /   _____/\\______ \\   \\      \\");
        LOG.info("|  | |  |/ ___\\|  |  \\   __<   |  |    |  |/  _ \\   \\_____  \\  |    |  \\  /   |   \\");
        LOG.info("|  |_|  / /_/  >   Y  \\  |  \\___  |    |  (  <_> )  /        \\ |    `   \\/    |    \\");
        LOG.info("|____/__\\___  /|___|  /__|  / ____| /\\ |__|\\____/  /_______  //_______  /\\____|__  /");
        LOG.info("        /_____/     \\/      \\/      \\/                     \\/         \\/         \\/");
        LOG.info("Starting Lighty.io RESTCONF-ACTIONS example application ...");
        LOG.info("https://lighty.io/");
        LOG.info("https://github.com/PANTHEONtech/lighty");
        try {
            final ControllerConfiguration singleNodeConfiguration;
            final RestConfConfiguration restconfConfiguration;
            if (args.length > 0) {
                final Path configPath = Paths.get(args[0]);
                LOG.info("using configuration from file {} ...", configPath);
                //1. get controller configuration
                singleNodeConfiguration = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
                //2. get RESTCONF NBP configuration
                restconfConfiguration = RestConfConfigUtils.getRestConfConfiguration(Files.newInputStream(configPath));
            } else {
                restconfConfiguration = RestConfConfigUtils.getDefaultRestConfConfiguration();
                final Set<YangModuleInfo> moduleInfos = new HashSet<>();
                ServiceLoader<YangModelBindingProvider> yangProviderLoader = ServiceLoader.load(YangModelBindingProvider.class);
                for (YangModelBindingProvider yangModelBindingProvider : yangProviderLoader) {
                    moduleInfos.add(yangModelBindingProvider.getModuleInfo());
                }
                singleNodeConfiguration = ControllerConfigUtils.getDefaultSingleNodeConfiguration(moduleInfos);
            }
            if (registerShutdownHook) {
                Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            }
            startLighty(singleNodeConfiguration, restconfConfiguration);
            LOG.info("lighty.io and RESTCONF-NETCONF started in {}", stopwatch.stop());
            rpcRegistration = HelloProviderRegistration.registerRpc(lightyController);
            LOG.info("Example DOM action implementation registered: {}", rpcRegistration.getInstance());
        } catch (IOException e) {
            LOG.error("Main RESTCONF-NETCONF application - error reading config file: ", e);
            shutdown();
        } catch (Exception e) {
            LOG.error("Main RESTCONF-NETCONF application exception: ", e);
            if (e instanceof InterruptedException) {
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
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        this.lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
        final boolean controllerStartOk = this.lightyController.start().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!controllerStartOk) {
            throw new ModuleStartupException("Lighty.io Controller startup failed!");
        }

        //2. build RestConf server
        LightyServerBuilder jettyServerBuilder = new LightyServerBuilder(new InetSocketAddress(
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
        this.restconf.startServer();
    }

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
        LOG.info("Lighty.io and RESTCONF-NETCONF shutting down ...");
        final Stopwatch stopwatch = Stopwatch.createStarted();
        if (rpcRegistration != null) {
            rpcRegistration.close();
        }
        closeLightyModule(this.restconf);
        closeLightyModule(this.lightyController);
        LOG.info("Lighty.io and RESTCONF-NETCONF stopped in {}", stopwatch.stop());
    }
}