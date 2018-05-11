/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.api;

/**
 * This is main Lighty module providing all important community services for
 * dependent modules in controller application.
 *
 * @author juraj.veverka
 */
public interface LightyController extends LightyModule {

    /**
     * Get important controller services.
     * @return running controller services.
     */
    LightyServices getServices();

}
