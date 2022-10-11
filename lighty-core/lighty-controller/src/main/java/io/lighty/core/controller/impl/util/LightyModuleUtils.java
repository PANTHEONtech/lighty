/*
 * Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.util;

import io.lighty.core.controller.api.LightyModule;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LightyModuleUtils {

    private static final Logger LOG = LoggerFactory.getLogger(LightyModuleUtils.class);

    private LightyModuleUtils() {
        // utility class
    }

    @SuppressWarnings("IllegalCatch")
    public static boolean stopMultipleModules(final long lightyModuleTimeout, final TimeUnit timeUnit,
           final LightyModule... modules) {
        boolean success = true;

        for (LightyModule module : modules) {
            if (module != null && !stopAndWaitLightyModule(lightyModuleTimeout, timeUnit,
                    module)) {
                success = false;
            }
        }
        return success;
    }

    @SuppressWarnings({"checkstyle:illegalCatch"})
    public static boolean stopAndWaitLightyModule(final long lightyModuleTimeout,
            final TimeUnit timeUnit,
            final LightyModule lightyModule) {
        try {
            LOG.info("Stopping lighty.io module ({})...", lightyModule.getClass());
            final boolean stopSuccess =
                    lightyModule.shutdown().get(lightyModuleTimeout, timeUnit);
            if (stopSuccess) {
                LOG.info("lighty.io module ({}) stopped successfully!", lightyModule.getClass());
                return true;
            } else {
                LOG.error("Unable to stop lighty.io module ({})!", lightyModule.getClass());
                return false;
            }
        } catch (Exception e) {
            LOG.error("Exception was thrown while stopping the lighty.io module ({})!", lightyModule.getClass(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

}
