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
 * Base class for lighty-swagger tests for different versions of {@link JsonRestConfServiceType}
 */
public abstract class SwaggerLightyTest extends SwaggerLightyTestBase {

    private String modelName;
    private String revisionDate;
    private UriInfo uriInfo;

    protected SwaggerLightyTest(JsonRestConfServiceType restConfServiceType) {
        super(restConfServiceType);

        final String basePath = "http://localhost:8888/apidoc/18/apis/";
        modelName = "ietf-yang-library";
        revisionDate = "2016-06-21";
        final URI absolutePath = URI.create(
                "http://localhost:8888/apidoc/18/explorer/index.html#/" + modelName + "(" + revisionDate + ")");

        uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getAbsolutePath()).thenReturn(absolutePath);
        Mockito.when(uriInfo.getBaseUri()).thenReturn(URI.create(basePath));
        Mockito.when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromPath(basePath));
        Mockito.when(uriInfo.getRequestUriBuilder()).thenReturn(UriBuilder.fromUri(absolutePath));
    }

    public void simpleSwaggerModuleTest() {
        Assert.assertNotNull(getLightyController());
        Assert.assertNotNull(getSwaggerModule());
    }

    public void testGetListOfMounts() {

        final Response response = getSwaggerModule().getApiDocService().getListOfMounts(uriInfo);

        Assert.assertEquals(200, response.getStatus());
    }

    public void testGetMountRootDoc() {
    }

    public void testGetMountDocByModule() {
    }

    public void testGetRootDoc() {

        final Response response = getSwaggerModule().getApiDocService().getRootDoc(uriInfo);

        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertNotNull(response.getEntity());
    }

    public void testGetDocByModule() {

        final Response response = getSwaggerModule().getApiDocService().getDocByModule(modelName, revisionDate, uriInfo);

        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertNotNull(response.getEntity());
    }

    public void testGetApiExplorer() {

        final Response response = getSwaggerModule().getApiDocService().getApiExplorer(uriInfo);

        final int redirectCode = 303;
        Assert.assertEquals(response.getStatus(), redirectCode);
    }
}
