/*
 * Copyright (c) 2018-2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.services;

import io.lighty.core.controller.springboot.services.dto.LoginRequest;
import io.lighty.core.controller.springboot.services.dto.UserData;

import java.util.Optional;

/**
 * Service for user session authentication.
 */
public interface UserAccessService {

    /**
     * Login session with username / password credentials.
     * @param sessionId unique session id to login
     * @param loginRequest username / password credentials
     * @return {@link Optional} of {@link UserData}, present if user credentials are valid, empty otherwise.
     */
    Optional<UserData> login(String sessionId, LoginRequest loginRequest);

    /**
     * Check is session is authenticated.
     * @param sessionId unique session id to check
     * @return {@link Optional} of {@link UserData}, present if session has been previously authenticated, empty otherwise.
     */
    Optional<UserData> isAuthenticated(String sessionId);

    /**
     * Logout session.
     * @param sessionId unique session id to logout
     */
    void logout(String sessionId);

}
