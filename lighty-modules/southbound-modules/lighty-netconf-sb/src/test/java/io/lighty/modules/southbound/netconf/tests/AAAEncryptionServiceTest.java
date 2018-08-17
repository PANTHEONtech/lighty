package io.lighty.modules.southbound.netconf.tests;

import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import java.nio.charset.Charset;
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

    @Test(enabled = false)
    public void testStringEncryptionDecryption() {
        final String rawData = "hello world";
        final String encryptedData = this.aaaEncryptionService.encrypt(rawData);
        Assert.assertNotNull(encryptedData);
        Assert.assertFalse(rawData.equals(encryptedData));
        final String decryptedData = this.aaaEncryptionService.decrypt(encryptedData);
        Assert.assertNotNull(encryptedData);
        Assert.assertTrue(rawData.equals(decryptedData));
    }

    @Test
    public void testByteEncryptionDecryption() {
        final String rawDataString = "hello world";
        final byte[] rawData = rawDataString.getBytes(Charset.forName("UTF-8"));
        final byte[] encryptedData = this.aaaEncryptionService.encrypt(rawData);
        Assert.assertNotNull(encryptedData);
        final String encryptedDataString = new String(encryptedData, Charset.forName("UTF-8"));
        Assert.assertFalse(rawDataString.equals(encryptedDataString));
        final byte[] decryptedData = this.aaaEncryptionService.decrypt(encryptedData);
        Assert.assertNotNull(encryptedData);
        final String decryptedDataString = new String(decryptedData, Charset.forName("UTF-8"));
        Assert.assertTrue(rawDataString.equals(decryptedDataString));
    }

    @Test
    public void testNullInputs() {
        final byte[] byteData = null;
        final String stringData = null;
        Assert.assertNull(this.aaaEncryptionService.decrypt(byteData));
        Assert.assertNull(this.aaaEncryptionService.encrypt(byteData));
        Assert.assertNull(this.aaaEncryptionService.decrypt(stringData));
        Assert.assertNull(this.aaaEncryptionService.encrypt(stringData));
    }

    @Test
    public void testEmptyInputs() {
        final byte[] byteData = new byte[0];
        final String stringData = "";
        final byte[] decryptedBytes = this.aaaEncryptionService.decrypt(byteData);
        Assert.assertNotNull(decryptedBytes);
        Assert.assertTrue(decryptedBytes.length == 0);
        final byte[] encryptedBytes = this.aaaEncryptionService.encrypt(byteData);
        Assert.assertNotNull(encryptedBytes);
        Assert.assertTrue(encryptedBytes.length == 0);
        final String decryptedString = this.aaaEncryptionService.decrypt(stringData);
        Assert.assertNotNull(decryptedString);
        Assert.assertTrue(decryptedString.equals(stringData));
        final String encryptedString = this.aaaEncryptionService.encrypt(stringData);
        Assert.assertNotNull(encryptedString);
        Assert.assertTrue(encryptedString.equals(stringData));
    }
}