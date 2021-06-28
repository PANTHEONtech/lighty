/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.device.session.security;

import io.lighty.modules.southbound.security.Security;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiNode;

public interface GnmiSecurityProvider {

    /**
     * Based on parameters provided in gnmiNode return the Security instance.
     * <br>
     * For example if gnmiNode contains path to certificates, load them from that path
     * <br>
     * If user specified ID of certificates which he uploaded via RPC before, load them.
     * @param gnmiNode node for which we are creating Security
     * @return security instance
     * @throws SessionSecurityException if something went wrong while creating Security (e.g no certificates found..)
     */
    Security getSecurity(GnmiNode gnmiNode) throws SessionSecurityException;
}
