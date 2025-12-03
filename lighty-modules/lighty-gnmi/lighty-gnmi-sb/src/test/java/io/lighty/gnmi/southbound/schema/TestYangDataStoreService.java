/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.gnmi.southbound.schema.yangstore.service.YangDataStoreService;
import io.lighty.gnmi.southbound.timeout.TimeoutUtils;
import io.lighty.modules.gnmi.commons.util.YangModelSanitizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.ModuleVersionType;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.gnmi.yang.models.GnmiYangModel;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.gnmi.yang.models.GnmiYangModelBuilder;

public class TestYangDataStoreService implements YangDataStoreService {

    private Map<ImmutablePair<String, String>, String> yangs;

    public TestYangDataStoreService() {
        yangs = new HashMap<>();
    }

    @Override
    public ListenableFuture<CommitInfo> addYangModel(final String modelName, final String modelVersion,
                                                               final String modelBody) {
        final String sanitizedModelBody = YangModelSanitizer.removeRegexpPosix(modelBody);
        yangs.put(ImmutablePair.of(modelName, modelVersion), sanitizedModelBody);
        return Futures.immediateFuture(CommitInfo.empty());
    }


    public boolean deleteYangModel(final String modelName, @Nullable String modelVersion)
            throws InterruptedException, ExecutionException, TimeoutException {
        final Optional<GnmiYangModel> model;
        if (modelVersion != null) {
            model = readYangModel(modelName, modelVersion)
                    .get(TimeoutUtils.DATASTORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } else {
            model = readYangModel(modelName)
                    .get(TimeoutUtils.DATASTORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
        return model.filter(gnmiYangModel -> yangs.remove(ImmutablePair.of(gnmiYangModel.getName(),
                gnmiYangModel.getVersion().getValue())) != null).isPresent();
    }

    @Override
    public ListenableFuture<Optional<GnmiYangModel>> readYangModel(final String modelName, final String modelVersion) {
        for (Map.Entry<ImmutablePair<String, String>, String> entry : yangs.entrySet()) {
            if (entry.getKey().left.equals(modelName) && entry.getKey().right.equals(modelVersion)) {
                return Futures.immediateFuture(Optional.of(new GnmiYangModelBuilder()
                        .setVersion(new ModuleVersionType(modelVersion))
                        .setName(modelName)
                        .setBody(entry.getValue()).build()));
            }
        }
        return Futures.immediateFuture(Optional.empty());
    }

    @Override
    public ListenableFuture<Optional<GnmiYangModel>> readYangModel(final String modelName) {
        final List<Map.Entry<ImmutablePair<String, String>, String>> entriesWithRequestedName =
                yangs.entrySet().stream()
                        .filter(e -> e.getKey().left.equals(modelName))
                        .collect(Collectors.toList());

        if (entriesWithRequestedName.size() == 1) {
            final Map.Entry<ImmutablePair<String, String>, String> matchedEntry =
                    entriesWithRequestedName.stream().findFirst().orElseThrow();
            return Futures.immediateFuture(Optional.of(new GnmiYangModelBuilder()
                    .setVersion(new ModuleVersionType(matchedEntry.getKey().right))
                    .setName(modelName)
                    .setBody(matchedEntry.getValue()).build()));
        }

        return Futures.immediateFuture(Optional.empty());
    }

}
