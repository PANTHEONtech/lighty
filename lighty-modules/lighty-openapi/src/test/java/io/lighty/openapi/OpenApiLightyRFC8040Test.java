/*
 * Copyright (c) 2019 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.openapi;

import org.testng.annotations.Test;

public class OpenApiLightyRFC8040Test extends OpenApiLightyTest {

    private static final String OPENAPI3_BASE_URI = "http://localhost:8888/apidoc/openapi3/apis";

    @Test
    public void simpleOpenApiModuleTest() {
        super.simpleOpenApiModuleTest();
    }


    @Test
    public void testGetListOfMountsOpenApi3() {
        super.testGetListOfMounts(mockUriInfo(OPENAPI3_BASE_URI + "/mounts"));
    }


    @Test
    public void testGetAllModulesDocOpenApi3() {
        super.testGetAllModulesDoc(mockUriInfo(OPENAPI3_BASE_URI + "/single"));
    }


    @Test
    public void testGetDocByModuleOpenApi3() {
        String path = OPENAPI3_BASE_URI + "/" + DEFAULT_MODEL_NAME + "(" + DEFAULT_REVISION_DATE + ")";
        super.testGetDocByModule(mockUriInfo(path), DEFAULT_MODEL_NAME, DEFAULT_REVISION_DATE);
    }


    @Test
    public void testGetApiExplorerOpenApi3() {
        super.testGetApiExplorer(mockUriInfo(OPENAPI3_BASE_URI + "/ui"));
    }
}