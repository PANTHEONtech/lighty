/*
 * Copyright (c) 2019 Pantheon.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.impl.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(SocketAnalyzer.class);
    private static final int SOCKET_PORT_WAIT_TIME = 500;

    public static Boolean awaitPortAvailable(int port, long timeout, TimeUnit timeUnit) throws InterruptedException {

        final long expectedEndTime = System.nanoTime() + timeUnit.toNanos(timeout);

        while (System.nanoTime() <= expectedEndTime) {
            try {
                LOG.debug("Check if port {} is available", port);

                new ServerSocket(port).close();

                LOG.debug("Port {} available", port);
                return true;

            } catch (IOException e) {
                LOG.info("Port {} not available - Awaiting port available 1s", port);
                Thread.sleep(SOCKET_PORT_WAIT_TIME);
            }
        }

        return false;
    }
}
