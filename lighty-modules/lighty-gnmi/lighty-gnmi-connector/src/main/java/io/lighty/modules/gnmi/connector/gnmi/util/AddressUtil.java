/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnmi.util;

public final class AddressUtil {

    private AddressUtil() {
        throw new UnsupportedOperationException("Utility classes should not be instantiated!");
    }

    public static final String ANY_IPV4 = "0.0.0.0";
    public static final String LOCALHOST = "127.0.0.1";
}
