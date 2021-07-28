/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.lightymodule;

import static org.mockito.Mockito.when;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import io.lighty.gnmi.southbound.lightymodule.util.GnmiConfigUtils;
import io.lighty.modules.encrypt.service.aaa.impl.AAAEncryptionServiceImpl;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev160915.AaaEncryptServiceConfig;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev160915.AaaEncryptServiceConfigBuilder;

public class GnmiSouthBoundModuleTest {
    private static final long MODULE_TIMEOUT = 60;
    private static final TimeUnit MODULE_TIME_UNIT = TimeUnit.SECONDS;

    @Test
    public void gnmiModuleSmokeTest() throws Exception {
        final LightyController services = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(GnmiConfigUtils.YANG_MODELS)).build();
        Assertions.assertTrue(services.start().get());

        final GnmiSouthboundModule gnmiModule = new GnmiSouthboundModule(services.getServices(),
                Executors.newCachedThreadPool(), createEncryptionService(),
                GnmiConfigUtils.getDefaultGnmiConfiguration(), null);
        Assertions.assertTrue(gnmiModule.start().get(MODULE_TIMEOUT, MODULE_TIME_UNIT));
        Assertions.assertTrue(gnmiModule.shutdown().get(MODULE_TIMEOUT, MODULE_TIME_UNIT));
        Assertions.assertTrue(services.shutdown().get(MODULE_TIMEOUT, MODULE_TIME_UNIT));
    }

    @Test
    public void gnmiModuleStartFailedTest() throws Exception {
        final LightyController services = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(GnmiConfigUtils.YANG_MODELS)).build();
        Assertions.assertTrue(services.start().get());
        final GnmiConfiguration defaultGnmiConfiguration = Mockito.mock(GnmiConfiguration.class);
        when(defaultGnmiConfiguration.getInitialYangsPaths())
                .thenReturn(List.of("invalid-path"));
        final GnmiSouthboundModule gnmiModule = new GnmiSouthboundModule(services.getServices(),
                Executors.newCachedThreadPool(), createEncryptionService(), defaultGnmiConfiguration, null);
        Assertions.assertFalse(gnmiModule.start().get(MODULE_TIMEOUT, MODULE_TIME_UNIT));
        Assertions.assertTrue(services.shutdown().get(MODULE_TIMEOUT, MODULE_TIME_UNIT));
    }

    private static AAAEncryptionServiceImpl createEncryptionService() throws NoSuchPaddingException,
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

    private static AaaEncryptServiceConfig getDefaultAaaEncryptServiceConfig() {
        return new AaaEncryptServiceConfigBuilder().setEncryptKey("V1S1ED4OMeEh")
                .setPasswordLength(12).setEncryptSalt("TdtWeHbch/7xP52/rp3Usw==")
                .setEncryptMethod("PBKDF2WithHmacSHA1").setEncryptType("AES")
                .setEncryptIterationCount(32768).setEncryptKeyLength(128)
                .setCipherTransforms("AES/CBC/PKCS5Padding").build();
    }
}

