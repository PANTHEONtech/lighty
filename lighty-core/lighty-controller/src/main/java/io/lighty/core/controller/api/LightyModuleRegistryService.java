/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.api;

import java.util.List;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.concepts.Registration;

/**
 * Yang module registration service for global schema context.
 *
 * @author juraj.veverka
 */
public interface LightyModuleRegistryService {
    /**
     * Register instances of Yang modules into global schema context.
     *
     * @param yangModuleInfos modules to register
     * @return list of registrations
     */
    List<Registration> registerModuleInfos(Iterable<? extends YangModuleInfo> yangModuleInfos);
}
