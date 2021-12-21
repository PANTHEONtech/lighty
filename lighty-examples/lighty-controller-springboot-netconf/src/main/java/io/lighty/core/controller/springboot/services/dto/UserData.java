/*
 * Copyright (c) 2018-2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.services.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserData {

    private final String userName;
    private final String password;

    @JsonCreator
    public UserData(@JsonProperty("userName") String userName,
                    @JsonProperty("password") String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public boolean verifyPassword(String password) {
        return this.password.equals(password);
    }

}
