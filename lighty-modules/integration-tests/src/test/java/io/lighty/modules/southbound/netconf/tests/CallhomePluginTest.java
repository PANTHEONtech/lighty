/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.modules.southbound.netconf.tests;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.southbound.netconf.impl.NetconfCallhomePluginBuilder;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

@Test(enabled = false)
public class CallhomePluginTest {

    private static final Logger LOG = LoggerFactory.getLogger(CallhomePluginTest.class);
    private LightyController lightyController;
    private CommunityRestConf restConf;
    private LightyModule netconfPlugin;

    @BeforeClass
    public void beforeClass() throws IOException, ConfigurationException {
        lightyController = LightyTestUtils.startController(NetconfConfigUtils.NETCONF_CALLHOME_MODELS);
        restConf = LightyTestUtils.startRestconf(lightyController.getServices());
        final NetconfConfiguration configuration =
                NetconfConfigUtils.createDefaultNetconfConfiguration();
        NetconfConfigUtils.injectServicesToTopologyConfig(configuration, lightyController.getServices());
        netconfPlugin = new NetconfCallhomePluginBuilder().from(configuration, lightyController.getServices()).build();
    }

    @AfterClass
    public void afterClass() throws Exception {
        if (netconfPlugin != null) {
            LOG.info("Shutting down Netconf topology Plugin");
            final ListenableFuture<Boolean> shutdown = netconfPlugin.shutdown();
            shutdown.get();
        }
        if (restConf != null) {
            LOG.info("Shutting down CommunityRestConf");
            final ListenableFuture<Boolean> shutdown = restConf.shutdown();
            shutdown.get();
            Thread.sleep(5_000);
        }
        if (lightyController != null) {
            LOG.info("Shutting down LightyController");
            final ListenableFuture<Boolean> shutdown = lightyController.shutdown();
            shutdown.get();
            Thread.sleep(10_000);
        }
    }

    @Test(enabled = false)
    public void testStart() throws Exception {
        netconfPlugin.start().get();
        // check, whether TCP server is running on port
        final Socket socket = new Socket();
        final SocketAddress endpoint = new InetSocketAddress(InetAddress.getLocalHost(), 6666);
        socket.connect(endpoint);
    }

}
