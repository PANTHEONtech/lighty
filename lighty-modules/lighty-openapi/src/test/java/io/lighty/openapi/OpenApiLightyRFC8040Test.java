/*
 * Copyright (c) 2019 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.openapi;

import java.io.IOException;
import org.testng.annotations.Test;

public class OpenApiLightyRFC8040Test extends OpenApiLightyTest {

    private static final String OPENAPI_BASE_URI = "http://localhost:8888/openapi";

    @Test
    public void simpleOpenApiModuleTest() {
        super.simpleOpenApiModuleTest();
    }


    @Test
    public void testGetListOfMountsOpenApi3() {
        super.testGetListOfMounts(mockUriInfo(OPENAPI_BASE_URI + "/api/v3/mounts/1"));
    }


    @Test
    public void testGetAllModulesDocOpenApi3() throws IOException {
        super.testGetAllModulesDoc(mockUriInfo(OPENAPI_BASE_URI + "/api/v3/single"));
    }


    @Test
    public void testGetDocByModuleOpenApi3() throws IOException {
        String path = OPENAPI_BASE_URI + "/api/v3/" + DEFAULT_MODEL_NAME + "(" + DEFAULT_REVISION_DATE + ")";
        super.testGetDocByModule(mockUriInfo(path), DEFAULT_MODEL_NAME, DEFAULT_REVISION_DATE);
    }


    @Test
    public void testGetApiExplorerOpenApi3() {
        super.testGetApiExplorer(mockUriInfo(OPENAPI_BASE_URI + "/explorer/index.html"));
    }
}
