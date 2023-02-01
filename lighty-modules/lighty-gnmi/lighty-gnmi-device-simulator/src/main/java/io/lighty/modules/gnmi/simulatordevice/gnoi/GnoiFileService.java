/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.gnoi;

import com.google.protobuf.ByteString;
import gnoi.file.FileGrpc;
import gnoi.file.FileOuterClass;
import gnoi.types.Types;
import io.grpc.stub.StreamObserver;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnoiFileService extends FileGrpc.FileImplBase {

    public static final String PATH_TO_DATA = "fileservice/dummy-file";
    public static final long LAST_MODIFIED = 1598429919;
    public static final int PERMISSIONS = 0444;
    public static final int UMASK = 0755;
    public static final long SIZE = 192000;
    private static final Logger LOG = LoggerFactory.getLogger(GnoiFileService.class);
    // When changing, don't forget to change expected number of chunks in it tests
    private static final int CHUNK_SIZE = 48000;

    @Override
    public void get(FileOuterClass.GetRequest request,
            StreamObserver<FileOuterClass.GetResponse> responseObserver) {
        LOG.info("Received get rpc: {}", request);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            readFileAndCompleteObserver(md, responseObserver);
        } catch (NoSuchAlgorithmException e) {
            responseObserver.onError(e);
        }
    }

    private void readFileAndCompleteObserver(MessageDigest md,
            StreamObserver<FileOuterClass.GetResponse> responseObserver) {
        try (InputStream is = new BufferedInputStream(Objects.requireNonNull(getClass().getClassLoader()
                .getResourceAsStream(PATH_TO_DATA)));
                DigestInputStream dis = new DigestInputStream(is, md)) {
            while (true) {
                byte[] bytes = dis.readNBytes(CHUNK_SIZE);

                if (bytes.length == 0) {
                    break;
                }

                responseObserver.onNext(FileOuterClass.GetResponse.newBuilder()
                        .setContents(ByteString.copyFrom(bytes)).build());
            }

            responseObserver.onNext(FileOuterClass.GetResponse.newBuilder()
                    .setHash(Types.HashType.newBuilder()
                            .setMethod(Types.HashType.HashMethod.MD5)
                            .setHash(ByteString.copyFrom(md.digest())).build())
                    .build());
            responseObserver.onCompleted();

        } catch (IOException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public StreamObserver<FileOuterClass.PutRequest> put(
            StreamObserver<FileOuterClass.PutResponse> responseObserver) {

        return new StreamObserver<>() {
            @Override
            public void onNext(FileOuterClass.PutRequest value) {
                LOG.info("Received file chunk");

            }

            @Override
            public void onError(Throwable throwable) {
                LOG.error("Put rpc failed.", throwable);
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(FileOuterClass.PutResponse.getDefaultInstance());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void stat(FileOuterClass.StatRequest request,
            StreamObserver<FileOuterClass.StatResponse> responseObserver) {
        LOG.info("Received stat rpc: {}", request);

        var response = FileOuterClass.StatResponse.newBuilder().addStats(
                        FileOuterClass.StatInfo.newBuilder().setPath(request.getPath() + "/" + PATH_TO_DATA)
                                .setLastModified(LAST_MODIFIED)
                                .setPermissions(PERMISSIONS)
                                .setSize(SIZE)
                                .setUmask(UMASK)
                                .build())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void remove(FileOuterClass.RemoveRequest request,
            StreamObserver<FileOuterClass.RemoveResponse> responseObserver) {
        LOG.info("Received remove rpc: {}", request);
        responseObserver.onNext(FileOuterClass.RemoveResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

}
