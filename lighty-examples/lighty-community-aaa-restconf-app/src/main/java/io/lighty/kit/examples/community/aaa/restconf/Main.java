/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.kit.examples.community.aaa.restconf;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.aaa.AAALighty;
import io.lighty.aaa.config.CertificateManagerConfig;
import io.lighty.aaa.config.DatastoreConfigurationConfig;
import io.lighty.aaa.config.ShiroConfigurationConfig;
import io.lighty.core.controller.api.LightyController;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) throws Exception {
        final long startTime = System.nanoTime();
        try {
            if (args.length > 0) {
                final Path configPath = Paths.get(args[0]);
                LOG.info("Lighty and Restconf starting, using configuration from file {} ...", configPath);
                final ControllerConfiguration singleNodeConfiguration =
                        ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
                final RestConfConfiguration restConfConfiguration = RestConfConfigUtils
                        .getRestConfConfiguration(Files.newInputStream(configPath));
                startLighty(singleNodeConfiguration, restConfConfiguration);
            } else {
                LOG.info("Lighty and Restconf starting, using default configuration ...");
                final Set<YangModuleInfo> modelPaths = Stream.concat(RestConfConfigUtils.YANG_MODELS.stream(),
                        AAALighty.YANG_MODELS.stream()).collect(Collectors.toSet());

                final ControllerConfiguration defaultSingleNodeConfiguration =
                        ControllerConfigUtils.getDefaultSingleNodeConfiguration(modelPaths);
                final RestConfConfiguration restConfConfig =
                        RestConfConfigUtils.getDefaultRestConfConfiguration();
                startLighty(defaultSingleNodeConfiguration, restConfConfig);
            }
            final float duration = (System.nanoTime() - startTime)/1_000_000f;
            LOG.info("Lighty and Restconf started in {}ms", duration);
        } catch (final Exception e) {
            LOG.error("Main Restconf application exception: ", e);
        }
    }

    private static void startLighty(final ControllerConfiguration controllerConfiguration,
            final RestConfConfiguration restconfConfiguration)
                    throws ConfigurationException, ExecutionException, InterruptedException {
        //1. initialize and start Lighty controller (MD-SAL, Controller, YangTools, Akka)
        final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        final LightyController lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
        lightyController.start().get();

        // 2. start Restconf server
        final CommunityRestConfBuilder communityRestConfBuilder = new CommunityRestConfBuilder();
        final LightyServerBuilder jettyServerBuilder = new LightyServerBuilder(new InetSocketAddress(
                restconfConfiguration.getInetAddress(), restconfConfiguration.getHttpPort()));
        final CommunityRestConf communityRestConf = communityRestConfBuilder.from(RestConfConfigUtils
                .getRestConfConfiguration(restconfConfiguration, lightyController.getServices())).withLightyServer(
                        jettyServerBuilder)
                .build();
        final ListenableFuture<Boolean> startRestconf = communityRestConf.start();
        addCallback(startRestconf);
        Security.addProvider(new BouncyCastleProvider());

        final DataBroker bindingDataBroker = lightyController.getServices().getBindingDataBroker();
        final String moonEndpointPath = "/moon";
        final String dbUsername = "foo";
        final String dbPassword = "bar";
        final AAALighty aaaLighty = new AAALighty(bindingDataBroker, CertificateManagerConfig.getDefault(
                bindingDataBroker), null, ShiroConfigurationConfig.getDefault(), moonEndpointPath,
                DatastoreConfigurationConfig.getDefault(), dbUsername, dbPassword, jettyServerBuilder);
        final ListenableFuture<Boolean> start = aaaLighty.start();

        addCallback(start);

        communityRestConf.startServer();

        Runtime.getRuntime().addShutdownHook(new ShutdownHook(lightyController, communityRestConf, aaaLighty));
    }

    public static void start() throws Exception {
        main(new String[]{});
    }

    private static void addCallback(final ListenableFuture<Boolean> start)
            throws InterruptedException {
        final CountDownLatch cdl = new CountDownLatch(1);
        Futures.addCallback(start, new FutureCallback<Boolean>() {

            @Override
            public void onSuccess(final Boolean result) {
                cdl.countDown();
            }

            @Override
            public void onFailure(final Throwable t) {
                throw new RuntimeException(t);
            }
        }, Executors.newSingleThreadExecutor());
        cdl.await();
    }

    private static class ShutdownHook extends Thread {

        private static final Logger LOG = LoggerFactory.getLogger(ShutdownHook.class);
        private final LightyController lightyController;
        private final CommunityRestConf communityRestConf;
        private final AAALighty aaaLighty;

        ShutdownHook(final LightyController lightyController, final CommunityRestConf communityRestConf,
                final AAALighty aaaLighty) {
            this.lightyController = lightyController;
            this.communityRestConf = communityRestConf;
            this.aaaLighty = aaaLighty;
        }

        @Override
        public void run() {
            LOG.info("Lighty and Restconf shutting down ...");
            final long startTime = System.nanoTime();
            try {
                this.communityRestConf.shutdown();
            } catch (final Exception e) {
                LOG.error("Exception:", e);
            }
            try {
                this.aaaLighty.shutdown();
            } catch (final Exception e) {
                LOG.error("Exception while shutting down Lighty controller:", e);
            }
            try {
                this.lightyController.shutdown();
            } catch (final Exception e) {
                LOG.error("Exception while shutting down Lighty controller:", e);
            }
            final float duration = (System.nanoTime() - startTime)/1_000_000f;
            LOG.info("Lighty and Restconf stopped in {}ms", duration);
        }
    }
}
