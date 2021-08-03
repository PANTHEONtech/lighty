/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module;

import io.lighty.aaa.AAALighty;
import io.lighty.aaa.config.CertificateManagerConfig;
import io.lighty.applications.rnc.module.config.RncAAAConfiguration;
import io.lighty.applications.rnc.module.config.RncLightyModuleConfiguration;
import io.lighty.applications.rnc.module.config.RncRestConfConfiguration;
import io.lighty.applications.rnc.module.config.util.RncRestConfConfigUtils;
import io.lighty.applications.rnc.module.exception.RncLightyAppStartException;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.southbound.netconf.impl.NetconfSBPlugin;
import io.lighty.modules.southbound.netconf.impl.NetconfTopologyPluginBuilder;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import io.lighty.server.LightyServerBuilder;
import java.net.InetSocketAddress;
import java.security.Security;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RncLightyModule extends AbstractLightyModule {

    private static final Logger LOG = LoggerFactory.getLogger(RncLightyModule.class);
    private static final long DEFAULT_LIGHTY_MODULE_TIMEOUT = 60;
    private static final TimeUnit DEFAULT_LIGHTY_MODULE_TIME_UNIT = TimeUnit.SECONDS;

    private final RncLightyModuleConfiguration rncModuleConfig;

    private LightyController lightyController;
    private CommunityRestConf lightyRestconf;
    private NetconfSBPlugin lightyNetconf;
    private AAALighty aaaLighty;
    private LightyServerBuilder jettyServerBuilder;

    public RncLightyModule(RncLightyModuleConfiguration rncModuleConfig) {
        LOG.info("Creating instance of RNC lighty.io module...");
        this.rncModuleConfig = rncModuleConfig;
        LOG.info("Instance of RNC lighty.io module created!");
    }

    @Override
    protected boolean initProcedure() {
        LOG.info("Initializing RNC lighty.io module...");
        try {
            this.lightyController = initController(this.rncModuleConfig.getControllerConfig());
            startAndWaitLightyModule(this.lightyController);

            this.lightyRestconf = initRestconf(this.rncModuleConfig.getRestconfConfig(),
                    this.lightyController.getServices());
            startAndWaitLightyModule(this.lightyRestconf);

            this.lightyNetconf = initNetconf(this.rncModuleConfig.getNetconfConfig(),
                    this.lightyController.getServices());
            startAndWaitLightyModule(this.lightyNetconf);

            if (rncModuleConfig.getAaaConfig().isEnableAAA()) {
                this.aaaLighty = initAAA(this.rncModuleConfig.getAaaConfig(), this.lightyController.getServices());
                startAndWaitLightyModule(this.aaaLighty);
            }

            lightyRestconf.startServer();
        } catch (RncLightyAppStartException e) {
            LOG.error("Unable to initialize and start RNC lighty.io module!", e);
            return false;
        }
        LOG.info("RNC lighty.io module initialized successfully!");
        return true;
    }

    private LightyController initController(ControllerConfiguration config) throws RncLightyAppStartException {
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        try {
            return lightyControllerBuilder.from(config).build();
        } catch (ConfigurationException e) {
            throw new RncLightyAppStartException("Unable to initialize lighty.io controller module!", e);
        }
    }

    private NetconfSBPlugin initNetconf(NetconfConfiguration config, LightyServices services)
            throws RncLightyAppStartException {
        try {
            NetconfConfiguration configWithServices =
                    NetconfConfigUtils.injectServicesToTopologyConfig(config, services);
            return NetconfTopologyPluginBuilder.from(configWithServices, services).build();
        } catch (ConfigurationException e) {
            throw new RncLightyAppStartException("Unable to initialize lighty.io NETCONF module!", e);
        }
    }

    private CommunityRestConf initRestconf(RncRestConfConfiguration config, LightyServices services) {
        final RncRestConfConfiguration confConf = RncRestConfConfigUtils.getRestConfConfiguration(config, services);
        final InetSocketAddress inetSocketAddress =
                new InetSocketAddress(confConf.getInetAddress(), confConf.getHttpPort());

        if (confConf.isUseHttps()) {
            this.jettyServerBuilder = new HttpsLightyServerBuilder(inetSocketAddress, config.getSecurityConfig());
        } else {
            this.jettyServerBuilder = new LightyServerBuilder(inetSocketAddress);
        }

        return CommunityRestConfBuilder.from(confConf)
            .withLightyServer(jettyServerBuilder)
            .build();
    }

    private AAALighty initAAA(RncAAAConfiguration config, LightyServices services) {
        Security.addProvider(new BouncyCastleProvider());
        final DataBroker dataBroker = services.getBindingDataBroker();
        config.setCertificateManager(CertificateManagerConfig.getDefault(services.getBindingDataBroker()));

        return new AAALighty(dataBroker, config.getCertificateManager(), null, config.getShiroConf(),
            config.getMoonEndpointPath(), config.getDatastoreConf(), config.getDbUsername(), config.getDbPassword(),
            this.jettyServerBuilder);
    }

    private void startAndWaitLightyModule(LightyModule lightyModule) throws RncLightyAppStartException {
        try {
            LOG.info("Initializing lighty.io module ({})...", lightyModule.getClass());
            boolean startSuccess = lightyModule.start()
                    .get(DEFAULT_LIGHTY_MODULE_TIMEOUT, DEFAULT_LIGHTY_MODULE_TIME_UNIT);
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

    @Override
    protected boolean stopProcedure() {
        LOG.info("Stopping RNC lighty.io application...");
        boolean success = true;

        if (this.rncModuleConfig.getAaaConfig().isEnableAAA()
                && this.aaaLighty != null && !stopAndWaitLightyModule(this.aaaLighty)) {
            success = false;
        }

        if (this.lightyRestconf != null && !stopAndWaitLightyModule(this.lightyRestconf)) {
            success = false;
        }

        if (this.lightyNetconf != null && !stopAndWaitLightyModule(this.lightyNetconf)) {
            success = false;
        }

        if (this.lightyController != null && !stopAndWaitLightyModule(this.lightyController)) {
            success = false;
        }

        if (success) {
            LOG.info("RNC lighty.io module stopped successfully!");
            return true;
        } else {
            LOG.error("Some components of RNC lighty.io module were not stopped successfully!");
            return false;
        }
    }

    @SuppressWarnings({"checkstyle:illegalCatch"})
    private boolean stopAndWaitLightyModule(LightyModule lightyModule) {
        try {
            LOG.info("Stopping lighty.io module ({})...", lightyModule.getClass());
            boolean stopSuccess =
                    lightyModule.shutdown().get(DEFAULT_LIGHTY_MODULE_TIMEOUT, DEFAULT_LIGHTY_MODULE_TIME_UNIT);
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
