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
