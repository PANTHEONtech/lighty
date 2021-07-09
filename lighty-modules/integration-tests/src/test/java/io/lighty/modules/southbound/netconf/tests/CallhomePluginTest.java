/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class CallhomePluginTest {

    private static final Logger LOG = LoggerFactory.getLogger(CallhomePluginTest.class);
    public static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000;
    public static final long SLEEP_AFTER_SHUTDOWN_TIMEOUT_MILLIS = 3_000;

    private LightyController lightyController;
    private CommunityRestConf restConf;
    private LightyModule netconfPlugin;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        lightyController = LightyTestUtils.startController(NetconfConfigUtils.NETCONF_CALLHOME_MODELS);
        RestConfConfiguration restConfConfig =
                RestConfConfigUtils.getDefaultRestConfConfiguration();
        restConf = LightyTestUtils.startRestconf(restConfConfig, lightyController.getServices());
        final NetconfConfiguration configuration =
                NetconfConfigUtils.createDefaultNetconfConfiguration();
        NetconfConfigUtils.injectServicesToTopologyConfig(configuration, lightyController.getServices());
        netconfPlugin = NetconfCallhomePluginBuilder.from(configuration, lightyController.getServices()).build();
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    @AfterClass
    public void afterClass() {
        if (netconfPlugin != null) {
            LOG.info("Shutting down Netconf topology Plugin");
            try {
                netconfPlugin.shutdown().get(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                LOG.error("Shutdown of Netconf topology Plugin failed", e);
            }
        }
        if (restConf != null) {
            LOG.info("Shutting down CommunityRestConf");
            try {
                restConf.shutdown().get(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                Thread.sleep(SLEEP_AFTER_SHUTDOWN_TIMEOUT_MILLIS);
            } catch (InterruptedException e) {
                LOG.error("Interrupted while shutting down CommunityRestConf", e);
            } catch (TimeoutException e) {
                LOG.error("Timeout while shutting down Lighty Swagger", e);
            } catch (ExecutionException e) {
                LOG.error("Execution of CommunityRestConf shutdown failed", e);
            }
        }
        if (lightyController != null) {
            LOG.info("Shutting down LightyController");
            try {
                lightyController.shutdown().get(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                Thread.sleep(SLEEP_AFTER_SHUTDOWN_TIMEOUT_MILLIS);
            } catch (Exception e) {
                LOG.error("Shutdown of LightyController failed", e);
            }
        }
    }

    @Test
    public void testStart() throws Exception {
        netconfPlugin.start().get();
        // check, whether TCP server is running on port
        try (Socket socket = new Socket()) {
            final SocketAddress endpoint = new InetSocketAddress(InetAddress.getLocalHost(), 6666);
            socket.connect(endpoint);
        }
    }

}
