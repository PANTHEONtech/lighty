/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.applications.rcgnmi.module;

public class RcGnmiAppException extends Exception {

    public RcGnmiAppException(String message) {
        super(message);
    }

    public RcGnmiAppException(String message, Throwable cause) {
        super(message, cause);
    }
}
