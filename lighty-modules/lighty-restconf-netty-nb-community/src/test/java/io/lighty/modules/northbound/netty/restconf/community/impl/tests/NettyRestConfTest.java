/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.netty.restconf.community.impl.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

public class NettyRestConfTest extends NettyRestConfTestBase {

    @Test
    public void simpleRestconfTest() {
        Assert.assertNotNull(getLightyController());
        Assert.assertNotNull(getNettyRestConf());
    }

}
