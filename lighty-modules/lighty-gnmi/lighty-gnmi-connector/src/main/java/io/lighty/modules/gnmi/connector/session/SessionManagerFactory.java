/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.session;

import io.lighty.modules.gnmi.connector.security.Security;
import io.lighty.modules.gnmi.connector.session.api.SessionManager;

public interface SessionManagerFactory {

    SessionManager createSessionManager(Security security);
}
