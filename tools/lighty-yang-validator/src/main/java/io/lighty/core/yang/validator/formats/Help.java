/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats;

class Help {

    private final String description;
    private final String name;

    Help(final String name, final String description) {
        this.name = name;
        this.description = description;
    }

    String getName() {
        return this.name;
    }

    public String generateFromatHelp() {
        return "\n" + name + " - " + description;
    }
}
