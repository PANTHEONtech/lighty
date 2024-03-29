/*
 * Copyright (c) 2023 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.springboot.config;

import io.lighty.core.controller.springboot.services.UserAccessService;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@EnableWebSecurity
@Configuration
@Profile("!deployed") //Not(!) deployed profile
public class SecurityConfigurationNotDeployed {

    private final Enforcer enforcer;
    private final UserAccessService userAccessService;

    @Autowired
    public SecurityConfigurationNotDeployed(Enforcer enforcer, UserAccessService userAccessService) {
        this.enforcer = enforcer;
        this.userAccessService = userAccessService;
    }

    @Bean
    @Order(1)
    protected SecurityFilterChain auth0FilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf()
                .disable()
                .addFilterBefore(new JCasBinFilter(enforcer, userAccessService), BasicAuthenticationFilter.class)
                .securityMatcher("/services/data/**")
                .build();
    }
}
