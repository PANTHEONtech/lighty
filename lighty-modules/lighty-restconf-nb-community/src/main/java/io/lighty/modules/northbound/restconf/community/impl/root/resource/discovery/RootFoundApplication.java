/*
 * Copyright © 2021 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.northbound.restconf.community.impl.root.resource.discovery;

import java.util.Set;
import javax.ws.rs.core.Application;
import org.opendaylight.restconf.nb.jaxrs.JaxRsWebHostMetadata;
import org.opendaylight.restconf.nb.rfc8040.legacy.RestconfDocumentedExceptionMapper;

public class RootFoundApplication extends Application {
    private final JaxRsWebHostMetadata rrds;

    public RootFoundApplication(final String restconfServletContextPath) {
        this.rrds = new JaxRsWebHostMetadata(restconfServletContextPath);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(RestconfDocumentedExceptionMapper.class);
    }

    @Override
    public Set<Object> getSingletons() {
        return Set.of(rrds);
    }
}
