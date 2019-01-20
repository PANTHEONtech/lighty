/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.config;

/**
 * author: vincent on 7.9.2017.
 */
public class ConfigurationException extends Exception {
    private static final long serialVersionUID = 1L;

    public ConfigurationException() {
        super();
    }

    public ConfigurationException(final String s) {
        super(s);
    }

    public ConfigurationException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public ConfigurationException(final Throwable throwable) {
        super(throwable);
    }
}
