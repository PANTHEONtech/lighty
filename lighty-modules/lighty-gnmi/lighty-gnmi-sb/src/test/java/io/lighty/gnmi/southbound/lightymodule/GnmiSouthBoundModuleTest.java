/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.lightymodule;

import static org.mockito.Mockito.when;

import io.lighty.aaa.encrypt.service.impl.AAAEncryptionServiceImpl;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import io.lighty.gnmi.southbound.lightymodule.util.GnmiConfigUtils;
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
        Assertions.assertTrue(gnmiModule.shutdown(MODULE_TIMEOUT, MODULE_TIME_UNIT));
        Assertions.assertTrue(services.shutdown(MODULE_TIMEOUT, MODULE_TIME_UNIT));
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
        Assertions.assertTrue(services.shutdown(MODULE_TIMEOUT, MODULE_TIME_UNIT));
    }

    private static AAAEncryptionServiceImpl createEncryptionService() throws NoSuchPaddingException,
        NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException {
        final String salt = "TdtWeHbch/7xP52/rp3Usw==";
        final String encryptKey = "V1S1ED4OMeEh";
        final String encryptMethod = "PBKDF2WithHmacSHA1";
        final String encryptType = "AES";
        final int iterationCount = 32768;
        final int encryptKeyLength = 128;
        final String cipherTransforms = "AES/CBC/PKCS5Padding";

        final byte[] encryptionKeySalt = Base64.getDecoder().decode(salt);
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encryptMethod);
        final KeySpec keySpec = new PBEKeySpec(encryptKey.toCharArray(), encryptionKeySalt,
            iterationCount, encryptKeyLength);
        final SecretKey key = new SecretKeySpec(keyFactory.generateSecret(keySpec).getEncoded(),
            encryptType);
        final IvParameterSpec ivParameterSpec = new IvParameterSpec(encryptionKeySalt);

        final Cipher encryptCipher = Cipher.getInstance(cipherTransforms);
        encryptCipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);

        final Cipher decryptCipher = Cipher.getInstance(cipherTransforms);
        decryptCipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);

        return new AAAEncryptionServiceImpl(encryptCipher, decryptCipher);
    }
}

