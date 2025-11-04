/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.gnmi.southbound.lightymodule;

import static org.mockito.Mockito.when;

import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.aaa.encrypt.impl.AAAEncryptionServiceImpl;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev240202.AaaEncryptServiceConfig;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev240202.AaaEncryptServiceConfigBuilder;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev240202.EncryptServiceConfig;


public class GnmiSouthBoundModuleTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private RpcProviderService rpcProviderService;
    @Mock
    private DOMMountPointService mountPointService;

    private static final long MODULE_TIMEOUT = 60;
    private static final TimeUnit MODULE_TIME_UNIT = TimeUnit.SECONDS;

    @Test
    public void gnmiModuleSmokeTest() throws Exception {
        // Build and start the controller

        final GnmiSouthboundModule gnmiModule = new GnmiSouthboundModule(dataBroker, rpcProviderService,
            mountPointService, createEncryptionService());
        gnmiModule.init();
        gnmiModule.close();
    }

    @Test
    public void gnmiModuleStartFailedTest() throws Exception {

        // Mock configuration with invalid YANG path
        final GnmiConfiguration badConfig = Mockito.mock(GnmiConfiguration.class);
        when(badConfig.getInitialYangsPaths()).thenReturn(List.of("invalid-path"));

        final GnmiSouthboundModule gnmiModule = new GnmiSouthboundModule(dataBroker, rpcProviderService,
            mountPointService, createEncryptionService());

        gnmiModule.init();
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

