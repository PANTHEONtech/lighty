/*
 * Copyright (c) 2018-2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.config;

import io.lighty.core.controller.springboot.services.UserAccessService;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final Enforcer enforcer;
    private final UserAccessService userAccessService;

    @Autowired
    public SecurityConfiguration(Enforcer enforcer, UserAccessService userAccessService) {
        this.enforcer = enforcer;
        this.userAccessService = userAccessService;
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .addFilterBefore(new JCasBinFilter(enforcer, userAccessService), BasicAuthenticationFilter.class)
                .antMatcher("/services/data/**")
                .csrf()
                .disable();
    }

}
