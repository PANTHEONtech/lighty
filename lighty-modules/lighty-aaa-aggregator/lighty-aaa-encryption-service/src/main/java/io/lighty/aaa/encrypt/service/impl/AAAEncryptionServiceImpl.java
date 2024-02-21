/*
 * Copyright © 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa.encrypt.service.impl;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AAAEncryptionServiceImpl implements AAAEncryptionService {

    private static final Logger LOG = LoggerFactory.getLogger(AAAEncryptionServiceImpl.class);

    private final Cipher encryptCipher;
    private final Cipher decryptCipher;

    public AAAEncryptionServiceImpl(Cipher encryptCipher, Cipher decryptCipher) {
        this.encryptCipher = encryptCipher;
        this.decryptCipher = decryptCipher;
    }

    @Override
    public byte[] encrypt(byte[] data) {
        if (data != null && data.length != 0) {
            try {
                synchronized (encryptCipher) {
                    return encryptCipher.doFinal(data);
                }
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                LOG.error("Failed to encrypt data.", e);
                return data;
            }
        } else {
            LOG.warn("data is empty or null.");
            return data;
        }
    }

    @Override
    public byte[] decrypt(byte[] encryptedData) {
        if (encryptedData != null && encryptedData.length != 0) {
            try {
                return decryptCipher.doFinal(encryptedData);
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                LOG.error("Failed to decrypt encoded data", e);
                return encryptedData;
            }
        } else {
            LOG.warn("encryptedData is empty or null.");
            return encryptedData;
        }
    }

}
