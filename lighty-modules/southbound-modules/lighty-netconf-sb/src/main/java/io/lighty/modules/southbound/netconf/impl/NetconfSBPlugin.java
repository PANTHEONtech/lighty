/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.modules.southbound.netconf.impl;

import io.lighty.core.controller.api.LightyModule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

import java.util.Optional;

/**
 * Marker interface for NETCONF SBP
 */
public interface NetconfSBPlugin extends LightyModule {

    /**
     * Indicates if this instance is clustered or not.
     * @return
     *   True if this instance of NETCONF SBP is clustered, false otherwise.
     */
    boolean isClustered();

    /**
     * Create an instance of {@link NetconfBaseService} for specific device (mount point)
     * @param nodeId
     *   Unique identifier of Netconf node in topology-netconf.
     * @return
     *   Instance of {@link NetconfBaseService} or empty if node is not found by nodeId.
     */
    Optional<NetconfBaseService> getNetconfBaseService(NodeId nodeId);

}
