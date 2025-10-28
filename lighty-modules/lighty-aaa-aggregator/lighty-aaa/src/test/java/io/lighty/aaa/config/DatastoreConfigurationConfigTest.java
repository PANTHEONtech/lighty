/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.aaa.config;

import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.DatastoreConfig;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DatastoreConfigurationConfigTest {

    @Test
    public void getDefaultTest() {
        DatastoreConfig config = DatastoreConfigurationConfig.getDefault();

        Assert.assertNotNull(config);

        Assert.assertEquals(config.getStore(), DatastoreConfig.Store.H2DataStore);
        Assert.assertEquals(config.getTimeToLive(), Uint64.valueOf(36000));
        Assert.assertEquals(config.getTimeToWait(), Uint64.valueOf(3600));
    }
}
