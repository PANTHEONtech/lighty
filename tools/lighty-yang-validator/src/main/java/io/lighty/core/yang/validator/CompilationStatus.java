/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.yang.validator;

public enum CompilationStatus {
    PASSED("PASSED"),
    FAILED("FAILED"),
    PASSED_WITH_WARNINGS("PASSED WITH WARNINGS");

    private final String name;

    CompilationStatus(final String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }
}
