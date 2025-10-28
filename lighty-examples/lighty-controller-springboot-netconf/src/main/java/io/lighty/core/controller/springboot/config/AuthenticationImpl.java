/*
 * Copyright (c) 2018-2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AuthenticationImpl implements Authentication {

    private final String name;
    private final Collection<GrantedAuthorityImpl> grantedAuthorities;

    private boolean isAuthenticated;

    public AuthenticationImpl(String name, List<String> roles) {
        this.name = name;
        List<GrantedAuthorityImpl> authorities = new ArrayList<>();
        roles.forEach(r->{
            authorities.add(new GrantedAuthorityImpl(r));
        });
        this.grantedAuthorities = Collections.unmodifiableCollection(authorities);
        this.isAuthenticated = true;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return grantedAuthorities;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return name;
    }

    @Override
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.isAuthenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return name;
    }

}
