/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.swagger;

import org.opendaylight.netconf.sal.rest.doc.api.ApiDocService;
import org.opendaylight.netconf.sal.rest.doc.impl.ApiDocGeneratorRFC8040;
import org.opendaylight.netconf.sal.rest.doc.impl.ApiDocServiceImpl;
import org.opendaylight.netconf.sal.rest.doc.impl.MountPointSwaggerGeneratorRFC8040;
import org.opendaylight.netconf.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.netconf.sal.rest.doc.swagger.ResourceList;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Objects;

public class ApiDocServiceRFC8040 extends ApiDocServiceLightyImpl implements ApiDocService {

    private final ApiDocGeneratorRFC8040 apiDocGeneratorRFC8040;

    public ApiDocServiceRFC8040(MountPointSwaggerGeneratorRFC8040 mountPointSwaggerGeneratorRFC8040, ApiDocGeneratorRFC8040 apiDocGeneratorRFC8040) {
        super(Objects.requireNonNull(mountPointSwaggerGeneratorRFC8040).getMountPointSwagger());
        this.apiDocGeneratorRFC8040 = apiDocGeneratorRFC8040;
    }

    /**
     * Generates index document for Swagger UI. This document lists out all
     * modules with link to get APIs for each module. The API for each module is
     * served by <code> getDocByModule()</code> method.
     */
    @Override
    public synchronized Response getRootDoc(final UriInfo uriInfo) {
        final ResourceList rootDoc;
        rootDoc = apiDocGeneratorRFC8040.getResourceListing(uriInfo, ApiDocServiceImpl.URIType.RFC8040);
        return Response.ok(rootDoc).build();
    }

    /**
     * Generates Swagger compliant document listing APIs for module.
     */
    @Override
    public synchronized Response getDocByModule(final String module, final String revision, final UriInfo uriInfo) {
        final ApiDeclaration doc;
        doc = apiDocGeneratorRFC8040.getApiDeclaration(module, revision, uriInfo, ApiDocServiceImpl.URIType.RFC8040);
        return Response.ok(doc).build();
    }

    /**
     * Redirects to embedded swagger ui.
     */
    @Override
    public synchronized Response getApiExplorer(final UriInfo uriInfo) {
        return Response.seeOther(uriInfo.getBaseUriBuilder().path("../18/explorer/index.html").build()).build();
    }

}
