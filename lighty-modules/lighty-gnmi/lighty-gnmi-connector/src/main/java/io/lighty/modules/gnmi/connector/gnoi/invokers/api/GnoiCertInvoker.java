/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnoi.invokers.api;

import gnoi.certificate.Cert;
import io.grpc.stub.StreamObserver;

/**
 * Interface exposing gnoi-cert methods.
 */
public interface GnoiCertInvoker {

    StreamObserver<Cert.RotateCertificateRequest> rotate(
            StreamObserver<Cert.RotateCertificateResponse> responseObserver);

    void generateCSR(Cert.GenerateCSRRequest request,
                     StreamObserver<Cert.GenerateCSRResponse> responseObserver);

    void loadCertificate(Cert.LoadCertificateRequest request,
                         StreamObserver<Cert.LoadCertificateResponse> responseObserver);

    void loadCertificateAuthorityBundle(Cert.LoadCertificateAuthorityBundleRequest request,
                                        StreamObserver<Cert.LoadCertificateAuthorityBundleResponse> responseObserver);

    void revokeCertificates(Cert.RevokeCertificatesRequest request,
                            StreamObserver<Cert.RevokeCertificatesResponse> responseObserver);

    StreamObserver<Cert.InstallCertificateRequest> install(
            StreamObserver<Cert.InstallCertificateResponse> responseObserver);

    void canGenerateCSR(Cert.CanGenerateCSRRequest request,
                        StreamObserver<Cert.CanGenerateCSRResponse> responseObserver);

    void getCertificates(Cert.GetCertificatesRequest request,
                         StreamObserver<Cert.GetCertificatesResponse> responseObserver);
}
