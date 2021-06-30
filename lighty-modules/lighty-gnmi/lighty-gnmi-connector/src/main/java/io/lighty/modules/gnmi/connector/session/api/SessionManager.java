/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.session.api;

import io.lighty.modules.gnmi.connector.configuration.SessionConfiguration;

/**
 * Instance of this class provides creation of {@link SessionProvider} instance. For more information about gNMI see
 * <a href="https://github.com/openconfig/reference/blob/master/rpc/gnmi/gnmi-specification.md">official specification</a>.
 */
public interface SessionManager extends SessionAdmin {

    /**
     * Create new gNMI and gNOI session to specific server (target).
     *
     * @param sessionConfiguration configuration of session
     * @return session instance
     */
    SessionProvider createSession(SessionConfiguration sessionConfiguration);

}
