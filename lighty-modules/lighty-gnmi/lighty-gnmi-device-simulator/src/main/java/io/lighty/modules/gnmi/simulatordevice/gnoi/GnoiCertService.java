/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.gnoi;

import com.google.protobuf.ByteString;
import gnoi.certificate.Cert;
import gnoi.certificate.CertificateManagementGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnoiCertService extends CertificateManagementGrpc.CertificateManagementImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(GnoiCertService.class);

    public static final Cert.CertificateInfo CERTIFICATE_INFO = Cert.CertificateInfo.newBuilder()
            .setCertificate(Cert.Certificate.newBuilder()
                    .setType(Cert.CertificateType.CT_X509)
                    .setCertificate(ByteString.copyFromUtf8("0000"))
                    .build())
            .setCertificateId("test-cert")
            .addEndpoints(Cert.Endpoint.newBuilder()
                    .setType(Cert.Endpoint.Type.EP_IPSEC_TUNNEL)
                    .setEndpoint("test-endpoint")
                    .build())
            .setModificationTime(System.currentTimeMillis())
            .build();


    @Override
    public StreamObserver<Cert.InstallCertificateRequest> install(
            final StreamObserver<Cert.InstallCertificateResponse> responseObserver) {
        LOG.info("Received install rpc.");

        return new StreamObserver<>() {
            @Override
            public void onNext(final Cert.InstallCertificateRequest value) {
                LOG.info("Received install rpc value: {}.", value);

                responseObserver.onNext(Cert.InstallCertificateResponse.newBuilder()
                        .setLoadCertificate(Cert.LoadCertificateResponse.getDefaultInstance()).build());
            }

            @Override
            public void onError(final Throwable throwable) {
                LOG.error("Install rpc failed.", throwable);
            }

            @Override
            public void onCompleted() {
                LOG.info("Install rpc finished.");
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void getCertificates(final Cert.GetCertificatesRequest request,
                                final StreamObserver<Cert.GetCertificatesResponse> responseObserver) {
        LOG.info("Received get certificates rpc. {}", request);
        responseObserver.onNext(Cert.GetCertificatesResponse.newBuilder().addCertificateInfo(CERTIFICATE_INFO).build());
        responseObserver.onCompleted();
    }

}
