/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.session.api;

/**
 * This is internal interface used to manage channel caching.
 */
public interface SessionCloseDelegate {

    /**
     * Closes channel backing this session if no other session is using same channel.
     *
     * @param session session to close
     * @throws InterruptedException when interrupted while waiting for channel to close
     */
    void closeSession(SessionProvider session) throws InterruptedException;

}
