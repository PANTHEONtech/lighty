/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.tests;

import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;

class AAAEncryptionServiceTest {

    private static AAAEncryptionService aaaEncryptionService;

    @BeforeAll
    static void init() throws ConfigurationException {
        aaaEncryptionService = NetconfConfigUtils.createAAAEncryptionService(
                NetconfConfigUtils.getDefaultAaaEncryptServiceConfig());
    }

    @Test
    void testStringEncryptionDecryption() throws GeneralSecurityException {
        final byte[] rawData = "hello world".getBytes();
        final byte[] encryptedData = aaaEncryptionService.encrypt(rawData);
        Assertions.assertNotNull(encryptedData);
        Assertions.assertNotEquals(encryptedData, rawData);
        final byte[] decryptedData = aaaEncryptionService.decrypt(encryptedData);
        Assertions.assertNotNull(encryptedData);
        Assertions.assertArrayEquals(decryptedData, rawData);
    }

    @Test
    void testByteEncryptionDecryption() throws GeneralSecurityException {
        final String rawDataString = "hello world";
        final byte[] rawData = rawDataString.getBytes(StandardCharsets.UTF_8);
        final byte[] encryptedData = aaaEncryptionService.encrypt(rawData);
        Assertions.assertNotNull(encryptedData);
        final String encryptedDataString = new String(encryptedData, StandardCharsets.UTF_8);
        Assertions.assertNotEquals(rawDataString, encryptedDataString);
        final byte[] decryptedData = aaaEncryptionService.decrypt(encryptedData);
        Assertions.assertNotNull(encryptedData);
        final String decryptedDataString = new String(decryptedData, StandardCharsets.UTF_8);
        Assertions.assertEquals(rawDataString, decryptedDataString);
    }

    @Test
    void testNullInputs() throws GeneralSecurityException {
        Assertions.assertNull(aaaEncryptionService.decrypt(null));
        Assertions.assertNull(aaaEncryptionService.encrypt(null));
    }

    @Test
    void testEmptyInputs() throws GeneralSecurityException {
        final byte[] byteData = new byte[0];
        final byte[] decryptedBytes = aaaEncryptionService.decrypt(byteData);
        Assertions.assertNotNull(decryptedBytes);
        Assertions.assertEquals(0, decryptedBytes.length);
        final byte[] encryptedBytes = aaaEncryptionService.encrypt(byteData);
        Assertions.assertNotNull(encryptedBytes);
        Assertions.assertEquals(0, encryptedBytes.length);
    }

    @Test
    void testDecryptBadByteData() throws GeneralSecurityException {
        final byte[] byteData = "test data".getBytes(StandardCharsets.UTF_8);
        final byte[] decryptedBytes = aaaEncryptionService.decrypt(byteData);
        Assertions.assertEquals(decryptedBytes, byteData);
    }
}