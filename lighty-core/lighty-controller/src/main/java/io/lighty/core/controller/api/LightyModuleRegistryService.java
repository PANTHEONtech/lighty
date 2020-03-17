/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.api;

import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

/**
 * Yang module registration service for global schema context.
 *
 * @author juraj.veverka
 */
public interface LightyModuleRegistryService {
    /**
     * Register an instance of Yang module into global schema context.
     */
    ObjectRegistration<YangModuleInfo> registerModuleInfo(YangModuleInfo yangModuleInfo);
}
