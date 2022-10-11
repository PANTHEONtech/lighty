/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module;

import io.lighty.aaa.AAALighty;
import io.lighty.aaa.config.AAAConfiguration;
import io.lighty.aaa.config.CertificateManagerConfig;
import io.lighty.applications.rnc.module.config.RncLightyModuleConfiguration;
import io.lighty.applications.rnc.module.exception.RncLightyAppStartException;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.modules.southbound.netconf.impl.NetconfSBPlugin;
import io.lighty.modules.southbound.netconf.impl.NetconfTopologyPluginBuilder;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import io.lighty.server.Http2LightyServerBuilder;
import io.lighty.server.HttpsLightyServerBuilder;
import io.lighty.server.LightyServerBuilder;
import io.lighty.server.config.LightyServerConfig;
import io.lighty.swagger.SwaggerLighty;
import java.net.InetSocketAddress;
import java.security.Security;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RncLightyModule {

    private static final Logger LOG = LoggerFactory.getLogger(RncLightyModule.class);
    private static final TimeUnit DEFAULT_LIGHTY_MODULE_TIME_UNIT = TimeUnit.SECONDS;

    private final RncLightyModuleConfiguration rncModuleConfig;

    private final long lightyModuleTimeout;
    private LightyController lightyController;
    private CommunityRestConf lightyRestconf;
    private NetconfSBPlugin lightyNetconf;
    private AAALighty aaaLighty;
    private LightyServerBuilder jettyServerBuilder;
    private SwaggerLighty swagger;

    public RncLightyModule(final RncLightyModuleConfiguration rncModuleConfig) {
        LOG.info("Creating instance of RNC lighty.io module...");
        this.rncModuleConfig = rncModuleConfig;
        this.lightyModuleTimeout = rncModuleConfig.getModuleConfig().getModuleTimeoutSeconds();
        LOG.info("Instance of RNC lighty.io module created!");
    }

    public boolean initModules() {
        LOG.info("Initializing RNC lighty.io module...");
        try {
            this.lightyController = initController(this.rncModuleConfig.getControllerConfig());
            startAndWaitLightyModule(this.lightyController);

            this.lightyRestconf = initRestconf(this.rncModuleConfig.getRestconfConfig(),
                    this.rncModuleConfig.getServerConfig(), this.lightyController.getServices());
            startAndWaitLightyModule(this.lightyRestconf);

            this.lightyNetconf = initNetconf(this.rncModuleConfig.getNetconfConfig(),
                    this.lightyController.getServices());
            startAndWaitLightyModule(this.lightyNetconf);

            if (rncModuleConfig.getAaaConfig().isEnableAAA()) {
                this.aaaLighty = initAAA(this.rncModuleConfig.getAaaConfig(), this.lightyController.getServices());
                startAndWaitLightyModule(this.aaaLighty);
            }

            if (rncModuleConfig.getServerConfig().isEnableSwagger()) {
                this.swagger = initSwaggerLighty(this.rncModuleConfig.getRestconfConfig(),
                                                 this.jettyServerBuilder,
                                                 this.lightyController.getServices());
                startAndWaitLightyModule(this.swagger);
            }

            lightyRestconf.startServer();
        } catch (RncLightyAppStartException e) {
            LOG.error("Unable to initialize and start RNC lighty.io module!", e);
            return false;
        }
        LOG.info("RNC lighty.io module initialized successfully!");
        return true;
    }

    private LightyController initController(final ControllerConfiguration config) throws RncLightyAppStartException {
        try {
            return new LightyControllerBuilder().from(config).build();
        } catch (ConfigurationException e) {
            throw new RncLightyAppStartException("Unable to initialize lighty.io controller module!", e);
        }
    }

    private NetconfSBPlugin initNetconf(final NetconfConfiguration config, final LightyServices services)
            throws RncLightyAppStartException {
        try {
            final NetconfConfiguration configWithServices =
                    NetconfConfigUtils.injectServicesToTopologyConfig(config, services);
            return NetconfTopologyPluginBuilder.from(configWithServices, services).build();
        } catch (ConfigurationException e) {
            throw new RncLightyAppStartException("Unable to initialize lighty.io NETCONF module!", e);
        }
    }

    private CommunityRestConf initRestconf(final RestConfConfiguration rcConfig, final LightyServerConfig serverConfig,
            final LightyServices services) {
        final var restConfConfiguration = RestConfConfigUtils.getRestConfConfiguration(rcConfig, services);
        final var inetSocketAddress = new InetSocketAddress(rcConfig.getInetAddress(), rcConfig.getHttpPort());

        if (serverConfig.isUseHttp2()) {
            jettyServerBuilder = new Http2LightyServerBuilder(inetSocketAddress, serverConfig.getSecurityConfig());
        } else if (serverConfig.isUseHttps()) {
            jettyServerBuilder = new HttpsLightyServerBuilder(inetSocketAddress, serverConfig.getSecurityConfig());
        } else {
            jettyServerBuilder = new LightyServerBuilder(inetSocketAddress);
        }

        return CommunityRestConfBuilder.from(restConfConfiguration)
            .withLightyServer(jettyServerBuilder)
            .build();
    }

    private AAALighty initAAA(final AAAConfiguration config, final LightyServices services) {
        Security.addProvider(new BouncyCastleProvider());
        config.setCertificateManager(CertificateManagerConfig.getDefault(services.getBindingDataBroker()));
        return new AAALighty(services.getBindingDataBroker(), null, this.jettyServerBuilder, config);
    }

    private SwaggerLighty initSwaggerLighty(final RestConfConfiguration config, final LightyServerBuilder serverBuilder,
            final LightyServices services) {
        return new SwaggerLighty(config, serverBuilder, services);
    }

    private void startAndWaitLightyModule(final LightyModule lightyModule) throws RncLightyAppStartException {
        try {
            LOG.info("Initializing lighty.io module ({})...", lightyModule.getClass());
            boolean startSuccess = lightyModule.start()
                    .get(lightyModuleTimeout, DEFAULT_LIGHTY_MODULE_TIME_UNIT);
            if (startSuccess) {
                LOG.info("lighty.io module ({}) initialized successfully!", lightyModule.getClass());
            } else {
                throw new RncLightyAppStartException(
                        String.format("Unable to initialize lighty.io module (%s)!", lightyModule.getClass()));
            }
        } catch (TimeoutException | ExecutionException e) {
            throw new RncLightyAppStartException(
                    String.format("Exception was thrown during initialization of lighty.io module (%s)!",
                            lightyModule.getClass()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RncLightyAppStartException(
                String.format("Exception was thrown during initialization of lighty.io module (%s)!",
                    lightyModule.getClass()), e);
        }
    }

    public boolean close() {
        LOG.info("Stopping RNC lighty.io application...");
        boolean success = true;
        if (rncModuleConfig.getServerConfig().isEnableSwagger()
                && this.swagger != null && !stopAndWaitLightyModule(this.swagger)) {
            success = false;
        }
        if (this.rncModuleConfig.getAaaConfig().isEnableAAA()
                && this.aaaLighty != null && !stopAndWaitLightyModule(this.aaaLighty)) {
            success = false;
        }

        final LightyModule[] lightyModuleList = new LightyModule[]{lightyRestconf, lightyNetconf, lightyController};
        success = !Arrays.stream(lightyModuleList)
                .anyMatch((lightyModule) -> lightyModule != null
                        && !stopAndWaitLightyModule(lightyModule));

        if (success) {
            LOG.info("RNC lighty.io module stopped successfully!");
            return true;
        } else {
            LOG.error("Some components of RNC lighty.io module were not stopped successfully!");
            return false;
        }
    }

    @SuppressWarnings({"checkstyle:illegalCatch"})
    private boolean stopAndWaitLightyModule(final LightyModule lightyModule) {
        try {
            LOG.info("Stopping lighty.io module ({})...", lightyModule.getClass());
            final boolean stopSuccess =
                    lightyModule.shutdown().get(lightyModuleTimeout, DEFAULT_LIGHTY_MODULE_TIME_UNIT);
            if (stopSuccess) {
                LOG.info("lighty.io module ({}) stopped successfully!", lightyModule.getClass());
                return true;
            } else {
                LOG.error("Unable to stop lighty.io module ({})!", lightyModule.getClass());
                return false;
            }
        } catch (Exception e) {
            LOG.error("Exception was thrown while stopping the lighty.io module ({})!", lightyModule.getClass(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}
