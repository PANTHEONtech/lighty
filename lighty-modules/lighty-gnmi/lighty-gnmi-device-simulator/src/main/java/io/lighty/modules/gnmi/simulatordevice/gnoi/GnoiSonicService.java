/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.gnoi;

import gnoi.sonic.SonicGnoi;
import gnoi.sonic.SonicServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnoiSonicService extends SonicServiceGrpc.SonicServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(GnoiSonicService.class);
    private static final String SIMULATED_RESPONSE = "This is simulated response of %s sonic_gnoi rpc";

    @Override
    public void showTechsupport(
            gnoi.sonic.SonicGnoi.TechsupportRequest request,
            io.grpc.stub.StreamObserver<gnoi.sonic.SonicGnoi.TechsupportResponse> responseObserver) {
        LOG.info("Received showTechsupport rpc request {}", request);
        final SonicGnoi.TechsupportResponse response = SonicGnoi.TechsupportResponse.newBuilder().setOutput(
                SonicGnoi.TechsupportResponse.Output.newBuilder()
                        .setOutputFilename(String.format(SIMULATED_RESPONSE, "showTechsupport")))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void copyConfig(gnoi.sonic.SonicGnoi.CopyConfigRequest request,
                           io.grpc.stub.StreamObserver<gnoi.sonic.SonicGnoi.CopyConfigResponse> responseObserver) {
        LOG.info("Received copyConfig rpc request {}", request);
        final SonicGnoi.CopyConfigResponse response = SonicGnoi.CopyConfigResponse.newBuilder()
                .setOutput(buildSimulatedSonicOutput("copyConfig"))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void imageInstall(gnoi.sonic.SonicGnoi.ImageInstallRequest request,
                             io.grpc.stub.StreamObserver<gnoi.sonic.SonicGnoi.ImageInstallResponse> responseObserver) {
        LOG.info("Received imageInstall rpc request {}", request);
        final SonicGnoi.ImageInstallResponse response = SonicGnoi.ImageInstallResponse.newBuilder()
                .setOutput(buildSimulatedSonicOutput("imageInstall"))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /*
        Returns StatusRuntimeException, so one can test error handling on client's side.
     */
    @Override
    public void imageRemove(gnoi.sonic.SonicGnoi.ImageRemoveRequest request,
                            io.grpc.stub.StreamObserver<gnoi.sonic.SonicGnoi.ImageRemoveResponse> responseObserver) {
        LOG.info("Received imageRemove rpc request {}", request);
        responseObserver.onError(new StatusRuntimeException(Status.UNKNOWN));
    }

    @Override
    public void imageDefault(gnoi.sonic.SonicGnoi.ImageDefaultRequest request,
                             io.grpc.stub.StreamObserver<gnoi.sonic.SonicGnoi.ImageDefaultResponse> responseObserver) {
        LOG.info("Received imageDefault rpc request {}", request);
        final SonicGnoi.ImageDefaultResponse response = SonicGnoi.ImageDefaultResponse.newBuilder()
                .setOutput(buildSimulatedSonicOutput("imageDefault"))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void clearNeighbors(
            gnoi.sonic.SonicGnoi.ClearNeighborsRequest request,
            io.grpc.stub.StreamObserver<gnoi.sonic.SonicGnoi.ClearNeighborsResponse> responseObserver) {
        LOG.info("Received clearNeighbors rpc request {}", request);
        final SonicGnoi.ClearNeighborsResponse response = SonicGnoi.ClearNeighborsResponse.newBuilder()
                .setOutput(SonicGnoi.ClearNeighborsResponse.Output.newBuilder()
                        .setResponse(String.format(SIMULATED_RESPONSE, "clearNeighbors")))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private static SonicGnoi.SonicOutput buildSimulatedSonicOutput(final String rpcName) {
        return SonicGnoi.SonicOutput.newBuilder()
                .setStatusDetail(String.format(SIMULATED_RESPONSE, rpcName))
                .setStatus(200).build();
    }

}
