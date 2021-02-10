/*
 * Copyright © 2021 Pantheon Technologies, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.northbound.restconf.community.impl.root.resource.discovery;

import java.util.Set;
import org.opendaylight.restconf.nb.rfc8040.rests.services.api.RootResourceDiscoveryService;

// FIXME remove once the upstream class is fixed
public class RootFoundApplication extends org.opendaylight.restconf.nb.rfc8040.RootFoundApplication {
    private final RootResourceDiscoveryService rrds;

    public RootFoundApplication(final String restconfServletContextPath) {
        this.rrds = new RootResourceDiscoveryServiceImpl(restconfServletContextPath);
    }

    @Override
    public Set<Object> getSingletons() {
        return Set.of(rrds);
    }
}
