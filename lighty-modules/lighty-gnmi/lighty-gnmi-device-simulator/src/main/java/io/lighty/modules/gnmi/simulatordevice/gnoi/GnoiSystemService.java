/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.gnoi;

import gnoi.system.SystemGrpc;
import gnoi.system.SystemOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnoiSystemService extends SystemGrpc.SystemImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(GnoiSystemService.class);

    @Override
    public void time(SystemOuterClass.TimeRequest request,
            StreamObserver<SystemOuterClass.TimeResponse> responseObserver) {
        LOG.info("Received time rpc: {}", request);

        long millis = System.currentTimeMillis();
        var response = SystemOuterClass.TimeResponse.newBuilder()
                .setTime(millis).build();
        LocalDate date =
                Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();

        LOG.info("Current time in millis: {}", date.format(DateTimeFormatter.BASIC_ISO_DATE));
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void reboot(SystemOuterClass.RebootRequest request,
            StreamObserver<SystemOuterClass.RebootResponse> responseObserver) {
        LOG.info("Received reboot rpc: {}", request);

        SystemOuterClass.RebootResponse response = SystemOuterClass.RebootResponse.getDefaultInstance();
        LOG.info("Reboot response: {}", response);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
