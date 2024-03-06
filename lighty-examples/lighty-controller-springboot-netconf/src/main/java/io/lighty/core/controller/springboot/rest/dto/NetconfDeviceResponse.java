/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.rest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev240120.ConnectionOper.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev231121.NetconfNode;
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
        final NetconfNode netconfNode = node.augmentation(NetconfNode.class);
        final ConnectionStatus connectionStatus = netconfNode != null ? netconfNode.getConnectionStatus() : null;
        return new NetconfDeviceResponse(node.getNodeId().getValue(), connectionStatus, darknessFactor);
    }

    public static NetconfDeviceResponse from(final Node node) {
        final NetconfNode netconfNode = node.augmentation(NetconfNode.class);
        final ConnectionStatus connectionStatus = netconfNode != null ? netconfNode.getConnectionStatus() : null;
        return new NetconfDeviceResponse(node.getNodeId().getValue(), connectionStatus, new Long(0));
    }

}
