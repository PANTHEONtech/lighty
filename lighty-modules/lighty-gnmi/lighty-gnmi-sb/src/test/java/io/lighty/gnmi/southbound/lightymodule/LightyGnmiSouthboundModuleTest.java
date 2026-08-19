/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.lightymodule;

import static org.mockito.Mockito.when;

import io.lighty.aaa.encrypt.service.impl.AAAEncryptionServiceImpl;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.gnmi.southbound.yangmodule.config.GnmiConfiguration;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev240202.AaaEncryptServiceConfig;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev240202.AaaEncryptServiceConfigBuilder;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;

public class LightyGnmiSouthboundModuleTest {
    private static final long MODULE_TIMEOUT = 60;
    private static final TimeUnit MODULE_TIME_UNIT = TimeUnit.SECONDS;
    private static final Set<YangModuleInfo> YANG_MODELS = Set.of(
        org.opendaylight.yang.svc.v1.urn.opendaylight.gnmi.topology.rev210316.YangModuleInfoImpl.INSTANCE,
        org.opendaylight.yang.svc.v1.urn.opendaylight.gnmi.yang.storage.rev210331.YangModuleInfoImpl.INSTANCE,
        org.opendaylight.yang.svc.v1.urn.opendaylight.gnmi.force.capabilities.rev210702.YangModuleInfoImpl.INSTANCE,
        org.opendaylight.yang.svc.v1.urn.opendaylight.gnmi.certificate.storage.rev210504.YangModuleInfoImpl.INSTANCE
    );

    @Test
    public void gnmiModuleSmokeTest() throws Exception {
        final LightyController services = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(YANG_MODELS)).build();
        Assertions.assertTrue(services.start().get());

        final LightyGnmiSouthboundModule gnmiModule = new LightyGnmiSouthboundModule(services.getServices(),
            Executors.newCachedThreadPool(), createEncryptionService(),
            new org.opendaylight.gnmi.southbound.yangmodule.config.GnmiConfiguration(), null, null);
        Assertions.assertTrue(gnmiModule.start().get(MODULE_TIMEOUT, MODULE_TIME_UNIT));
        Assertions.assertTrue(gnmiModule.shutdown(MODULE_TIMEOUT, MODULE_TIME_UNIT));
        Assertions.assertTrue(services.shutdown(MODULE_TIMEOUT, MODULE_TIME_UNIT));
    }

    @Test
    public void gnmiModuleStartFailedTest() throws Exception {
        final LightyController services = new LightyControllerBuilder()
            .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(YANG_MODELS)).build();
        Assertions.assertTrue(services.start().get());
        final GnmiConfiguration defaultGnmiConfiguration = Mockito.mock(GnmiConfiguration.class);
        when(defaultGnmiConfiguration.getInitialYangsPaths())
            .thenReturn(List.of("invalid-path"));
        final LightyGnmiSouthboundModule gnmiModule = new LightyGnmiSouthboundModule(services.getServices(),
            Executors.newCachedThreadPool(), createEncryptionService(), defaultGnmiConfiguration, null, null);
        Assertions.assertFalse(gnmiModule.start().get(MODULE_TIMEOUT, MODULE_TIME_UNIT));
        Assertions.assertTrue(services.shutdown(MODULE_TIMEOUT, MODULE_TIME_UNIT));
    }

    private static AAAEncryptionServiceImpl createEncryptionService()
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        final AaaEncryptServiceConfig encrySrvConfig = getDefaultAaaEncryptServiceConfig();
        final byte[] encryptionKeySalt = Base64.getDecoder().decode(encrySrvConfig.getEncryptSalt());
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encrySrvConfig.getEncryptMethod());
        final KeySpec keySpec = new PBEKeySpec(encrySrvConfig.getEncryptKey().toCharArray(), encryptionKeySalt,
            encrySrvConfig.getEncryptIterationCount(), encrySrvConfig.getEncryptKeyLength());
        final SecretKey key
            = new SecretKeySpec(keyFactory.generateSecret(keySpec).getEncoded(), encrySrvConfig.getEncryptType());
        final GCMParameterSpec ivParameterSpec = new GCMParameterSpec(encrySrvConfig.getAuthTagLength(),
            encryptionKeySalt);
        return new AAAEncryptionServiceImpl(ivParameterSpec, encrySrvConfig.getCipherTransforms(), key);
    }

    private static AaaEncryptServiceConfig getDefaultAaaEncryptServiceConfig() {
        return new AaaEncryptServiceConfigBuilder().setEncryptKey("V1S1ED4OMeEh")
            .setPasswordLength(12).setEncryptSalt("TdtWeHbch/7xP52/rp3Usw==")
            .setEncryptMethod("PBKDF2WithHmacSHA1").setEncryptType("AES")
            .setEncryptIterationCount(32768).setEncryptKeyLength(128)
            .setAuthTagLength(128)
            .setCipherTransforms("AES/GCM/NoPadding").build();
    }
}

