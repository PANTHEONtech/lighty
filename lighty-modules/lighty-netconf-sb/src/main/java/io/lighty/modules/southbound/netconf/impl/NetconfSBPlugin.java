/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl;

import io.lighty.core.controller.api.LightyModule;
import java.util.Optional;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

/**
 * Marker interface for NETCONF SBP.
 */
public interface NetconfSBPlugin extends LightyModule {

    /**
     * Indicates if this instance is clustered or not.
     * @return True if this instance of NETCONF SBP is clustered, false otherwise.
     */
    boolean isClustered();

    /**
     * Create an instance of {@link NetconfBaseService} for specific device (mount point).
     * @param nodeId Unique identifier of Netconf node in topology-netconf.
     * @return Instance of {@link NetconfBaseService} or empty if node is not found by nodeId.
     */
    Optional<NetconfBaseService> getNetconfBaseService(NodeId nodeId);

    /**
     * Create an instance of {@link NetconfNmdaBaseService} for specific device (mount point).
     * @param nodeId Unique identifier of Netconf node in topology-netconf.
     * @return Instance of {@link NetconfNmdaBaseService} or empty if node is not found by nodeId.
     */
    Optional<NetconfNmdaBaseService> getNetconfNmdaBaseService(NodeId nodeId);

}
