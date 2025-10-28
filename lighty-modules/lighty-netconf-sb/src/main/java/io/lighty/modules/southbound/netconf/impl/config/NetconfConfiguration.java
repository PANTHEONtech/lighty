/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;

public class NetconfConfiguration {

    private String topologyId = "topology-netconf";
    private int writeTxTimeout = 0;
    private boolean clusterEnabled = false;

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

}
