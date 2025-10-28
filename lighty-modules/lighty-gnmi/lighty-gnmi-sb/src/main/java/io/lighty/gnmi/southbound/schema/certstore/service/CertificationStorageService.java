/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema.certstore.service;

import com.google.common.util.concurrent.ListenableFuture;
import java.security.GeneralSecurityException;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.AddKeystoreCertificateInput;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.Keystore;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.RemoveKeystoreCertificateInput;

public interface CertificationStorageService {

    @NonNull ListenableFuture<? extends CommitInfo> writeCertificates(AddKeystoreCertificateInput input)
            throws GeneralSecurityException;

    @NonNull ListenableFuture<? extends CommitInfo> removeCertificates(RemoveKeystoreCertificateInput input);

    @NonNull ListenableFuture<Optional<Keystore>> readCertificate(String keystoreId);

    String decrypt(String data) throws GeneralSecurityException;
}
