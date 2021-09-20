/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.exception;

public class RncLightyAppStartException extends Exception {

    public RncLightyAppStartException(String message) {
        super(message);
    }

    public RncLightyAppStartException(String message, Throwable cause) {
        super(message, cause);
    }
}
