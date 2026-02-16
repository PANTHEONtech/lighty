/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.openapi;

import java.io.IOException;
import org.testng.annotations.Test;

class OpenApiLightyRFC8040Test extends OpenApiLightyTest {

    private static final String OPENAPI_BASE_URI = "http://localhost:8888/openapi";

    @Test
    void simpleOpenApiModuleTest() {
        super.simpleOpenApiModuleTest();
    }


    @Test
    void testGetListOfMountsOpenApi3() {
        super.testGetListOfMounts(mockUriInfo(OPENAPI_BASE_URI + "/api/v3/mounts/1"));
    }


    @Test
    void testGetAllModulesDocOpenApi3() throws IOException {
        super.testGetAllModulesDoc(mockUriInfo(OPENAPI_BASE_URI + "/api/v3/single"));
    }


    @Test
    void testGetDocByModuleOpenApi3() throws IOException {
        String path = OPENAPI_BASE_URI + "/api/v3/" + DEFAULT_MODEL_NAME + "(" + DEFAULT_REVISION_DATE + ")";
        super.testGetDocByModule(mockUriInfo(path), DEFAULT_MODEL_NAME, DEFAULT_REVISION_DATE);
    }


    @Test
    void testGetApiExplorerOpenApi3() {
        super.testGetApiExplorer(mockUriInfo(OPENAPI_BASE_URI + "/explorer/index.html"));
    }
}
