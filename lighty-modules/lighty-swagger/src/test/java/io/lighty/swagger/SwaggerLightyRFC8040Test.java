/*
 * Copyright (c) 2019 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.swagger;

import org.opendaylight.restconf.common.util.SimpleUriInfo;
import org.testng.annotations.Test;

public class SwaggerLightyRFC8040Test extends SwaggerLightyTest {

    private static final String SWAGGER2_BASE_URI = "http://localhost:8888/apidoc/swagger2/18/apis";
    private static final String OPENAPI3_BASE_URI = "http://localhost:8888/apidoc/openapi3/18/apis";

    @Test
    public void simpleSwaggerModuleTest() {
        super.simpleSwaggerModuleTest();
    }

    @Test
    public void testGetListOfMountsSwagger2() {
        super.testGetListOfMounts(new SimpleUriInfo(SWAGGER2_BASE_URI + "/mounts"));
    }

    @Test
    public void testGetListOfMountsOpenApi3() {
        super.testGetListOfMounts(new SimpleUriInfo(OPENAPI3_BASE_URI + "/mounts"));
    }

    @Test
    public void testGetAllModulesDocSwagger2() {
        super.testGetAllModulesDoc(mockUriInfo(SWAGGER2_BASE_URI + "/single"));
    }

    @Test
    public void testGetAllModulesDocOpenApi3() {
        super.testGetAllModulesDoc(mockUriInfo(OPENAPI3_BASE_URI + "/single"));
    }

    @Test
    public void testGetDocByModuleSwagger2() {
        String path = SWAGGER2_BASE_URI + "/" + DEFAULT_MODEL_NAME + "(" + DEFAULT_REVISION_DATE + ")";
        super.testGetDocByModule(mockUriInfo(path), DEFAULT_MODEL_NAME, DEFAULT_REVISION_DATE);
    }

    @Test
    public void testGetDocByModuleOpenApi3() {
        String path = OPENAPI3_BASE_URI + "/" + DEFAULT_MODEL_NAME + "(" + DEFAULT_REVISION_DATE + ")";
        super.testGetDocByModule(mockUriInfo(path), DEFAULT_MODEL_NAME, DEFAULT_REVISION_DATE);
    }

    @Test
    public void testGetApiExplorerSwagger2() {
        super.testGetApiExplorer(new SimpleUriInfo(SWAGGER2_BASE_URI + "/ui"));
    }

    @Test
    public void testGetApiExplorerOpenApi3() {
        super.testGetApiExplorer(new SimpleUriInfo(OPENAPI3_BASE_URI + "/ui"));
    }
}
