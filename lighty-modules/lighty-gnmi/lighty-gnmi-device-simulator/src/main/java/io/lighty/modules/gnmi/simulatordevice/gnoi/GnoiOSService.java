/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.gnoi;

import gnoi.os.OSGrpc;
import gnoi.os.Os;
import io.grpc.stub.StreamObserver;

public class GnoiOSService extends OSGrpc.OSImplBase {

    @Override
    public StreamObserver<Os.InstallRequest> install(final StreamObserver<Os.InstallResponse> responseObserver) {
        return new StreamObserver<>() {
            Os.InstallRequest transferRequest = null;
            Os.InstallRequest content = null;
            Os.InstallRequest transferEnd = null;

            @Override
            public void onNext(final Os.InstallRequest value) {
                if (transferRequest == null) {
                    if (!value.hasTransferRequest()) {
                        responseObserver.onError(new IllegalArgumentException("TransferRequest needs to be the first"
                                + "request to initialize install rpc"));
                        return;
                    }
                    transferRequest = value;
                    return;
                }

                if (content == null) {
                    if (value.getTransferContent() == null) {
                        responseObserver.onError(new IllegalArgumentException("TransferRequest expected content."));
                        return;
                    }
                    content = value;
                    return;
                }

                if (transferEnd == null) {
                    if (!value.hasTransferEnd()) {
                        responseObserver
                                .onError(new IllegalArgumentException("TransferRequest expected transfer end."));
                        return;
                    }
                    transferEnd = value;

                    responseObserver.onNext(Os.InstallResponse.newBuilder()
                            .setValidated(Os.Validated.getDefaultInstance()).build());
                }
            }

            @Override
            public void onError(final Throwable throwable) {
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

}
