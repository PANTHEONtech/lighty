/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.northbound.restconf.community.impl.tests;

import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.northbound.restconf.community.impl.config.JsonRestConfServiceType;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import java.io.InputStream;
import java.net.InetAddress;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RestConfConfigurationTest {

    @Test
    public void testRestConfConfiguration() {
        RestConfConfiguration defaultRestConfConfiguration = RestConfConfigUtils.getDefaultRestConfConfiguration();
        RestConfConfiguration restConfConfiguration = new RestConfConfiguration(defaultRestConfConfiguration);

        Assert.assertEquals(defaultRestConfConfiguration, restConfConfiguration);
        Assert.assertTrue(defaultRestConfConfiguration.hashCode() == restConfConfiguration.hashCode());

        restConfConfiguration.setHttpPort(3333);
        restConfConfiguration.setJsonRestconfServiceType(JsonRestConfServiceType.DRAFT_02);
        restConfConfiguration.setInetAddress(InetAddress.getLoopbackAddress());
        restConfConfiguration.setWebSocketPort(4444);

        Assert.assertNotEquals(defaultRestConfConfiguration, restConfConfiguration);
        Assert.assertFalse(defaultRestConfConfiguration.hashCode() == restConfConfiguration.hashCode());
    }

    @Test
    public void testRestConfConfigurationUtilsLoadFromStrem() throws ConfigurationException {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("restconf-config.json");
        RestConfConfiguration restConfConfiguration = RestConfConfigUtils.getRestConfConfiguration(resourceAsStream);
        Assert.assertNotNull(restConfConfiguration);
        Assert.assertTrue(restConfConfiguration.getHttpPort() == 5555);
        Assert.assertTrue(restConfConfiguration.getWebSocketPort() == 4444);
        Assert.assertEquals(restConfConfiguration.getJsonRestconfServiceType(), JsonRestConfServiceType.DRAFT_18);
        Assert.assertEquals(restConfConfiguration.getInetAddress().getHostAddress(), "127.0.0.3");
    }

}
