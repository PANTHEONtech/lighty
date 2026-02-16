/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.netty.restconf.community.impl.tests;

import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.northbound.netty.restconf.community.impl.config.NettyRestConfConfiguration;
import io.lighty.modules.northbound.netty.restconf.community.impl.util.NettyRestConfUtils;
import java.net.InetAddress;
import org.testng.Assert;
import org.testng.annotations.Test;

class NettyRestConfConfigurationTest {

    @Test
    void testNettyRestConfConfiguration() {
        final var defaultRestConfConfiguration = NettyRestConfUtils.getDefaultNettyRestConfConfiguration();
        final var restConfConfiguration = new NettyRestConfConfiguration(defaultRestConfConfiguration);

        Assert.assertEquals(defaultRestConfConfiguration, restConfConfiguration);
        Assert.assertEquals(restConfConfiguration.hashCode(), defaultRestConfConfiguration.hashCode());

        restConfConfiguration.setHttpPort(3333);
        restConfConfiguration.setInetAddress(InetAddress.getLoopbackAddress());

        Assert.assertNotEquals(defaultRestConfConfiguration, restConfConfiguration);
        Assert.assertNotEquals(restConfConfiguration.hashCode(), defaultRestConfConfiguration.hashCode());
    }

    @Test
    void testNettyRestConfConfigurationUtilsLoadFromStream() throws ConfigurationException {
        final var resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("restconf-config.json");
        final var restConfConfiguration = NettyRestConfUtils.getNettyRestConfConfiguration(resourceAsStream);
        Assert.assertNotNull(restConfConfiguration);
        Assert.assertEquals(restConfConfiguration.getHttpPort(), 5555);
        Assert.assertEquals(restConfConfiguration.getInetAddress().getHostAddress(), "127.0.0.3");
    }

}
