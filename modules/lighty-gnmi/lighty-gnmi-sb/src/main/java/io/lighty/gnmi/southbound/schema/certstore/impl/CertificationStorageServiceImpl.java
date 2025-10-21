/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema.certstore.impl;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.gnmi.southbound.schema.certstore.service.CertificationStorageService;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Optional;
import javax.xml.bind.DatatypeConverter;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.AddKeystoreCertificateInput;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.Keystore;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.KeystoreBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.KeystoreKey;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.RemoveKeystoreCertificateInput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class CertificationStorageServiceImpl implements CertificationStorageService {

    private final AAAEncryptionService encryptionService;
    private final DataBroker dataBroker;

    public CertificationStorageServiceImpl(final AAAEncryptionService encryptionService, final DataBroker dataBroker) {
        this.encryptionService = encryptionService;
        this.dataBroker = dataBroker;
    }

    @Override
    public @NonNull ListenableFuture<? extends CommitInfo> writeCertificates(final AddKeystoreCertificateInput input)
            throws GeneralSecurityException {
        byte[] encryptedParaphrase = this.encryptionService.encrypt(
                input.getPassphrase() != null ? input.getPassphrase().getBytes(Charset.defaultCharset()) : null);
        final Keystore keystore = new KeystoreBuilder()
                .setKeystoreId(input.getKeystoreId())
                .setClientKey(DatatypeConverter.printBase64Binary(
                        this.encryptionService.encrypt(input.getClientKey().getBytes(Charset.defaultCharset()))))
                .setPassphrase(
                        encryptedParaphrase != null ? DatatypeConverter.printBase64Binary(encryptedParaphrase) : null)
                .setClientCert(input.getClientCert())
                .setCaCertificate(input.getCaCertificate())
                .build();

        final WriteTransaction writeTX = this.dataBroker.newWriteOnlyTransaction();
        writeTX.merge(LogicalDatastoreType.OPERATIONAL, getKeystoreII(input.getKeystoreId()), keystore);
        return writeTX.commit();
    }

    @Override
    public @NonNull ListenableFuture<? extends CommitInfo> removeCertificates(
            final RemoveKeystoreCertificateInput input) {
        final WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.OPERATIONAL, getKeystoreII(input.getKeystoreId()));
        return writeTransaction.commit();
    }

    @Override
    public @NonNull ListenableFuture<Optional<Keystore>> readCertificate(final String keystoreId) {
        try (ReadTransaction readOnlyTransaction = this.dataBroker.newReadOnlyTransaction()) {
            return readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, getKeystoreII(keystoreId));
        }
    }

    @Override
    public String decrypt(final String data) throws GeneralSecurityException {
        return new String(this.encryptionService.decrypt(DatatypeConverter.parseBase64Binary(data)),
                Charset.defaultCharset());
    }

    private InstanceIdentifier<Keystore> getKeystoreII(final String keystoreId) {
        return InstanceIdentifier
                .builder(Keystore.class, new KeystoreKey(keystoreId))
                .build();
    }
}
