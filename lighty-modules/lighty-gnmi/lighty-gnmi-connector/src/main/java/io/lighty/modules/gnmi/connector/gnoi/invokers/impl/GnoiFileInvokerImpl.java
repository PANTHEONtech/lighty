/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnoi.invokers.impl;

import gnoi.file.FileGrpc;
import gnoi.file.FileOuterClass;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import io.lighty.modules.gnmi.connector.gnoi.invokers.api.GnoiFileInvoker;

public final class GnoiFileInvokerImpl implements GnoiFileInvoker {

    private final FileGrpc.FileStub fileStub;

    private GnoiFileInvokerImpl(final FileGrpc.FileStub fileStub) {
        this.fileStub = fileStub;
    }

    public static GnoiFileInvoker fromChannel(final Channel channel) {
        return new GnoiFileInvokerImpl(FileGrpc.newStub(channel));
    }

    @Override
    public void get(final FileOuterClass.GetRequest getRequest,
                    final StreamObserver<FileOuterClass.GetResponse> responseObserver) {
        fileStub.get(getRequest, responseObserver);
    }

    @Override
    public StreamObserver<FileOuterClass.PutRequest> put(
            final StreamObserver<FileOuterClass.PutResponse> responseObserver) {
        return fileStub.put(responseObserver);
    }

    @Override
    public void stat(final FileOuterClass.StatRequest statRequest,
                     final StreamObserver<FileOuterClass.StatResponse> responseObserver) {
        fileStub.stat(statRequest, responseObserver);
    }

    @Override
    public void remove(final FileOuterClass.RemoveRequest request,
                       final StreamObserver<FileOuterClass.RemoveResponse> responseObserver) {
        fileStub.remove(request, responseObserver);
    }
}
