/*
 * Copyright (c) 2019 Pantheon.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.swagger;

import io.lighty.modules.northbound.restconf.community.impl.config.JsonRestConfServiceType;
import org.testng.annotations.Test;

public class SwaggerLightyDRAFT02Test extends SwaggerLightyTest {

    protected SwaggerLightyDRAFT02Test() {
        super(JsonRestConfServiceType.DRAFT_02);
    }

    @Test
    public void simpleSwaggerModuleTest() {
        super.simpleSwaggerModuleTest();
    }

    @Test
    public void testGetListOfMounts() {
        super.testGetListOfMounts();
    }

    @Test
    public void testGetRootDoc() {
        super.testGetRootDoc();
    }

    @Test
    public void testGetDocByModule() {
        super.testGetDocByModule();
    }

    @Test
    public void testGetApiExplorer() {
        super.testGetApiExplorer();
    }
}
