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
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;


public class GnmiSouthBoundModuleTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private RpcProviderService rpcProviderService;
    @Mock
    private DOMMountPointService mountPointService;
    @Mock
    private AAAEncryptionService aaaEncryptionService;

    private static final long MODULE_TIMEOUT = 60;
    private static final TimeUnit MODULE_TIME_UNIT = TimeUnit.SECONDS;

    @Test
    public void gnmiModuleSmokeTest() throws Exception {
        // Build and start the controller

        final GnmiSouthboundModule gnmiModule = new GnmiSouthboundModule(dataBroker, rpcProviderService,
            mountPointService, aaaEncryptionService, null, null);
        gnmiModule.init();
        gnmiModule.close();
    }

    @Test
    public void gnmiModuleStartFailedTest() throws Exception {

        // Mock configuration with invalid YANG path
        final GnmiConfiguration badConfig = Mockito.mock(GnmiConfiguration.class);
        when(badConfig.getInitialYangsPaths()).thenReturn(List.of("invalid-path"));

        final GnmiSouthboundModule gnmiModule = new GnmiSouthboundModule(dataBroker, rpcProviderService,
            mountPointService, aaaEncryptionService, null, null);

        gnmiModule.init();
    }
}

