/*
 * Copyright (c) 2018-2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnforcerProducer {

    private static final Logger LOG = LoggerFactory.getLogger(EnforcerProducer.class);

    private final Enforcer enforcer;

    public EnforcerProducer() throws IOException {
        this("/data/security/authz_model.conf", "/data/security/authz_policy.csv");
    }

    public EnforcerProducer(String modelClassPath, String policyClassPath) throws IOException {
        LOG.info("initializing enforcer ...");
        Model model = new Model();
        Adapter adapter = new IsAdapter(policyClassPath);

        try (final InputStream is = EnforcerProducer.class.getResourceAsStream(modelClassPath);
                final BufferedReader bufferedReader =
                        new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String text = bufferedReader
                    .lines().collect(Collectors.joining("\n"));
            model.loadModelFromText(text);
            this.enforcer = new Enforcer(model, adapter);
        }
    }

    @Bean
    public Enforcer getEnforcer() {
        return this.enforcer;
    }

}
