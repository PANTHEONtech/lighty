/*
 * Copyright (c) 2018-2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.common;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SocketAnalyzerTest {

    private static final long TIMEOUT = 3;

    @Test
    public void socketAnalyzerAwaitPortSuccess() throws IOException, InterruptedException {
        final int availablePort = findAvailablePort();
        try (ServerSocket ignored = new ServerSocket(availablePort)) {
            Assert.assertFalse(SocketAnalyzer.awaitPortAvailable(availablePort, TIMEOUT, TimeUnit.SECONDS));
        }
    }

    private static int findAvailablePort() throws InterruptedException {
        for (int port = 50000; port < 80000; port++) {
            if (SocketAnalyzer.awaitPortAvailable(port, 1, TimeUnit.SECONDS)) {
                return port;
            }
        }
        throw new IllegalStateException();
    }
}