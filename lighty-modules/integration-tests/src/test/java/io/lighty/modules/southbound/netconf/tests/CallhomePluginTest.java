/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
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
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
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
            Thread.sleep(3_000);
        }
        if (lightyController != null) {
            LOG.info("Shutting down LightyController");
            final ListenableFuture<Boolean> shutdown = lightyController.shutdown();
            shutdown.get();
            Thread.sleep(1_000);
        }
    }

    @Test
    public void testStart() throws Exception {
        netconfPlugin.start().get();
        // check, whether TCP server is running on port
        try (final Socket socket = new Socket()) {
            final SocketAddress endpoint = new InetSocketAddress(InetAddress.getLocalHost(), 6666);
            socket.connect(endpoint);
        }
    }

}
