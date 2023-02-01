/*
 * Copyright (c) 2019-2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.spring;

public class LightyLaunchException extends Exception {
    private static final long serialVersionUID = 1L;

    public LightyLaunchException() {
        super();
    }

    public LightyLaunchException(String message) {
        super(message);
    }

    public LightyLaunchException(String message, Throwable cause) {
        super(message, cause);
    }

    public LightyLaunchException(Throwable cause) {
        super(cause);
    }
}
