/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.config;

import java.util.List;

public class CheckUpdateFromConfiguration {

    private final int rfcVersion;
    private final List<String> checkUpdateFromPath;

    CheckUpdateFromConfiguration(final int rfcVersion, final List<String> checkUpdateFromPath) {
        this.checkUpdateFromPath = checkUpdateFromPath;
        this.rfcVersion = rfcVersion;
    }

    public int getRfcVersion() {
        return rfcVersion;
    }

    public List<String> getCheckUpdateFromPath() {
        return checkUpdateFromPath;
    }
}
