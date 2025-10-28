/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnmi.session.impl;

import com.google.common.util.concurrent.ListenableFuture;
import gnmi.Gnmi.CapabilityRequest;
import gnmi.Gnmi.CapabilityResponse;
import gnmi.Gnmi.GetRequest;
import gnmi.Gnmi.GetResponse;
import gnmi.Gnmi.SetRequest;
import gnmi.Gnmi.SetResponse;
import gnmi.Gnmi.SubscribeRequest;
import gnmi.Gnmi.SubscribeResponse;
import gnmi.gNMIGrpc;
import gnmi.gNMIGrpc.gNMIFutureStub;
import gnmi.gNMIGrpc.gNMIStub;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import io.lighty.modules.gnmi.connector.gnmi.session.api.GnmiSession;

public class GnmiSessionImpl implements GnmiSession {

    private final gNMIFutureStub futureStub;
    private final gNMIStub stub;

    GnmiSessionImpl(final gNMIFutureStub futureStub, final gNMIStub stub) {
        this.futureStub = futureStub;
        this.stub = stub;
    }

    public GnmiSessionImpl(final Channel channel) {
        this(gNMIGrpc.newFutureStub(channel), gNMIGrpc.newStub(channel));
    }

    public GnmiSessionImpl(final Channel channel, final CallCredentials credentials) {
        this(gNMIGrpc.newFutureStub(channel).withCallCredentials(credentials),
                gNMIGrpc.newStub(channel).withCallCredentials(credentials));
    }

    @Override
    public synchronized ListenableFuture<GetResponse> get(final GetRequest getRequest) {
        return futureStub.get(getRequest);
    }

    @Override
    public synchronized ListenableFuture<SetResponse> set(final SetRequest setRequest) {
        return futureStub.set(setRequest);
    }

    @Override
    public synchronized ListenableFuture<CapabilityResponse> capabilities(final CapabilityRequest capabilityRequest) {
        return futureStub.capabilities(capabilityRequest);
    }

    @Override
    public synchronized StreamObserver<SubscribeRequest> subscribe(
            final StreamObserver<SubscribeResponse> responseObserver) {
        return stub.subscribe(responseObserver);
    }
}
