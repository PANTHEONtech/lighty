/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.modules.southbound.netconf.impl;

import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;

public class AAAEncryptionServiceImpl implements AAAEncryptionService {

    private static final Logger LOG = LoggerFactory.getLogger(AAAEncryptionServiceImpl.class);

    private final Cipher encryptCipher;
    private final Cipher decryptCipher;

    public AAAEncryptionServiceImpl(Cipher encryptCipher, Cipher decryptCipher) {
        this.encryptCipher = encryptCipher;
        this.decryptCipher = decryptCipher;
    }

    @Override
    public String encrypt(String data) {
        if (data != null && data.length() != 0 ) {
            try {
                synchronized (encryptCipher) {
                    byte[] cryptobytes = encryptCipher.doFinal(data.getBytes(Charset.defaultCharset()));
                    String cryptostring = DatatypeConverter.printBase64Binary(cryptobytes);
                    return cryptostring;
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
    public String decrypt(String encryptedData) {
        if (encryptedData != null && encryptedData.length() != 0) {
            try {
                byte[] cryptobytes = DatatypeConverter.parseBase64Binary(encryptedData);
                byte[] clearbytes = decryptCipher.doFinal(cryptobytes);
                return new String(clearbytes, Charset.defaultCharset());
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                LOG.error("Failed to decrypt encoded data", e);
                return encryptedData;
            }
        } else {
            LOG.warn("encryptedData is empty or null.");
            return encryptedData;
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
