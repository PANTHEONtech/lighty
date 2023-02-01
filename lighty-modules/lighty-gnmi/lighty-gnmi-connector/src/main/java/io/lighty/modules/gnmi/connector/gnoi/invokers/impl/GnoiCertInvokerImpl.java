/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnoi.invokers.impl;

import gnoi.certificate.Cert;
import gnoi.certificate.CertificateManagementGrpc;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import io.lighty.modules.gnmi.connector.gnoi.invokers.api.GnoiCertInvoker;

public final class GnoiCertInvokerImpl implements GnoiCertInvoker {

    private final CertificateManagementGrpc.CertificateManagementStub futureStub;

    private GnoiCertInvokerImpl(CertificateManagementGrpc.CertificateManagementStub futureStub) {
        this.futureStub = futureStub;
    }

    public static GnoiCertInvoker fromChannel(Channel channel) {
        return new GnoiCertInvokerImpl(CertificateManagementGrpc.newStub(channel));
    }


    @Override
    public StreamObserver<Cert.RotateCertificateRequest> rotate(
            StreamObserver<Cert.RotateCertificateResponse> responseObserver) {
        return futureStub.rotate(responseObserver);
    }

    @Override
    public void generateCSR(Cert.GenerateCSRRequest request,
            StreamObserver<Cert.GenerateCSRResponse> responseObserver) {
        futureStub.generateCSR(request, responseObserver);
    }

    @Override
    public void loadCertificate(Cert.LoadCertificateRequest request,
            StreamObserver<Cert.LoadCertificateResponse> responseObserver) {
        futureStub.loadCertificate(request, responseObserver);
    }

    @Override
    public void loadCertificateAuthorityBundle(
            Cert.LoadCertificateAuthorityBundleRequest request,
            StreamObserver<Cert.LoadCertificateAuthorityBundleResponse> responseObserver) {
        futureStub.loadCertificateAuthorityBundle(request, responseObserver);
    }

    @Override
    public void revokeCertificates(Cert.RevokeCertificatesRequest request,
            StreamObserver<Cert.RevokeCertificatesResponse> responseObserver) {
        futureStub.revokeCertificates(request, responseObserver);
    }

    @Override
    public StreamObserver<Cert.InstallCertificateRequest> install(
            StreamObserver<Cert.InstallCertificateResponse> responseObserver) {
        return futureStub.install(responseObserver);
    }

    @Override
    public void canGenerateCSR(Cert.CanGenerateCSRRequest request,
            StreamObserver<Cert.CanGenerateCSRResponse> responseObserver) {
        futureStub.canGenerateCSR(request, responseObserver);
    }

    @Override
    public void getCertificates(Cert.GetCertificatesRequest request,
            StreamObserver<Cert.GetCertificatesResponse> responseObserver) {
        futureStub.getCertificates(request, responseObserver);
    }
}
