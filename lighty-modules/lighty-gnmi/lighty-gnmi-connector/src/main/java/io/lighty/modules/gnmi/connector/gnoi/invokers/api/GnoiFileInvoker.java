/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnoi.invokers.api;

import gnoi.file.FileOuterClass;
import io.grpc.stub.StreamObserver;

/**
 * Interface exposing gnoi-file methods.
 */
public interface GnoiFileInvoker {
    void get(FileOuterClass.GetRequest getRequest,
             StreamObserver<FileOuterClass.GetResponse> responseObserver);

    StreamObserver<FileOuterClass.PutRequest> put(StreamObserver<FileOuterClass.PutResponse> responseObserver);

    void stat(FileOuterClass.StatRequest statRequest,
              StreamObserver<FileOuterClass.StatResponse> responseObserver);

    void remove(FileOuterClass.RemoveRequest request,
                StreamObserver<FileOuterClass.RemoveResponse> responseObserver);
}
