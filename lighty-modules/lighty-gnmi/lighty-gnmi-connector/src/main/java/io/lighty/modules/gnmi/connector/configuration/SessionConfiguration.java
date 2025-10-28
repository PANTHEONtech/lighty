/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.configuration;

import io.lighty.modules.gnmi.connector.gnmi.util.AddressUtil;
import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * Configuration for gNMI and gNOI session. This configuration can be used to uniquely identify session.
 */
public class SessionConfiguration {

    // If you are adding new parameter, DON'T FORGET to update EQUALS and HASHCODE method

    private InetSocketAddress address;
    private boolean usePlainText;
    private String username;
    private String password;

    /**
     * Constructor with default values.
     */
    public SessionConfiguration() {
        this.address = new InetSocketAddress(AddressUtil.LOCALHOST, 8080);
        this.usePlainText = false;
    }

    public SessionConfiguration(final InetSocketAddress address, final boolean usePlainText) {
        this.address = address;
        this.usePlainText = usePlainText;
    }

    public SessionConfiguration(final InetSocketAddress address, final boolean usePlainText, final String username,
                                final String password) {
        this.address = address;
        this.usePlainText = usePlainText;
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        SessionConfiguration that = (SessionConfiguration) obj;

        if (usePlainText != that.usePlainText) {
            return false;
        }
        return Objects.equals(address, that.address)
                && Objects.equals(username, that.username)
                && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, usePlainText, username, password);
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    public boolean isUsePlainText() {
        return usePlainText;
    }

    public void setUsePlainText(boolean usePlainText) {
        this.usePlainText = usePlainText;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
