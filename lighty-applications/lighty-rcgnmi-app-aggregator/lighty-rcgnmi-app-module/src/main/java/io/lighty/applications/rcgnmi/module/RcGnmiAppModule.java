/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.applications.rcgnmi.module;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.gnmi.southbound.lightymodule.GnmiSouthboundModule;
import io.lighty.gnmi.southbound.lightymodule.GnmiSouthboundModuleBuilder;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.modules.southbound.netconf.impl.AAAEncryptionServiceImpl;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev160915.AaaEncryptServiceConfig;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev160915.AaaEncryptServiceConfigBuilder;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RcGnmiAppModule extends AbstractLightyModule {

    private static final Logger LOG = LoggerFactory.getLogger(RcGnmiAppModule.class);
    private static final long DEFAULT_LIGHTY_MODULE_TIMEOUT = 60;
    private static final TimeUnit DEFAULT_LIGHTY_MODULE_TIME_UNIT = TimeUnit.SECONDS;

    private final RcGnmiAppConfiguration appModuleConfig;
    private final ExecutorService gnmiExecutorService;
    private final CrossSourceStatementReactor customReactor;
    private LightyController lightyController;
    private CommunityRestConf lightyRestconf;
    private GnmiSouthboundModule gnmiSouthboundModule;

    public RcGnmiAppModule(final RcGnmiAppConfiguration appModuleConfig,
                           final ExecutorService gnmiExecutorService,
                           @Nullable final CrossSourceStatementReactor customReactor) {
        LOG.info("Creating instance of RgNMI lighty.io module...");
        this.appModuleConfig = Objects.requireNonNull(appModuleConfig);
        this.gnmiExecutorService = Objects.requireNonNull(gnmiExecutorService);
        this.customReactor = customReactor;
        LOG.info("Instance of RCgNMI lighty.io module created!");
    }

    @Override
    protected boolean initProcedure() {
        LOG.info("Initializing RCgNMI lighty.io module...");
        try {
            this.lightyController = initController(this.appModuleConfig.getControllerConfig());
            startAndWaitLightyModule(this.lightyController);

            this.lightyRestconf = initRestconf(this.appModuleConfig.getRestconfConfig(),
                    this.lightyController.getServices());
            startAndWaitLightyModule(this.lightyRestconf);

            final AAAEncryptionService encryptionService = createEncryptionServiceWithErrorHandling();
            this.gnmiSouthboundModule = initGnmiModule(this.lightyController.getServices(),
                    this.gnmiExecutorService, this.appModuleConfig.getGnmiConfiguration(), encryptionService,
                    this.customReactor);
            startAndWaitLightyModule(this.gnmiSouthboundModule);

        } catch (RcGnmiAppException e) {
            LOG.error("Unable to initialize and start RCgNMI lighty.io module!", e);
            return false;
        }
        LOG.info("RCgNMI lighty.io module initialized successfully!");
        return true;
    }

    private LightyController initController(final ControllerConfiguration config) throws RcGnmiAppException {
        final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        try {
            return lightyControllerBuilder.from(config).build();
        } catch (ConfigurationException e) {
            throw new RcGnmiAppException("Unable to initialize lighty.io controller module!", e);
        }
    }

    private CommunityRestConf initRestconf(final RestConfConfiguration config, final LightyServices services) {
        final RestConfConfiguration conf = RestConfConfigUtils.getRestConfConfiguration(config, services);
        return CommunityRestConfBuilder.from(conf).build();
    }

    private GnmiSouthboundModule initGnmiModule(final LightyServices services,
                                                final ExecutorService gnmiExecService,
                                                final GnmiConfiguration gnmiConfiguration,
                                                final AAAEncryptionService encryptionService,
                                                final CrossSourceStatementReactor reactor) {

        return new GnmiSouthboundModuleBuilder()
                .withConfig(gnmiConfiguration)
                .withLightyServices(services)
                .withExecutorService(gnmiExecService)
                .withEncryptionService(encryptionService)
                .withReactor(reactor)
                .build();
    }

    private void startAndWaitLightyModule(final LightyModule lightyModule) throws RcGnmiAppException {
        try {
            LOG.info("Initializing lighty.io module ({})...", lightyModule.getClass());
            final boolean startSuccess = lightyModule.start()
                    .get(DEFAULT_LIGHTY_MODULE_TIMEOUT, DEFAULT_LIGHTY_MODULE_TIME_UNIT);
            if (startSuccess) {
                LOG.info("lighty.io module ({}) initialized successfully!", lightyModule.getClass());
            } else {
                throw new RcGnmiAppException(
                        String.format("Unable to initialize lighty.io module (%s)!", lightyModule.getClass()));
            }
        } catch (TimeoutException | ExecutionException e) {
            throw new RcGnmiAppException(
                    String.format("Exception was thrown during initialization of lighty.io module (%s)!",
                            lightyModule.getClass()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RcGnmiAppException(
                    String.format("Exception was thrown during initialization of lighty.io module (%s)!",
                            lightyModule.getClass()), e);
        }
    }

    @Override
    protected boolean stopProcedure() {
        LOG.info("Stopping RCgNMI lighty.io application...");
        boolean success = true;

        if (this.lightyRestconf != null && !stopAndWaitLightyModule(this.lightyRestconf)) {
            success = false;
        }
        if (this.lightyController != null && !stopAndWaitLightyModule(this.lightyController)) {
            success = false;
        }
        if (this.lightyController != null && !stopAndWaitLightyModule(this.gnmiSouthboundModule)) {
            success = false;
        }

        if (success) {
            LOG.info("RCgNMI lighty.io module stopped successfully!");
            return true;
        } else {
            LOG.error("Some components of RCgNMI lighty.io module were not stopped successfully!");
            return false;
        }
    }

    @SuppressWarnings({"checkstyle:illegalCatch"})
    private boolean stopAndWaitLightyModule(final LightyModule lightyModule) {
        try {
            LOG.info("Stopping lighty.io module ({})...", lightyModule.getClass());
            final boolean stopSuccess =
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
            return false;
        }
    }

    private AAAEncryptionService createEncryptionServiceWithErrorHandling() throws RcGnmiAppException {
        try {
            return createEncryptionService();
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeySpecException
                | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new RcGnmiAppException("Failed to create Encryption Service", e);
        }
    }

    private AAAEncryptionServiceImpl createEncryptionService() throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException {
        final AaaEncryptServiceConfig encrySrvConfig = getDefaultAaaEncryptServiceConfig();
        final byte[] encryptionKeySalt = Base64.getDecoder().decode(encrySrvConfig.getEncryptSalt());
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encrySrvConfig.getEncryptMethod());
        final KeySpec keySpec = new PBEKeySpec(encrySrvConfig.getEncryptKey().toCharArray(), encryptionKeySalt,
                encrySrvConfig.getEncryptIterationCount(), encrySrvConfig.getEncryptKeyLength());
        final SecretKey key
                = new SecretKeySpec(keyFactory.generateSecret(keySpec).getEncoded(), encrySrvConfig.getEncryptType());
        final IvParameterSpec ivParameterSpec = new IvParameterSpec(encryptionKeySalt);

        final Cipher encryptCipher = Cipher.getInstance(encrySrvConfig.getCipherTransforms());
        encryptCipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);

        final Cipher decryptCipher = Cipher.getInstance(encrySrvConfig.getCipherTransforms());
        decryptCipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);

        return new AAAEncryptionServiceImpl(encryptCipher, decryptCipher);
    }

    private AaaEncryptServiceConfig getDefaultAaaEncryptServiceConfig() {
        return new AaaEncryptServiceConfigBuilder().setEncryptKey("V1S1ED4OMeEh")
                .setPasswordLength(12).setEncryptSalt("TdtWeHbch/7xP52/rp3Usw==")
                .setEncryptMethod("PBKDF2WithHmacSHA1").setEncryptType("AES")
                .setEncryptIterationCount(32768).setEncryptKeyLength(128)
                .setCipherTransforms("AES/CBC/PKCS5Padding").build();
    }
}
