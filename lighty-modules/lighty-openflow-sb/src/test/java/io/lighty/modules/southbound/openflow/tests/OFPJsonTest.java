/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.openflow.tests;

import io.lighty.modules.southbound.openflow.impl.config.OpenflowpluginConfiguration;
import io.lighty.modules.southbound.openflow.impl.util.OpenflowConfigUtils;
import java.io.InputStream;
import org.testng.Assert;
import org.testng.annotations.Test;

public class OFPJsonTest {

    @Test
    public void testDefaultConfig() {
        OpenflowpluginConfiguration ofpDefaultConfig = OpenflowConfigUtils.getDefaultOfpConfiguration();
        Assert.assertNotNull(ofpDefaultConfig);
        Assert.assertEquals(ofpDefaultConfig.getBarrierCountLimit(), 25600);
        Assert.assertEquals(ofpDefaultConfig.getSwitchConfig().getPort(), 6653);
        Assert.assertEquals(ofpDefaultConfig.getSwitchConfig().getSwitchIdleTimeout().longValue(), 15000L);
    }

    @Test
    public void testJSONConfig() throws Exception {
        final InputStream ofConfig = this.getClass().getClassLoader().getResourceAsStream("ofpConfig.json");

        OpenflowpluginConfiguration ofpJSONConfig = OpenflowConfigUtils.getOfpConfiguration(ofConfig);

        Assert.assertNotNull(ofpJSONConfig);
        Assert.assertEquals(ofpJSONConfig.getBarrierCountLimit(), 1234);
        Assert.assertEquals(ofpJSONConfig.getSwitchConfig().getPort(), 1234);
        Assert.assertEquals(ofpJSONConfig.getSwitchConfig().getSwitchIdleTimeout().longValue(), 15001L);
    }
}
