/*
 * Copyright © 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa.encrypt.service.impl;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AAAEncryptionServiceImpl implements AAAEncryptionService {

    private static final Logger LOG = LoggerFactory.getLogger(AAAEncryptionServiceImpl.class);
    private static final int GCM_IV_LENGTH = 12;

    private final Cipher encryptCipher;
    private final Cipher decryptCipher;
    private final SecretKey key;
    final SecureRandom random;

    public AAAEncryptionServiceImpl(Cipher encryptCipher, Cipher decryptCipher, SecretKey key) {
        this.encryptCipher = encryptCipher;
        this.decryptCipher = decryptCipher;
        this.key = key;
        this.random = new SecureRandom();
    }

    @Override
    public byte[] encrypt(final byte[] data) {
        if (data != null && data.length != 0) {
            try {
                final byte[] iv = new byte[GCM_IV_LENGTH];
                random.nextBytes(iv);
                final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);

                synchronized (encryptCipher) {
                    encryptCipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
                    final byte[] encryptedData = encryptCipher.doFinal(data);
                    final byte[] result = new byte[iv.length + encryptedData.length];
                    System.arraycopy(iv, 0, result, 0, iv.length);
                    System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);
                    return result;
                }
            } catch (final IllegalBlockSizeException | BadPaddingException
                | InvalidAlgorithmParameterException | InvalidKeyException e) {
                LOG.error("Failed to encrypt data.", e);
                return data;
            }
        } else {
            LOG.warn("encrypt data is empty or null.");
            return data;
        }
    }

    @Override
    public byte[] decrypt(final byte[] encryptedData) {
        if (encryptedData != null && encryptedData.length != 0) {
            try {
                if (encryptedData.length < GCM_IV_LENGTH) {
                    LOG.warn("Invalid encrypted data length (data unencrypted or missing IV)");
                    return encryptedData;
                }
                final byte[] iv = new byte[GCM_IV_LENGTH];
                System.arraycopy(encryptedData, 0, iv, 0, iv.length);
                final byte[] ciphertext = new byte[encryptedData.length - iv.length];
                System.arraycopy(encryptedData, iv.length, ciphertext, 0, ciphertext.length);

                final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);

                synchronized (decryptCipher) {
                    decryptCipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
                    return decryptCipher.doFinal(ciphertext);
                }
            } catch (final IllegalBlockSizeException | BadPaddingException
                | InvalidAlgorithmParameterException | InvalidKeyException e) {
                LOG.error("Failed to decrypt data", e);
                return encryptedData;
            }
        } else {
            LOG.warn("decrypt data is empty or null.");
            return encryptedData;
        }
    }

}
