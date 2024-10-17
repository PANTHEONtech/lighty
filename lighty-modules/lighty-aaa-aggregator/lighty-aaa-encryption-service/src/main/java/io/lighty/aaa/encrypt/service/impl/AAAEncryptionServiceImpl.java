/*
 * Copyright © 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa.encrypt.service.impl;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AAAEncryptionServiceImpl implements AAAEncryptionService {

    private static final Logger LOG = LoggerFactory.getLogger(AAAEncryptionServiceImpl.class);

    private final String cipherTransforms;
    private final SecretKey key;
    private final byte[] iv;
    private final int tagLength;

    public AAAEncryptionServiceImpl(final GCMParameterSpec gcmParameterSpec, final String cipherTransforms,
            final SecretKey key) {
        this.iv = gcmParameterSpec.getIV();
        this.tagLength = gcmParameterSpec.getTLen();
        this.cipherTransforms = cipherTransforms;
        this.key = key;
    }

    @Override
    public byte[] encrypt(final byte[] data) {
        if (data != null && data.length != 0) {
            final Cipher encryptCipher;
            try {
                encryptCipher = initCipher(Cipher.ENCRYPT_MODE, iv);
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Failed to create encrypt cipher.", e);
            }
            final byte[] encryptedData;
            try {
                encryptedData = encryptCipher.doFinal(requireNonNull(data));
                return ByteBuffer.allocate(iv.length + encryptedData.length)
                    .put(iv)
                    .put(encryptedData)
                    .array();
            } catch (final IllegalBlockSizeException | BadPaddingException e) {
                LOG.error("Failed to encrypt data.", e);
                return data;
            }
        }
        else {
            LOG.warn("encrypt data is empty or null.");
            return data;
        }
    }

    @Override
    public byte[] decrypt(final byte[] encryptedDataWithIv) {
        if (encryptedDataWithIv != null && encryptedDataWithIv.length != 0) {
            final var ivLength = iv.length;
            if (encryptedDataWithIv.length < ivLength) {
                LOG.error("Invalid encrypted data length.");
                return encryptedDataWithIv;
            }
            final var byteBuffer = ByteBuffer.wrap(encryptedDataWithIv);

            final var localIv = new byte[ivLength];
            byteBuffer.get(localIv);

            final var encryptedData = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedData);

            final Cipher decryptCipher;
            try {
                decryptCipher = initCipher(Cipher.DECRYPT_MODE, iv);
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Failed to create decrypt cipher.", e);
            }
            try {
                return decryptCipher.doFinal(requireNonNull(encryptedData));
            } catch (final IllegalBlockSizeException | BadPaddingException e) {
                LOG.error("Failed to decrypt data", e);
                return encryptedData;
            }
        }
        else {
            LOG.warn("decrypt data is empty or null.");
            return encryptedDataWithIv;
        }
    }

    private Cipher initCipher(final int mode, final byte[] localIv) throws
        NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        final var cipher = Cipher.getInstance(cipherTransforms);
        cipher.init(mode, key, new GCMParameterSpec(tagLength, localIv));
        return cipher;
    }
}
