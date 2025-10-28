/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.codecs.util.exception;

/**
 * This exception should be thrown when deserialization problem occurs.
 */
public class DeserializationException extends Exception {

    public DeserializationException(final Throwable cause) {
        super(cause);
    }
}
