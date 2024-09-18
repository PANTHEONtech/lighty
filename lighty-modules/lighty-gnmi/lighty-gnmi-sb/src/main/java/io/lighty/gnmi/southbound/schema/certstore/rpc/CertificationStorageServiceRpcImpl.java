/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema.certstore.rpc;


import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import io.lighty.gnmi.southbound.schema.certstore.service.CertificationStorageService;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.List;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.AddKeystoreCertificate;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.AddKeystoreCertificateInput;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.AddKeystoreCertificateOutput;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.AddKeystoreCertificateOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.RemoveKeystoreCertificate;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.RemoveKeystoreCertificateInput;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.RemoveKeystoreCertificateOutput;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.RemoveKeystoreCertificateOutputBuilder;
import org.opendaylight.yangtools.binding.Rpc;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertificationStorageServiceRpcImpl {

    private static final Logger LOG = LoggerFactory.getLogger(CertificationStorageServiceRpcImpl.class);
    private final CertificationStorageService certStorage;

    public CertificationStorageServiceRpcImpl(final CertificationStorageService certStorage) {
        this.certStorage = certStorage;
    }

    private ListenableFuture<RpcResult<AddKeystoreCertificateOutput>> addKeystoreCertificate(
            final AddKeystoreCertificateInput input) {
        final ListenableFuture<? extends CommitInfo> writeResult;
        try {
            writeResult = this.certStorage.writeCertificates(input);
        } catch (GeneralSecurityException e) {
            LOG.error("Failed do encrypt input {}", input);
            throw new RuntimeException(e);
        }
        final SettableFuture<RpcResult<AddKeystoreCertificateOutput>> rpcResult = SettableFuture.create();

        Futures.addCallback(writeResult, new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("add-keystore-certificate success. Keystore-id: {}", input.getKeystoreId());
                rpcResult.set(RpcResultBuilder.success(new AddKeystoreCertificateOutputBuilder().build()).build());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("add-keystore-certificate failed. Input: {}", input, throwable);
                rpcResult.setException(throwable);
            }
        }, MoreExecutors.directExecutor());
        return rpcResult;
    }

    private ListenableFuture<RpcResult<RemoveKeystoreCertificateOutput>> removeKeystoreCertificate(
            final RemoveKeystoreCertificateInput input) {
        final ListenableFuture<? extends CommitInfo> removeResult = this.certStorage.removeCertificates(input);
        final SettableFuture<RpcResult<RemoveKeystoreCertificateOutput>> rpcResult = SettableFuture.create();

        Futures.addCallback(removeResult, new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("remove-keystore-certificate success. keystore-id: {}", input.getKeystoreId());
                rpcResult.set(RpcResultBuilder.success(new RemoveKeystoreCertificateOutputBuilder().build()).build());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("remove-keystore-certificate failed. Input: {}", input, throwable);
                rpcResult.setException(throwable);
            }
        }, MoreExecutors.directExecutor());
        return rpcResult;
    }

    public Collection<Rpc<?,?>> getRpcClassToInstanceMap() {
        return List.of(
            (AddKeystoreCertificate) this::addKeystoreCertificate,
            (RemoveKeystoreCertificate) this::removeKeystoreCertificate);
    }
}
