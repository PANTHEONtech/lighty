/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema.yangstore.service;


import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.gnmi.yang.models.GnmiYangModel;

/**
 * Manages yangs, writes/reads them to/from operational datastore.
 */
public interface YangDataStoreService {

    /**
     * Adds a yang model so later it can be read and used for building schema context for device
     *  based on gNMI Capabilities.
     * @param modelName name of the module
     * @param modelVersion version of module (revision date format/semantic version/empty string if model is
     *                    not versioned)
     * @param modelBody content of the yang file
     * @return future result
     */
    ListenableFuture<? extends CommitInfo> addYangModel(String modelName, String modelVersion, String modelBody);


    /**
     * Tries to read yang model with specified version from datastore.
     * @param modelName name of the module
     * @param modelVersion version of module (revision date format/semantic format)
     * @return future optional yang model
     */
    ListenableFuture<Optional<GnmiYangModel>> readYangModel(String modelName, String modelVersion);

    /**
     * Tries to read yang model from datastore without independent of it's version.
     * Model is returned only if one version is present in the datastore.
     * @param modelName name of the module
     * @return future optional yang model
     */
    ListenableFuture<Optional<GnmiYangModel>> readYangModel(String modelName);


}
