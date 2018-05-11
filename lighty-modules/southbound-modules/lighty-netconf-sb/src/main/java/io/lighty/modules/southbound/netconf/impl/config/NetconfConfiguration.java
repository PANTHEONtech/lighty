/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
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
