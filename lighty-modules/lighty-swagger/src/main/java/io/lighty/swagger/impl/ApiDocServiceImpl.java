/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.swagger.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.lighty.swagger.mountpoints.MountPointSwagger;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.netconf.sal.rest.doc.api.ApiDocService;
import org.opendaylight.netconf.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.netconf.sal.rest.doc.swagger.ResourceList;

/**
 * This service generates swagger (See
 * <a href="https://helloreverb.com/developers/swagger"
 * >https://helloreverb.com/developers/swagger</a>) compliant documentation for
 * RESTCONF APIs. The output of this is used by embedded Swagger UI.
 *
 * <p>
 * NOTE: These API's need to be synchronized due to bug 1198. Thread access to
 * the SchemaContext is not synchronized properly and thus you can end up with
 * missing definitions without this synchronization. There are likely otherways
 * to work around this limitation, but given that this API is a dev only tool
 * and not dependent UI, this was the fastest work around.
 */
public class ApiDocServiceImpl implements ApiDocService {

    private static final ApiDocService INSTANCE = new ApiDocServiceImpl();

    public static ApiDocService getInstance() {
        return INSTANCE;
    }

    // LIGHTY-CHANGE-START
    private static boolean isNew(final UriInfo uriInfo) {
        return true;
    }

    /**
     * Generates index document for Swagger UI. This document lists out all
     * modules with link to get APIs for each module. The API for each module is
     * served by <code> getDocByModule()</code> method.
     */
    @Override
    public synchronized Response getRootDoc(final UriInfo uriInfo) {
        final ApiDocGenerator generator = ApiDocGenerator.getInstance();
        generator.setDraft(isNew(uriInfo));
        final ResourceList rootDoc = generator.getResourceListing(uriInfo);

        return Response.ok(rootDoc).build();
    }

    /**
     * Generates Swagger compliant document listing APIs for module.
     */
    @Override
    public synchronized Response getDocByModule(final String module, final String revision, final UriInfo uriInfo) {
        final ApiDocGenerator generator = ApiDocGenerator.getInstance();
        generator.setDraft(isNew(uriInfo));
        final ApiDeclaration doc = generator.getApiDeclaration(module, revision, uriInfo);
        return Response.ok(doc).build();
    }

    /**
     * Redirects to embedded swagger ui.
     */
    @Override
    public synchronized Response getApiExplorer(final UriInfo uriInfo) {
        return Response.seeOther(uriInfo.getBaseUriBuilder().path("../explorer/index.html").build()).build();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public synchronized Response getListOfMounts(final UriInfo uriInfo) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter streamWriter = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            JsonGenerator writer = new JsonFactory().createGenerator(streamWriter);
            writer.writeStartArray();
            for (final Entry<String, Long> entry : MountPointSwagger.getInstance().getInstanceIdentifiers()
                    .entrySet()) {
                writer.writeStartObject();
                writer.writeObjectField("instance", entry.getKey());
                writer.writeObjectField("id", entry.getValue());
                writer.writeEndObject();
            }
            writer.writeEndArray();
            writer.flush();
            return Response.status(200).entity(baos.toString("UTF-8")).build();
        } catch (final IOException e) {
            return Response.status(500).entity(e.getMessage()).build();
        }
    }

    @Override
    public synchronized Response getMountRootDoc(final String instanceNum, final UriInfo uriInfo) {
        final ResourceList resourceList;
        if (isNew(uriInfo)) {
            resourceList = MountPointSwagger.getInstanceDraft18().getResourceList(uriInfo, Long.parseLong(instanceNum));
        } else {
            resourceList = MountPointSwagger.getInstance().getResourceList(uriInfo, Long.parseLong(instanceNum));
        }
        return Response.ok(resourceList).build();
    }

    @Override
    public synchronized Response getMountDocByModule(final String instanceNum, final String module,
                                                     final String revision, final UriInfo uriInfo) {
        final ApiDeclaration api;
        if (isNew(uriInfo)) {
            api = MountPointSwagger.getInstanceDraft18().getMountPointApi(uriInfo, Long.parseLong(instanceNum), module,
                    revision);
        } else {
            api = MountPointSwagger.getInstance().getMountPointApi(uriInfo, Long.parseLong(instanceNum), module,
                    revision);
        }
        return Response.ok(api).build();
    }
    // LIGHTY-CHANGE-END
}
