/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.tests;

import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import java.nio.charset.StandardCharsets;
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
    public void testStringEncryptionDecryption() {
        final String rawData = "hello world";
        final String encryptedData = this.aaaEncryptionService.encrypt(rawData);
        Assert.assertNotNull(encryptedData);
        Assert.assertNotEquals(encryptedData, rawData);
        final String decryptedData = this.aaaEncryptionService.decrypt(encryptedData);
        Assert.assertNotNull(encryptedData);
        Assert.assertEquals(decryptedData, rawData);
    }

    @Test
    public void testByteEncryptionDecryption() {
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
    public void testNullInputs() {
        Assert.assertNull(this.aaaEncryptionService.decrypt((byte[]) null));
        Assert.assertNull(this.aaaEncryptionService.encrypt((byte[]) null));
        Assert.assertNull(this.aaaEncryptionService.decrypt((String) null));
        Assert.assertNull(this.aaaEncryptionService.encrypt((String) null));
    }

    @Test
    public void testEmptyInputs() {
        final byte[] byteData = new byte[0];
        final String stringData = "";
        final byte[] decryptedBytes = this.aaaEncryptionService.decrypt(byteData);
        Assert.assertNotNull(decryptedBytes);
        Assert.assertEquals(decryptedBytes.length, 0);
        final byte[] encryptedBytes = this.aaaEncryptionService.encrypt(byteData);
        Assert.assertNotNull(encryptedBytes);
        Assert.assertEquals(encryptedBytes.length, 0);
        final String decryptedString = this.aaaEncryptionService.decrypt(stringData);
        Assert.assertNotNull(decryptedString);
        Assert.assertEquals(stringData, decryptedString);
        final String encryptedString = this.aaaEncryptionService.encrypt(stringData);
        Assert.assertNotNull(encryptedString);
        Assert.assertEquals(stringData, encryptedString);
    }

    @Test
    public void testDecryptBadByteData() {
        final byte[] byteData = "test data".getBytes(StandardCharsets.UTF_8);
        final byte[] decryptedBytes = this.aaaEncryptionService.decrypt(byteData);
        Assert.assertEquals(decryptedBytes, byteData);
    }
}