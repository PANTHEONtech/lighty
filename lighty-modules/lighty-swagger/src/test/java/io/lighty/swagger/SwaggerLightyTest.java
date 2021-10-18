/*
 * Copyright (c) 2019 Pantheon.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.swagger;

import io.lighty.modules.northbound.restconf.community.impl.config.JsonRestConfServiceType;
import java.net.URI;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.mockito.Mockito;
import org.testng.Assert;

/**
 * Base class for lighty-swagger tests for different versions of {@link JsonRestConfServiceType}.
 */
public abstract class SwaggerLightyTest extends SwaggerLightyTestBase {

    protected static final String DEFAULT_MODEL_NAME = "ietf-yang-library";
    protected static final String DEFAULT_REVISION_DATE = "2019-01-04";

    protected SwaggerLightyTest(JsonRestConfServiceType restConfServiceType) {
        super(restConfServiceType);
    }

    public void simpleSwaggerModuleTest() {
        Assert.assertNotNull(getLightyController());
        Assert.assertNotNull(getSwaggerModule());
    }

    public void testGetListOfMounts(UriInfo uriInfo) {
        assertSuccessResponse(getSwaggerModule().getApiDocService().getListOfMounts(uriInfo));
    }

    public void testGetAllModulesDoc(UriInfo uriInfo) {
        assertSuccessResponse(getSwaggerModule().getApiDocService().getAllModulesDoc(uriInfo));
    }

    public void testGetDocByModule(UriInfo uriInfo, String modelName, String revisionDate) {
        assertSuccessResponse(getSwaggerModule().getApiDocService().getDocByModule(modelName, revisionDate, uriInfo));
    }

    public void testGetApiExplorer(UriInfo uriInfo) {
        final Response response = getSwaggerModule().getApiDocService().getApiExplorer(uriInfo);

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
        Mockito.when(uriInfo.getRequestUriBuilder()).thenReturn(UriBuilder.fromUri(absolutePathUri));

        return uriInfo;
    }
}
