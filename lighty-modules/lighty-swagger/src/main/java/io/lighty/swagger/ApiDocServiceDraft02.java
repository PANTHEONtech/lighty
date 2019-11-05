/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.swagger;

import org.opendaylight.netconf.sal.rest.doc.api.ApiDocService;
import org.opendaylight.netconf.sal.rest.doc.impl.ApiDocGeneratorDraftO2;
import org.opendaylight.netconf.sal.rest.doc.impl.ApiDocServiceImpl;
import org.opendaylight.netconf.sal.rest.doc.impl.MountPointSwaggerGeneratorDraft02;
import org.opendaylight.netconf.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.netconf.sal.rest.doc.swagger.ResourceList;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Objects;

public class ApiDocServiceDraft02 extends ApiDocServiceLightyImpl implements ApiDocService {

    private final ApiDocGeneratorDraftO2 apiDocGeneratorDraft02;

    public ApiDocServiceDraft02(MountPointSwaggerGeneratorDraft02 mountPointSwaggerGeneratorDraft02, ApiDocGeneratorDraftO2 apiDocGeneratorDraft02) {
        super(Objects.requireNonNull(mountPointSwaggerGeneratorDraft02).getMountPointSwagger());
        this.apiDocGeneratorDraft02 = apiDocGeneratorDraft02;
    }

    /**
     * Generates index document for Swagger UI. This document lists out all
     * modules with link to get APIs for each module. The API for each module is
     * served by <code> getDocByModule()</code> method.
     */
    @Override
    public synchronized Response getRootDoc(final UriInfo uriInfo) {
        final ResourceList rootDoc;
        rootDoc = apiDocGeneratorDraft02.getResourceListing(uriInfo, ApiDocServiceImpl.URIType.DRAFT02);
        return Response.ok(rootDoc).build();
    }

    /**
     * Generates Swagger compliant document listing APIs for module.
     */
    @Override
    public synchronized Response getDocByModule(final String module, final String revision, final UriInfo uriInfo) {
        final ApiDeclaration doc;
        doc = apiDocGeneratorDraft02.getApiDeclaration(module, revision, uriInfo, ApiDocServiceImpl.URIType.DRAFT02);
        return Response.ok(doc).build();
    }

    /**
     * Redirects to embedded swagger ui.
     */
    @Override
    public synchronized Response getApiExplorer(final UriInfo uriInfo) {
        return Response.seeOther(uriInfo.getBaseUriBuilder().path("../explorer/index.html").build()).build();
    }

}
