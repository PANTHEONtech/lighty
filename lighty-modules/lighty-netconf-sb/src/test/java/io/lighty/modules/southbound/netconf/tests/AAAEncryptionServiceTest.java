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
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AAAEncryptionServiceTest {

    private AAAEncryptionService aaaEncryptionService;

    @BeforeClass
    public void init() throws ConfigurationException {
        this.aaaEncryptionService = NetconfConfigUtils.createAAAEncryptionService(
                NetconfConfigUtils.getDefaultAaaEncryptServiceConfig());
    }

    @Test
    public void testStringEncryptionDecryption() throws GeneralSecurityException {
        final byte[] rawData = "hello world".getBytes();
        final byte[] encryptedData = this.aaaEncryptionService.encrypt(rawData);
        Assert.assertNotNull(encryptedData);
        Assert.assertNotEquals(encryptedData, rawData);
        final byte[] decryptedData = this.aaaEncryptionService.decrypt(encryptedData);
        Assert.assertNotNull(encryptedData);
        Assert.assertEquals(decryptedData, rawData);
    }

    @Test
    public void testByteEncryptionDecryption() throws GeneralSecurityException {
        final String rawDataString = "hello world";
        final byte[] rawData = rawDataString.getBytes(StandardCharsets.UTF_8);
        final byte[] encryptedData = this.aaaEncryptionService.encrypt(rawData);
        Assert.assertNotNull(encryptedData);
        final String encryptedDataString = new String(encryptedData, StandardCharsets.UTF_8);
        Assert.assertNotEquals(encryptedDataString, rawDataString);
        final byte[] decryptedData = this.aaaEncryptionService.decrypt(encryptedData);
        Assert.assertNotNull(encryptedData);
        final String decryptedDataString = new String(decryptedData, StandardCharsets.UTF_8);
        Assert.assertEquals(decryptedDataString, rawDataString);
    }

    @Test
    public void testNullInputs() throws GeneralSecurityException {
        Assert.assertNull(this.aaaEncryptionService.decrypt(null));
        Assert.assertNull(this.aaaEncryptionService.encrypt(null));
    }

    @Test
    public void testEmptyInputs() throws GeneralSecurityException {
        final byte[] byteData = new byte[0];
        final byte[] decryptedBytes = this.aaaEncryptionService.decrypt(byteData);
        Assert.assertNotNull(decryptedBytes);
        Assert.assertEquals(decryptedBytes.length, 0);
        final byte[] encryptedBytes = this.aaaEncryptionService.encrypt(byteData);
        Assert.assertNotNull(encryptedBytes);
        Assert.assertEquals(encryptedBytes.length, 0);
    }

    @Test
    public void testDecryptBadByteData() throws GeneralSecurityException {
        final byte[] byteData = "test data".getBytes(StandardCharsets.UTF_8);
        final byte[] decryptedBytes = this.aaaEncryptionService.decrypt(byteData);
        Assert.assertEquals(decryptedBytes, byteData);
    }
}