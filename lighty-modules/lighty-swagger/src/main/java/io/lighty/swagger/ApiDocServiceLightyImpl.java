/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.swagger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.opendaylight.netconf.sal.rest.doc.api.ApiDocService;
import org.opendaylight.netconf.sal.rest.doc.impl.ApiDocServiceImpl;
import org.opendaylight.netconf.sal.rest.doc.mountpoints.MountPointSwagger;
import org.opendaylight.netconf.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.netconf.sal.rest.doc.swagger.ResourceList;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class ApiDocServiceLightyImpl implements ApiDocService {

    private final MountPointSwagger mountPointSwagger;

    public ApiDocServiceLightyImpl(MountPointSwagger mountPointSwagger) {
        this.mountPointSwagger = mountPointSwagger;
    }

    @Override
    public synchronized Response getListOfMounts(final UriInfo uriInfo) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter streamWriter = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             JsonGenerator writer = new JsonFactory().createGenerator(streamWriter)) {
            writer.writeStartArray();
            for (final Map.Entry<String, Long> entry : mountPointSwagger.getInstanceIdentifiers()
                    .entrySet()) {
                writer.writeStartObject();
                writer.writeObjectField("instance", entry.getKey());
                writer.writeObjectField("id", entry.getValue());
                writer.writeEndObject();
            }
            writer.writeEndArray();
            writer.flush();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        try {
            String responseStr = baos.toString(StandardCharsets.UTF_8.name());
            return Response.status(Response.Status.OK).entity(responseStr).build();
        } catch (UnsupportedEncodingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @Override
    public synchronized Response getMountRootDoc(final String instanceNum, final UriInfo uriInfo) {
        final ResourceList resourceList;
        resourceList = mountPointSwagger.getResourceList(uriInfo, Long.parseLong(instanceNum), ApiDocServiceImpl.URIType.RFC8040);
        return Response.ok(resourceList).build();
    }

    @Override
    public synchronized Response getMountDocByModule(final String instanceNum, final String module,
                                                     final String revision, final UriInfo uriInfo) {
        final ApiDeclaration api;
        api = mountPointSwagger.getMountPointApi(uriInfo, Long.parseLong(instanceNum), module, revision, ApiDocServiceImpl.URIType.RFC8040);
        return Response.ok(api).build();
    }

}
