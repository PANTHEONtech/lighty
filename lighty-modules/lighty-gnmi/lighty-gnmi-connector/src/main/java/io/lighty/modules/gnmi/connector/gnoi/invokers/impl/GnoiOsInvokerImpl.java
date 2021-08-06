/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnoi.invokers.impl;

import gnoi.os.OSGrpc;
import gnoi.os.Os;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import io.lighty.modules.gnmi.connector.gnoi.invokers.api.GnoiOsInvoker;

public final class GnoiOsInvokerImpl implements GnoiOsInvoker {

    private final OSGrpc.OSStub stub;

    private GnoiOsInvokerImpl(final OSGrpc.OSStub stub) {
        this.stub = stub;
    }

    public static GnoiOsInvoker fromChannel(final Channel channel) {
        return new GnoiOsInvokerImpl(OSGrpc.newStub(channel));
    }

    @Override
    public StreamObserver<Os.InstallRequest> install(final StreamObserver<Os.InstallResponse> responseObserver) {
        return stub.install(responseObserver);
    }

    @Override
    public void activate(final Os.ActivateRequest request, final StreamObserver<Os.ActivateResponse> responseObserver) {
        stub.activate(request, responseObserver);
    }

    @Override
    public void verify(final Os.VerifyRequest request, final StreamObserver<Os.VerifyResponse> responseObserver) {
        stub.verify(request, responseObserver);
    }
}
