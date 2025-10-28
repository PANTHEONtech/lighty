/*
 * Copyright (c) 2019-2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.spring;

public class LightyLaunchException extends Exception {
    private static final long serialVersionUID = 1L;

    public LightyLaunchException() {
        super();
    }

    public LightyLaunchException(final String message) {
        super(message);
    }

    public LightyLaunchException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public LightyLaunchException(final Throwable cause) {
        super(cause);
    }
}
