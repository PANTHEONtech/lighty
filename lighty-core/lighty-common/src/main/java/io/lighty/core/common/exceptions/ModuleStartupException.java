/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.common.exceptions;

/**
 * Exception to be thrown for module startup failures - when they return false from start() method.
 */
public class ModuleStartupException extends Exception {
    private static final long serialVersionUID = 1L;

    public ModuleStartupException() {
        super();
    }

    public ModuleStartupException(final String message) {
        super(message);
    }

    public ModuleStartupException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ModuleStartupException(final Throwable cause) {
        super(cause);
    }
}
