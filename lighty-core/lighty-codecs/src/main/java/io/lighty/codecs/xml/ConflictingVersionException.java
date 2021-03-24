/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.xml;

/**
 * Can be thrown during commitConfig to indicate
 * that the transaction cannot be committed due to the fact that another
 * transaction was committed after creating this transaction. Clients can create
 * new transaction and merge the changes.
 *
 * @deprecated No longer needed. Used in deprecated {@link DocumentedException}.
 */
@Deprecated(forRemoval = true)
public class ConflictingVersionException extends Exception {
    private static final long serialVersionUID = 1L;

    public ConflictingVersionException() {
    }

    public ConflictingVersionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ConflictingVersionException(final String message) {
        super(message);
    }

    public ConflictingVersionException(final Throwable cause) {
        super(cause);
    }

}
