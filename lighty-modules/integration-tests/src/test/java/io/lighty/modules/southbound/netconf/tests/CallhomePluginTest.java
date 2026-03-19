/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.tests;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.modules.southbound.netconf.impl.NetconfCallhomePluginBuilder;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CallhomePluginTest {
    public static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000;

    private static LightyController lightyController;
    private static CommunityRestConf restConf;
    private static LightyModule netconfPlugin;

    @BeforeAll
    static void beforeClass() throws ConfigurationException {
        lightyController = LightyTestUtils.startController(NetconfConfigUtils.NETCONF_CALLHOME_MODELS);
        RestConfConfiguration restConfConfig =
                RestConfConfigUtils.getDefaultRestConfConfiguration();
        restConf = LightyTestUtils.startRestconf(restConfConfig, lightyController.getServices());
        final NetconfConfiguration configuration =
                NetconfConfigUtils.createDefaultNetconfConfiguration();
        NetconfConfigUtils.injectServicesToTopologyConfig(configuration, lightyController.getServices());
        netconfPlugin = NetconfCallhomePluginBuilder.from(configuration, lightyController.getServices(),
                restConfConfig.getInetAddress().getHostAddress(), 4334).build();
    }

    @AfterAll
    static void afterClass() {
        if (netconfPlugin != null) {
            netconfPlugin.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
        if (restConf != null) {
            restConf.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
        if (lightyController != null) {
            lightyController.shutdown(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    void testStart() throws Exception {
        netconfPlugin.start().get();
        // check, whether TCP server is running on port
        try (Socket socket = new Socket()) {
            final SocketAddress endpoint = new InetSocketAddress(
                    RestConfConfigUtils.getDefaultRestConfConfiguration().getInetAddress(), 4334);
            socket.connect(endpoint);
        }
    }
}