/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats;

import io.lighty.core.yang.validator.LyvParameters;

public interface CommandLineOptions {
    /**
     * Add option to "options" instance.
     * @param options options that exists for this tool.
     */
    void createOptions(LyvParameters options);
}
