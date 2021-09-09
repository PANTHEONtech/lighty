/*
 * Copyright © 2021 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.northbound.restconf.community.impl.root.resource.discovery;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.opendaylight.restconf.nb.rfc8040.rests.services.api.RootResourceDiscoveryService;

@Path("/")
public final class RootResourceDiscoveryServiceImpl implements RootResourceDiscoveryService {

    private final String restconfServletContextPath;

    public RootResourceDiscoveryServiceImpl(final String restconfServletContextPath) {
        if (restconfServletContextPath.charAt(0) == '/') {
            this.restconfServletContextPath = restconfServletContextPath.substring(1);
        } else {
            this.restconfServletContextPath = restconfServletContextPath;
        }
    }

    @Override
    public Response readXrdData() {
        return Response.status(Status.OK)
                .entity("<?xml version='1.0' encoding='UTF-8'?>\n"
                        + "<XRD xmlns='http://docs.oasis-open.org/ns/xri/xrd-1.0'>\n"
                        + "     <Link rel='restconf' href='/" + restconfServletContextPath + "'/>\n"
                        + "</XRD>")
                .build();
    }

    @Override
    public Response readJsonData() {
        return Response.status(Status.OK)
                .entity("{\n"
                        + " \"links\" :\n"
                        + " {\n"
                        + "     \"rel\" : \"restconf\",\n"
                        + "     \"href\" : \"/" + restconfServletContextPath + "/\"\n"
                        + " }\n"
                        + "}")
                .build();
    }
}

