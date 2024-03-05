/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema.yangstore.rpc;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import io.lighty.gnmi.southbound.schema.yangstore.service.YangDataStoreService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.UploadYangModel;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.UploadYangModelInput;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.UploadYangModelOutput;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.UploadYangModelOutputBuilder;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangStorageServiceRpcImpl {
    private static final Logger LOG = LoggerFactory.getLogger(YangStorageServiceRpcImpl.class);

    private final YangDataStoreService yangDataStoreService;

    public YangStorageServiceRpcImpl(final YangDataStoreService yangDataStoreService) {
        this.yangDataStoreService = yangDataStoreService;
    }

    private ListenableFuture<RpcResult<UploadYangModelOutput>> uploadYangModel(final UploadYangModelInput input) {
        final ListenableFuture<? extends CommitInfo> uploadResultFuture =
                yangDataStoreService
                        .addYangModel(input.getName(), input.getVersion().getValue(), input.getBody());

        final SettableFuture<RpcResult<UploadYangModelOutput>> rpcResultFuture = SettableFuture.create();
        Futures.addCallback(uploadResultFuture, new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(@Nullable CommitInfo commitInfo) {

                LOG.info("Yang model {} with version {} added to operational datastore",
                        input.getName(), input.getVersion().getValue());
                final RpcResult<UploadYangModelOutput> result =
                        RpcResultBuilder.success(new UploadYangModelOutputBuilder().build()).build();
                rpcResultFuture.set(result);
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.warn("Failed writing yang model {} with version {} to operational datastore",
                        input.getName(), input.getVersion().getValue());
                final RpcResult<UploadYangModelOutput> result = RpcResultBuilder.<UploadYangModelOutput>failed()
                        .withError(ErrorType.APPLICATION, "Failed to write model", throwable)
                        .build();
                rpcResultFuture.set(result);

            }
        }, MoreExecutors.directExecutor());
        return rpcResultFuture;
    }

    public ClassToInstanceMap<Rpc<?,?>> getRpcClassToInstanceMap() {
        return ImmutableClassToInstanceMap.<Rpc<?, ?>>builder()
                .put(UploadYangModel.class, this::uploadYangModel)
                .build();
    }
}
