/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
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
