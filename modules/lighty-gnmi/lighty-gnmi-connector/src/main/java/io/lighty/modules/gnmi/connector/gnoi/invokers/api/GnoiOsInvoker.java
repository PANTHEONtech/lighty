/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnoi.invokers.api;

import gnoi.os.Os;
import io.grpc.stub.StreamObserver;

/**
 * Interface exposing gnoi-os methods.
 */
public interface GnoiOsInvoker {
    StreamObserver<Os.InstallRequest> install(StreamObserver<Os.InstallResponse> responseObserver);

    void activate(Os.ActivateRequest request, StreamObserver<Os.ActivateResponse> responseObserver);

    void verify(Os.VerifyRequest request, StreamObserver<Os.VerifyResponse> responseObserver);


}
