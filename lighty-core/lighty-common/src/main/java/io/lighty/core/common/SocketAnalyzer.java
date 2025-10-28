/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.common;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SocketAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(SocketAnalyzer.class);
    private static final int SOCKET_PORT_WAIT_TIME = 500;

    private SocketAnalyzer() {

    }

    public static Boolean awaitPortAvailable(final int port, final long timeout, final TimeUnit timeUnit)
            throws InterruptedException {

        final long expectedEndTime = System.nanoTime() + timeUnit.toNanos(timeout);

        while (System.nanoTime() <= expectedEndTime) {
            LOG.debug("Check if port {} is available", port);
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                LOG.debug("Port {} available", port);
                return true;
            } catch (IOException e) {
                LOG.info("Port {} is not available - Awaiting port availability 1s", port);
                Thread.sleep(SOCKET_PORT_WAIT_TIME);
            }
        }

        return false;
    }
}
