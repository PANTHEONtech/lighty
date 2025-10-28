/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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
