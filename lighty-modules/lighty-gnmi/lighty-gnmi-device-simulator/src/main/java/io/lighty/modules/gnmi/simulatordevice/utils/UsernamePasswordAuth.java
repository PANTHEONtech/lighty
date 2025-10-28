/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.utils;

import com.google.common.base.Strings;
import org.eclipse.jdt.annotation.NonNull;

public class UsernamePasswordAuth {
    private final String username;
    private final String password;

    public UsernamePasswordAuth(@NonNull final String username, @NonNull final String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    public boolean isNotEmpty() {
        return !Strings.isNullOrEmpty(this.username) && !Strings.isNullOrEmpty(this.password);
    }

    public boolean authenticate(final String receivedUsername, final String receivedPassword) {
        return this.username.equals(receivedUsername) && this.password.equals(receivedPassword);
    }
}
