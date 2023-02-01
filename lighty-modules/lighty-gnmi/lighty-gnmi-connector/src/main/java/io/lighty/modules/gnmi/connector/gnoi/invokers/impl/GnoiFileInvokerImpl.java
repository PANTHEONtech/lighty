/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnoi.invokers.impl;

import gnoi.file.FileGrpc;
import gnoi.file.FileOuterClass;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import io.lighty.modules.gnmi.connector.gnoi.invokers.api.GnoiFileInvoker;

public final class GnoiFileInvokerImpl implements GnoiFileInvoker {

    private final FileGrpc.FileStub fileStub;

    private GnoiFileInvokerImpl(FileGrpc.FileStub fileStub) {
        this.fileStub = fileStub;
    }

    public static GnoiFileInvoker fromChannel(Channel channel) {
        return new GnoiFileInvokerImpl(FileGrpc.newStub(channel));
    }

    @Override
    public void get(FileOuterClass.GetRequest getRequest,
            StreamObserver<FileOuterClass.GetResponse> responseObserver) {
        fileStub.get(getRequest, responseObserver);
    }

    @Override
    public StreamObserver<FileOuterClass.PutRequest> put(
            StreamObserver<FileOuterClass.PutResponse> responseObserver) {
        return fileStub.put(responseObserver);
    }

    @Override
    public void stat(FileOuterClass.StatRequest statRequest,
            StreamObserver<FileOuterClass.StatResponse> responseObserver) {
        fileStub.stat(statRequest, responseObserver);
    }

    @Override
    public void remove(FileOuterClass.RemoveRequest request,
            StreamObserver<FileOuterClass.RemoveResponse> responseObserver) {
        fileStub.remove(request, responseObserver);
    }
}
