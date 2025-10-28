/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.spring;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    HelloMessage getHello() {
        final HelloMessage helloMessage = new HelloMessage("Hello");
        LOG.info("initialized with bean dummy HelloMessage {}", helloMessage);
        return helloMessage;
    }

    @PostConstruct
    private void init() {
        LOG.info("initialized");
    }

    public static class HelloMessage {

        private final String message;

        public HelloMessage(String message) {

            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "HelloMessage{" + "message='" + message + '\'' + '}';
        }
    }
}
