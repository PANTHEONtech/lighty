/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.netty.restconf.community.impl.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NettyRestConfTest extends NettyRestConfTestBase {

    @Test
    void simpleRestconfTest() {
        Assertions.assertNotNull(getLightyController());
        Assertions.assertNotNull(getNettyRestConf());
    }
}
