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

public class NetconfDeviceRequest {

    private final String username;
    private final String password;
    private final String address;
    private final Integer port;

    @JsonCreator
    public NetconfDeviceRequest(@JsonProperty("username") String username,
                                @JsonProperty("password") String password,
                                @JsonProperty("address") String address,
                                @JsonProperty("port") Integer port) {
        this.username = username;
        this.password = password;
        this.address = address;
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAddress() {
        return address;
    }

    public Integer getPort() {
        return port;
    }

}
