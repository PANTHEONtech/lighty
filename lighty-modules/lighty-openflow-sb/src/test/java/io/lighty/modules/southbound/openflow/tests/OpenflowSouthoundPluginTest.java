/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the LIGHTY.IO LICENSE,
 * version 1.1. If a copy of the license was not distributed with this file,
 * You can obtain one at https://lighty.io/license/1.1/
 */
package io.lighty.modules.southbound.openflow.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

public class OpenflowSouthoundPluginTest extends OpenflowSouthboundPluginTestBase {

    @Test(timeOut = 60_000)
    public void testStartOfpPlugin() {
        Assert.assertNotNull(getLightyController());
        Assert.assertNotNull(getCommunityRestConf());
        Assert.assertNotNull(getOpenflowSouthboundPlugin());
    }
}