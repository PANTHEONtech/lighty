package io.lighty.core.common;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

public class SocketAnalyzerTest {

    private static final long TIMEOUT = 3;

    @Test
    public void socketAnalyzerAwaitPortSuccess() {
        try {
            Assert.assertTrue(SocketAnalyzer.awaitPortAvailable(51050, TIMEOUT, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Assert.fail("SocketAnalyzer awaitPortAvailable failed.", e);
        }
    }

    @Test
    public void socketAnalyzerAwaitPortNotAvailable() {
        try {
            Assert.assertFalse(SocketAnalyzer.awaitPortAvailable(28, TIMEOUT, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Assert.fail("SocketAnalyzer awaitPortAvailable failed.", e);
        }
    }
}