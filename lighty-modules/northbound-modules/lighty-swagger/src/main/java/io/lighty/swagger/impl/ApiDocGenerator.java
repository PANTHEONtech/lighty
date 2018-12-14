/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.swagger.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.netconf.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.netconf.sal.rest.doc.swagger.ResourceList;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import javax.ws.rs.core.UriInfo;

/**
 * This class gathers all YANG-defined Modules and
 * generates Swagger compliant documentation.
 */
public class ApiDocGenerator extends BaseYangSwaggerGenerator {

    private static final ApiDocGenerator INSTANCE = new ApiDocGenerator();
    private SchemaService schemaService;

    public ResourceList getResourceListing(final UriInfo uriInfo) {
        Preconditions.checkState(schemaService != null);
        final SchemaContext schemaContext = schemaService.getGlobalContext();
        Preconditions.checkState(schemaContext != null);
        return super.getResourceListing(uriInfo, schemaContext, "");
    }

    public ApiDeclaration getApiDeclaration(final String module, final String revision, final UriInfo uriInfo) {
        final SchemaContext schemaContext = schemaService.getGlobalContext();
        Preconditions.checkState(schemaContext != null);
        return super.getApiDeclaration(module, revision, uriInfo, schemaContext, "");
    }

    /**
     * Returns singleton instance.
     */
    public static ApiDocGenerator getInstance() {
        return INSTANCE;
    }

    public void setDraft(final boolean newDraft) {
        super.setDraft(newDraft);
    }

    public void setSchemaService(final SchemaService schemaService) {
        this.schemaService = schemaService;
    }
}
