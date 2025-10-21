/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema.loader.api;


import io.lighty.gnmi.southbound.capabilities.GnmiDeviceCapability;
import io.lighty.gnmi.southbound.schema.yangstore.service.YangDataStoreService;
import java.util.List;

public interface YangLoaderService {

    /**
     * Loads models into YangDataStoreService.
     * @param storeService YangDataStoreService in which the models are stored, useful for loading default models
     * @return loaded models
     * @throws YangLoadException when loading fails
     */
    List<GnmiDeviceCapability> load(YangDataStoreService storeService) throws YangLoadException;

}
