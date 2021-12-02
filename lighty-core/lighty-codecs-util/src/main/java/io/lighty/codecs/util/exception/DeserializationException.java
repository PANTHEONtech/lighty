/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.codecs.util.exception;

/**
 * This exception should be thrown when deserialization problem occurs.
 */
public class DeserializationException extends Exception {

    public DeserializationException(final Throwable cause) {
        super(cause);
    }

    public DeserializationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
