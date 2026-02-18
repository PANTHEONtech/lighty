/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.northbound.restconf.community.impl.tests;

import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import java.io.InputStream;
import java.net.InetAddress;
import org.testng.Assert;
import org.testng.annotations.Test;

class RestConfConfigurationTest {

    @Test
    void testRestConfConfiguration() {
        RestConfConfiguration defaultRestConfConfiguration = RestConfConfigUtils.getDefaultRestConfConfiguration();
        RestConfConfiguration restConfConfiguration = new RestConfConfiguration(defaultRestConfConfiguration);

        Assert.assertEquals(defaultRestConfConfiguration, restConfConfiguration);
        Assert.assertTrue(defaultRestConfConfiguration.hashCode() == restConfConfiguration.hashCode());

        restConfConfiguration.setHttpPort(3333);
        restConfConfiguration.setInetAddress(InetAddress.getLoopbackAddress());

        Assert.assertNotEquals(defaultRestConfConfiguration, restConfConfiguration);
        Assert.assertFalse(defaultRestConfConfiguration.hashCode() == restConfConfiguration.hashCode());
    }

    @Test
    void testRestConfConfigurationUtilsLoadFromStrem() throws ConfigurationException {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("restconf-config.json");
        RestConfConfiguration restConfConfiguration = RestConfConfigUtils.getRestConfConfiguration(resourceAsStream);
        Assert.assertNotNull(restConfConfiguration);
        Assert.assertTrue(restConfConfiguration.getHttpPort() == 5555);
        Assert.assertEquals(restConfConfiguration.getInetAddress().getHostAddress(), "127.0.0.3");
    }

}
