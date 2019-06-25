/*
 * Copyright (c) 2019 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.aaa.config;

import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.DatastoreConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigInteger;

public class DatastoreConfigurationConfigTest {

    @Test
    public void getDefaultTest() {
        DatastoreConfig config = DatastoreConfigurationConfig.getDefault();

        Assert.assertNotNull(config);

        Assert.assertEquals(config.getStore(), DatastoreConfig.Store.H2DataStore);
        Assert.assertEquals(config.getTimeToLive(), new BigInteger("36000"));
        Assert.assertEquals(config.getTimeToWait(), new BigInteger("3600"));
    }
}
