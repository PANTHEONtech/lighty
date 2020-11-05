/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.api;

import org.opendaylight.mdsal.binding.runtime.spi.ModuleInfoSnapshotBuilder;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

/**
 * Yang module registration service for global schema context.
 *
 * @author juraj.veverka
 */
public interface LightyModuleRegistryService {
    /**
     * Register instances of Yang modules into global schema context.
     * @param yangModuleInfos modules to register
     * @return list of registrations
     */
    ModuleInfoSnapshotBuilder registerModuleInfos(Iterable<? extends YangModuleInfo> yangModuleInfos);
}
