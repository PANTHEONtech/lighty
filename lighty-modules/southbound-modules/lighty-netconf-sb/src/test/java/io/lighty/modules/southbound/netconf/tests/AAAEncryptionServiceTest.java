package io.lighty.modules.southbound.netconf.tests;

import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.charset.Charset;

public class AAAEncryptionServiceTest {

    private AAAEncryptionService aaaEncryptionService;

    @BeforeClass
    public void init() throws ConfigurationException {
        aaaEncryptionService = NetconfConfigUtils.createAAAEncryptionService(
                NetconfConfigUtils.getDefaultAaaEncryptServiceConfig());
    }

    @Test(enabled = false)
    public void testStringEncryptionDecryption() {
        String rawData = "hello world";
        String encryptedData = aaaEncryptionService.encrypt(rawData);
        Assert.assertNotNull(encryptedData);
        Assert.assertFalse(rawData.equals(encryptedData));
        String decryptedData = aaaEncryptionService.decrypt(encryptedData);
        Assert.assertNotNull(encryptedData);
        Assert.assertTrue(rawData.equals(decryptedData));
    }

    @Test(enabled = false)
    public void testByteEncryptionDecryption() {
        String rawDataString = "hello world";
        byte[] rawData = rawDataString.getBytes(Charset.forName("UTF-8"));
        byte[] encryptedData = aaaEncryptionService.encrypt(rawData);
        Assert.assertNotNull(encryptedData);
        String encryptedDataString = new String(encryptedData, Charset.forName("UTF-8"));
        Assert.assertFalse(rawDataString.equals(encryptedDataString));
        byte[] decryptedData = aaaEncryptionService.decrypt(encryptedData);
        Assert.assertNotNull(encryptedData);
        String decryptedDataString = new String(decryptedData, Charset.forName("UTF-8"));
        Assert.assertTrue(rawDataString.equals(decryptedDataString));
    }

    @Test(enabled = false)
    public void testNullInputs() {
        byte[] byteData = null;
        String stringData = null;
        Assert.assertNull(aaaEncryptionService.decrypt(byteData));
        Assert.assertNull(aaaEncryptionService.encrypt(byteData));
        Assert.assertNull(aaaEncryptionService.decrypt(stringData));
        Assert.assertNull(aaaEncryptionService.encrypt(stringData));
    }

    @Test(enabled = false)
    public void testEmptyInputs() {
        byte[] byteData = new byte[0];
        String stringData = "";
        byte[] decryptedBytes = aaaEncryptionService.decrypt(byteData);
        Assert.assertNotNull(decryptedBytes);
        Assert.assertTrue(decryptedBytes.length == 0);
        byte[] encryptedBytes = aaaEncryptionService.encrypt(byteData);
        Assert.assertNotNull(encryptedBytes);
        Assert.assertTrue(encryptedBytes.length == 0);
        String decryptedString = aaaEncryptionService.decrypt(stringData);
        Assert.assertNotNull(decryptedString);
        Assert.assertTrue(decryptedString.equals(stringData));
        String encryptedString = aaaEncryptionService.encrypt(stringData);
        Assert.assertNotNull(encryptedString);
        Assert.assertTrue(encryptedString.equals(stringData));
    }
}