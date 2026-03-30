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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RestConfConfigurationTest {

    @Test
    void testRestConfConfiguration() {
        RestConfConfiguration defaultRestConfConfiguration = RestConfConfigUtils.getDefaultRestConfConfiguration();
        RestConfConfiguration restConfConfiguration = new RestConfConfiguration(defaultRestConfConfiguration);

        Assertions.assertEquals(defaultRestConfConfiguration, restConfConfiguration);
        Assertions.assertTrue(defaultRestConfConfiguration.hashCode() == restConfConfiguration.hashCode());

        restConfConfiguration.setHttpPort(3333);
        restConfConfiguration.setInetAddress(InetAddress.getLoopbackAddress());

        Assertions.assertNotEquals(defaultRestConfConfiguration, restConfConfiguration);
        Assertions.assertFalse(defaultRestConfConfiguration.hashCode() == restConfConfiguration.hashCode());
    }

    @Test
    void testRestConfConfigurationUtilsLoadFromStrem() throws ConfigurationException {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("restconf-config.json");
        RestConfConfiguration restConfConfiguration = RestConfConfigUtils.getRestConfConfiguration(resourceAsStream);
        Assertions.assertNotNull(restConfConfiguration);
        Assertions.assertTrue(restConfConfiguration.getHttpPort() == 5555);
        Assertions.assertEquals(restConfConfiguration.getInetAddress().getHostAddress(), "127.0.0.3");
    }

}
