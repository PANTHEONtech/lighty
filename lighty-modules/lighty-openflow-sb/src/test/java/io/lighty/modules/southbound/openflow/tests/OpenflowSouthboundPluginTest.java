/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.openflow.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

public class OpenflowSouthboundPluginTest extends OpenflowSouthboundPluginTestBase {

    @Test(timeOut = 60_000)
    public void testStartOfpPlugin() {
        Assert.assertNotNull(getLightyController());
        Assert.assertNotNull(getCommunityRestConf());
        Assert.assertNotNull(getOpenflowSouthboundPlugin());
    }
}