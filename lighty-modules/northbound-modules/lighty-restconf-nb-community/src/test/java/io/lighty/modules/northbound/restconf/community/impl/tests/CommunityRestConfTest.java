/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.modules.northbound.restconf.community.impl.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CommunityRestConfTest extends CommunityRestConfTestBase {

    @Test
    public void simpleRestconfTest() {
        Assert.assertNotNull(getLightyController());
        Assert.assertNotNull(getCommunityRestConf());
    }

}
