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
import io.lighty.openapi.OpenApiLighty;
import io.lighty.server.LightyJettyServerProvider;
import io.lighty.server.config.LightyServerConfig;
import java.net.InetSocketAddress;
import java.security.Security;
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
    private LightyJettyServerProvider jettyServerBuilder;
    private OpenApiLighty openApi;

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

            if (rncModuleConfig.getServerConfig().isEnableOpenApi()) {
                lightyController.getServices().withJaxRsEndpoint(lightyRestconf.getJaxRsEndpoint());
                this.openApi = initOpenApiLighty(this.rncModuleConfig.getRestconfConfig(),
                                                 this.jettyServerBuilder,
                                                 this.lightyController.getServices());
                startAndWaitLightyModule(this.openApi);
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

        jettyServerBuilder = new LightyJettyServerProvider(serverConfig, inetSocketAddress);

        return CommunityRestConfBuilder.from(restConfConfiguration)
            .withLightyServer(jettyServerBuilder)
            .build();
    }

    private AAALighty initAAA(final AAAConfiguration config, final LightyServices services) {
        Security.addProvider(new BouncyCastleProvider());
        config.setCertificateManager(
                CertificateManagerConfig.getDefault(services.getBindingDataBroker(), services.getRpcProviderService()));
        return new AAALighty(services.getBindingDataBroker(), null, this.jettyServerBuilder, config);
    }

    private OpenApiLighty initOpenApiLighty(final RestConfConfiguration config,
            final LightyJettyServerProvider serverBuilder, final LightyServices services) {
        return new OpenApiLighty(config, serverBuilder, services, null);
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
        if (rncModuleConfig.getServerConfig().isEnableOpenApi() && this.openApi != null) {
            success &= openApi.shutdown(lightyModuleTimeout, DEFAULT_LIGHTY_MODULE_TIME_UNIT);
        }
        if (this.rncModuleConfig.getAaaConfig().isEnableAAA() && this.aaaLighty != null) {
            success &= aaaLighty.shutdown(lightyModuleTimeout, DEFAULT_LIGHTY_MODULE_TIME_UNIT);
        }
        if (this.lightyRestconf != null) {
            success &= lightyRestconf.shutdown(lightyModuleTimeout, DEFAULT_LIGHTY_MODULE_TIME_UNIT);
        }
        if (this.lightyNetconf != null) {
            success &= lightyNetconf.shutdown(lightyModuleTimeout, DEFAULT_LIGHTY_MODULE_TIME_UNIT);
        }
        if (this.lightyController != null) {
            success &= lightyController.shutdown(lightyModuleTimeout, DEFAULT_LIGHTY_MODULE_TIME_UNIT);
        }
        if (success) {
            LOG.info("RNC lighty.io module stopped successfully!");
            return true;
        } else {
            LOG.error("Some components of RNC lighty.io module were not stopped successfully!");
            return false;
        }
    }
}
