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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnoiFileService extends FileGrpc.FileImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(GnoiFileService.class);

    public static final String PATH_TO_DATA = "fileservice/dummy-file";
    // When changing, don't forget to change expected number of chunks in it tests
    private static final int CHUNK_SIZE = 48000;

    public static final long LAST_MODIFIED = 1598429919;
    public static final int PERMISSIONS = 0444;
    public static final int UMASK = 0755;
    public static final long SIZE = 192000;


    @Override
    public void get(final FileOuterClass.GetRequest request,
                    final StreamObserver<FileOuterClass.GetResponse> responseObserver) {
        LOG.info("Received get rpc: {}", request);
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            readFileAndCompleteObserver(md, responseObserver);
        } catch (final NoSuchAlgorithmException e) {
            responseObserver.onError(e);
        }
    }

    private void readFileAndCompleteObserver(final MessageDigest md,
                                             final StreamObserver<FileOuterClass.GetResponse> responseObserver) {
        try (InputStream is = new BufferedInputStream(Objects.requireNonNull(getClass().getClassLoader()
            .getResourceAsStream(PATH_TO_DATA)));
            DigestInputStream dis = new DigestInputStream(is, md)) {
            while (true) {
                final byte[] bytes = dis.readNBytes(CHUNK_SIZE);

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

        } catch (final IOException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public StreamObserver<FileOuterClass.PutRequest> put(
            final StreamObserver<FileOuterClass.PutResponse> responseObserver) {
        final List<FileOuterClass.PutRequest> requests = new ArrayList<>();
        return new StreamObserver<>() {
            @Override
            public void onNext(final FileOuterClass.PutRequest value) {
                LOG.info("Received file chunk");
                requests.add(value);
            }

            @Override
            public void onError(final Throwable throwable) {
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
    public void stat(final FileOuterClass.StatRequest request,
                     final StreamObserver<FileOuterClass.StatResponse> responseObserver) {
        LOG.info("Received stat rpc: {}", request);

        final FileOuterClass.StatResponse response = FileOuterClass.StatResponse.newBuilder().addStats(
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
    public void remove(final FileOuterClass.RemoveRequest request,
                       final StreamObserver<FileOuterClass.RemoveResponse> responseObserver) {
        LOG.info("Received remove rpc: {}", request);
        responseObserver.onNext(FileOuterClass.RemoveResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

}
