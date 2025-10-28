/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema.yangstore.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.gnmi.southbound.schema.yangstore.service.YangDataStoreService;
import io.lighty.modules.gnmi.commons.util.YangModelSanitizer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.GnmiYangModels;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.ModuleVersionType;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.gnmi.yang.models.GnmiYangModel;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.gnmi.yang.models.GnmiYangModelBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.gnmi.yang.models.GnmiYangModelKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangDataStoreServiceImpl implements YangDataStoreService {
    private static final Logger LOG = LoggerFactory.getLogger(YangDataStoreServiceImpl.class);

    private final DataBroker dataBroker;
    private final ExecutorService executorService;

    public YangDataStoreServiceImpl(final DataBroker dataBroker, final ExecutorService executorService) {
        this.dataBroker = dataBroker;
        this.executorService = executorService;
    }

    @Override
    public ListenableFuture<? extends CommitInfo> addYangModel(final String modelName, final String modelVersion,
                                                               final String modelBody) {
        LOG.debug("Adding yang model {} with version {} to operational datastore", modelName, modelVersion);
        final GnmiYangModelKey gnmiYangModelKey = new GnmiYangModelKey(modelName, new ModuleVersionType(modelVersion));
        final InstanceIdentifier<GnmiYangModel> instanceIdentifier = InstanceIdentifier.builder(GnmiYangModels.class)
                .child(GnmiYangModel.class, gnmiYangModelKey)
                .build();
        final String sanitizedModelBody = YangModelSanitizer.removeRegexpPosix(modelBody);
        final GnmiYangModelBuilder gnmiYangModelBuilder = new GnmiYangModelBuilder()
                .setName(modelName)
                .setBody(sanitizedModelBody)
                .withKey(gnmiYangModelKey);
        final WriteTransaction writeTX = dataBroker.newWriteOnlyTransaction();
        writeTX.merge(LogicalDatastoreType.OPERATIONAL, instanceIdentifier, gnmiYangModelBuilder.build());
        return writeTX.commit();
    }

    @Override
    public ListenableFuture<Optional<GnmiYangModel>> readYangModel(final String modelName, final String modelVersion) {
        final InstanceIdentifier<GnmiYangModel> instanceIdentifier = InstanceIdentifier.builder(GnmiYangModels.class)
                .child(GnmiYangModel.class, new GnmiYangModelKey(modelName, new ModuleVersionType(modelVersion)))
                .build();
        try (ReadTransaction readOnlyTransaction = this.dataBroker.newReadOnlyTransaction()) {
            return readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);
        }
    }

    @Override
    public ListenableFuture<Optional<GnmiYangModel>> readYangModel(final String modelName) {
        // In case we only know the modelName, return found module if only one is present in datastore
        final InstanceIdentifier<GnmiYangModels> instanceIdentifier = InstanceIdentifier.builder(GnmiYangModels.class)
                .build();

        try (ReadTransaction readOnlyTransaction = this.dataBroker.newReadOnlyTransaction()) {
            final ListenableFuture<Optional<GnmiYangModels>> yangModelOptionalFuture = readOnlyTransaction.read(
                    LogicalDatastoreType.OPERATIONAL, instanceIdentifier);
            return Futures.transform(yangModelOptionalFuture, yangModelOptional -> {
                if (yangModelOptional.isPresent()) {
                    // Keep only models with requested name
                    final List<Map.Entry<GnmiYangModelKey, GnmiYangModel>> modelsWithRequestedName =
                            yangModelOptional.get().nonnullGnmiYangModel().entrySet().stream()
                                    .filter(m -> m.getKey().getName().equals(modelName))
                                    .collect(Collectors.toList());

                    if (modelsWithRequestedName.size() == 1) {
                        return Optional.of(modelsWithRequestedName.stream().findFirst().get().getValue());
                    } else if (modelsWithRequestedName.size() > 1) {
                        LOG.warn("There are multiple version of model {} in datastore, unable to safely determine"
                                + " which one to use, since only the model name is known", modelName);
                    }

                }
                return Optional.empty();

            }, executorService);
        }
    }

}
