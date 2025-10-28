/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.rest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev241009.ConnectionOper.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev240911.NetconfNodeAugment;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public class NetconfDeviceResponse {

    private final String nodeId;
    private final ConnectionStatus connectionStatus;
    private final Long darknessFactor;

    @JsonCreator
    private NetconfDeviceResponse(@JsonProperty("nodeId") String nodeId,
                                  @JsonProperty("connectionStatus") ConnectionStatus connectionStatus,
                                  @JsonProperty("darknessFactor") Long darknessFactor) {
        this.nodeId = nodeId;
        this.connectionStatus = connectionStatus;
        this.darknessFactor = darknessFactor;
    }

    public String getNodeId() {
        return nodeId;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public Long getDarknessFactor() {
        return darknessFactor;
    }

    public static NetconfDeviceResponse from(final Node node, Long darknessFactor) {
        final NetconfNodeAugment netconfNode = node.augmentation(NetconfNodeAugment.class);
        final ConnectionStatus connectionStatus = netconfNode != null ? netconfNode.getNetconfNode().getConnectionStatus() : null;
        return new NetconfDeviceResponse(node.getNodeId().getValue(), connectionStatus, darknessFactor);
    }

    public static NetconfDeviceResponse from(final Node node) {
        final NetconfNodeAugment netconfNode = node.augmentation(NetconfNodeAugment.class);
        final ConnectionStatus connectionStatus = netconfNode != null ? netconfNode.getNetconfNode().getConnectionStatus() : null;
        return new NetconfDeviceResponse(node.getNodeId().getValue(), connectionStatus, new Long(0));
    }

}
