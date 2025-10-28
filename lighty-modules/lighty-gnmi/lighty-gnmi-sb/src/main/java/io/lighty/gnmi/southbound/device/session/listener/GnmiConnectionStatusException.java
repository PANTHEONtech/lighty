/*
 * Copyright © 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.device.session.listener;

import io.grpc.ConnectivityState;

public class GnmiConnectionStatusException extends Exception {

    private final ConnectivityState currentState;

    public GnmiConnectionStatusException(final String message, final ConnectivityState currentState) {
        super(message);
        this.currentState = currentState;
    }

    public ConnectivityState getCurrentState() {
        return currentState;
    }
}
