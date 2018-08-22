/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.api;

/**
 * This exception should be thrown when serialization problem occurs
 */
public class SerializationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 2053802415449540367L;

    public SerializationException(Throwable t) {
        super(t);
    }

    public SerializationException(String message, Throwable t) {
        super(message, t);
    }
}
