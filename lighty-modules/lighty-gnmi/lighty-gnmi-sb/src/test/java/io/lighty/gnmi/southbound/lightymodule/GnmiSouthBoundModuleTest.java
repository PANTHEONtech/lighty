/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.gnmi.southbound.lightymodule;

import static org.mockito.Mockito.when;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import io.lighty.gnmi.southbound.lightymodule.util.GnmiConfigUtils;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.aaa.encrypt.impl.AAAEncryptionServiceImpl;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev240202.AaaEncryptServiceConfig;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev240202.AaaEncryptServiceConfigBuilder;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev240202.EncryptServiceConfig;


public class GnmiSouthBoundModuleTest {

    private static final long MODULE_TIMEOUT = 60;
    private static final TimeUnit MODULE_TIME_UNIT = TimeUnit.SECONDS;

    @Test
    public void gnmiModuleSmokeTest() throws Exception {
        // Build and start the controller
        final LightyController services = new LightyControllerBuilder()
            .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(GnmiConfigUtils.YANG_MODELS))
            .build();
        Assertions.assertTrue(services.start().get());

        final GnmiSouthboundModule gnmiModule = new GnmiSouthboundModule(services.getServices().getBindingDataBroker(),
                services.getServices().getRpcProviderService(), services.getServices().getDOMMountPointService(),
                createEncryptionService());
        gnmiModule.init();
        gnmiModule.close();
        Assertions.assertTrue(services.shutdown(MODULE_TIMEOUT, MODULE_TIME_UNIT));
    }

    @Test
    public void gnmiModuleStartFailedTest() throws Exception {
        final LightyController services = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(GnmiConfigUtils.YANG_MODELS)).build();
        Assertions.assertTrue(services.start().get());

        // Mock configuration with invalid YANG path
        final GnmiConfiguration badConfig = Mockito.mock(GnmiConfiguration.class);
        when(badConfig.getInitialYangsPaths()).thenReturn(List.of("invalid-path"));

        final GnmiSouthboundModule gnmiModule = new GnmiSouthboundModule(services.getServices().getBindingDataBroker(),
            services.getServices().getRpcProviderService(), services.getServices().getDOMMountPointService(),
            createEncryptionService());

        gnmiModule.init();
        Assertions.assertTrue(services.shutdown(MODULE_TIMEOUT, MODULE_TIME_UNIT));
    }

    /**
     * Creates the official ODL AAAEncryptionServiceImpl with a test configuration.
     */
    private static AAAEncryptionServiceImpl createEncryptionService() {
        final AaaEncryptServiceConfig config = new AaaEncryptServiceConfigBuilder()
            .setEncryptKey("V1S1ED4OMeEh")
            .setPasswordLength(12)
            .setEncryptSalt("TdtWeHbch/7xP52/rp3Usw==")
            .setEncryptMethod("PBKDF2WithHmacSHA1")
            .setEncryptType("AES")
            .setEncryptIterationCount(32768)
            .setEncryptKeyLength(128)
            .setAuthTagLength(128)
            .setCipherTransforms("AES/GCM/NoPadding")
            .build();

        // This constructor internally handles key derivation, IV setup, etc.
        return new AAAEncryptionServiceImpl((EncryptServiceConfig) config);
    }
}

