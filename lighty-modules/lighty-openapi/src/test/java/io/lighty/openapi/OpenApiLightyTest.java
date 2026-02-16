/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.openapi;

import java.io.IOException;
import java.net.URI;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.mockito.Mockito;
import org.testng.Assert;

public abstract class OpenApiLightyTest extends OpenApiLightyTestBase {

    protected static final String DEFAULT_MODEL_NAME = "ietf-yang-library";
    protected static final String DEFAULT_REVISION_DATE = "2019-01-04";


    void simpleOpenApiModuleTest() {
        Assert.assertNotNull(getLightyController());
        Assert.assertNotNull(getJaxRsOpenapi());
    }

    void testGetListOfMounts(UriInfo uriInfo) {
        assertSuccessResponse(getJaxRsOpenapi().getListOfMounts(uriInfo));
    }

    void testGetAllModulesDoc(UriInfo uriInfo) throws IOException {
        assertSuccessResponse(getJaxRsOpenapi().getAllModulesDoc(uriInfo, 0, 0, 0, 0));
    }

    void testGetDocByModule(UriInfo uriInfo, String modelName, String revisionDate) throws IOException {
        assertSuccessResponse(getJaxRsOpenapi().getDocByModule(modelName, revisionDate, uriInfo, 0, 0));
    }

    void testGetApiExplorer(UriInfo uriInfo) {
        final Response response = getJaxRsOpenapi().getApiExplorer(uriInfo);

        final int redirectCode = 303;
        Assert.assertEquals(response.getStatus(), redirectCode);
    }

    private void assertSuccessResponse(Response response) {
        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertNotNull(response.getEntity());
    }

    protected UriInfo mockUriInfo(String path) {
        URI absolutePathUri = URI.create(path);
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getAbsolutePath()).thenReturn(absolutePathUri);
        Mockito.when(uriInfo.getBaseUri()).thenReturn(URI.create(path));
        Mockito.when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromPath(path));
        Mockito.when(uriInfo.getRequestUri()).thenReturn(absolutePathUri);

        return uriInfo;
    }
}
