/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.rest.dto;

import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public class NetconfDeviceResponse {

    private String nodeId;
    private ConnectionStatus connectionStatus;
    private Long darknessFactor;

    public NetconfDeviceResponse() {
    }

    private NetconfDeviceResponse(final String nodeId, final ConnectionStatus connectionStatus) {
        this.nodeId = nodeId;
        this.connectionStatus = connectionStatus;
    }

    public static NetconfDeviceResponse from(final Node node) {
        final NetconfNode netconfNode = node.augmentation(NetconfNode.class);

        final ConnectionStatus connectionStatus = netconfNode != null ? netconfNode.getConnectionStatus() : null;

        return new NetconfDeviceResponse(node.getNodeId().getValue(), connectionStatus);
    }

    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setConnectionStatus(
        final ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setDarknessFactor(final Long darknessFactor) {
        this.darknessFactor = darknessFactor;
    }

    public Long getDarknessFactor() {
        return darknessFactor;
    }
}
