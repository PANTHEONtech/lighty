/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.common.models;

@SuppressWarnings("serial")
public class YangClasspathResolutionException extends Exception {

    public YangClasspathResolutionException() {
    }

    public YangClasspathResolutionException(final Exception e) {
        super(e);
    }

}
