/*
 * Copyright (c) 2020 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa;

import io.lighty.aaa.service.ManagedService;
import io.lighty.core.controller.impl.config.ConfigurationException;
import java.util.Dictionary;
import org.opendaylight.aaa.api.Authentication;
import org.opendaylight.aaa.api.AuthenticationService;

public class AuthenticationManager implements AuthenticationService, ManagedService {

    private static final String AUTH_ENABLED_ERR = "Error setting authEnabled";

    protected static final String AUTH_ENABLED = "authEnabled";

    // In non-Karaf environments, authEnabled is set to false by default
    private volatile boolean authEnabled = false;

    private final ThreadLocal<Authentication> auth = new InheritableThreadLocal<>();

    public AuthenticationManager() {
    }

    @Override
    public Authentication get() {
        return auth.get();
    }

    @Override
    public void set(Authentication authentication) {
        auth.set(authentication);
    }

    @Override
    public void clear() {
        auth.remove();
    }

    @Override
    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties == null) {
            return;
        }

        String propertyValue = (String) properties.get(AUTH_ENABLED);
        boolean isTrueString = Boolean.parseBoolean(propertyValue);
        if (!isTrueString && !"false".equalsIgnoreCase(propertyValue)) {
            throw new ConfigurationException(AUTH_ENABLED_ERR);
        }
        authEnabled = isTrueString;
    }
}