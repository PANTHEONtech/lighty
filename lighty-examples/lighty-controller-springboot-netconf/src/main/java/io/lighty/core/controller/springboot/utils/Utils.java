/*
 * Copyright (c) 2018-2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.utils;

import org.slf4j.Logger;
import org.springframework.security.core.Authentication;

public final class Utils {

    private Utils() {
        throw new UnsupportedOperationException("please do not instantiate utility class !");
    }

    public static void logUserData(Logger logger, Authentication authentication) {
        logger.info("authentication={}", authentication.getName());
        authentication.getAuthorities().forEach(a->{
            logger.info("  authority={}", a.getAuthority());
        });
    }

}
