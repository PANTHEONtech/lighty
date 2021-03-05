/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.openflow.tests;

import org.testng.Assert;

public class OpenflowSouthboundPluginTest extends OpenflowSouthboundPluginTestBase {

//    @Test(timeOut = 60_000)
//    TODO: Allow when will be released OFP at version 0.12.0 or higher and serviceutils at 0.7.0 or higher
    public void testStartOfpPlugin() {
        Assert.assertNotNull(getLightyController());
        Assert.assertNotNull(getCommunityRestConf());
        Assert.assertNotNull(getOpenflowSouthboundPlugin());
    }
}