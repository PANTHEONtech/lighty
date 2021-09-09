/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.netconf.client.NetconfClientDispatcher;

public class NetconfConfiguration {

    private String topologyId = "topology-netconf";
    private int writeTxTimeout = 0;
    private boolean clusterEnabled = false;

    @JsonIgnore
    private NetconfClientDispatcher clientDispatcher;

    @JsonIgnore
    private AAAEncryptionService aaaService;

    public String getTopologyId() {
        return topologyId;
    }

    public void setTopologyId(final String topologyId) {
        this.topologyId = topologyId;
    }

    public AAAEncryptionService getAaaService() {
        return aaaService;
    }

    public void setAaaService(final AAAEncryptionService aaaService) {
        this.aaaService = aaaService;
    }

    public int getWriteTxTimeout() {
        return writeTxTimeout;
    }

    public void setWriteTxTimeout(int writeTxTimeout) {
        this.writeTxTimeout = writeTxTimeout;
    }

    public boolean isClusterEnabled() {
        return clusterEnabled;
    }

    public void setClusterEnabled(boolean clusterEnabled) {
        this.clusterEnabled = clusterEnabled;
    }

    public NetconfClientDispatcher getClientDispatcher() {
        return clientDispatcher;
    }

    public void setClientDispatcher(final NetconfClientDispatcher clientDispatcher) {
        this.clientDispatcher = clientDispatcher;
    }

}
