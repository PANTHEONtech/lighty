/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.session.api;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.lighty.modules.gnmi.connector.configuration.SessionConfiguration;
import io.lighty.modules.gnmi.connector.gnmi.session.api.GnmiSession;
import io.lighty.modules.gnmi.connector.gnoi.session.api.GnoiSession;
import io.lighty.modules.gnmi.connector.gnoi.session.impl.GnoiSessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionProviderImpl implements SessionProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SessionProviderImpl.class);
    private static final String CLOSED_SESSION_MESSAGE = "Session was already closed!";

    private final GnoiSession gnoiSession;
    private final GnmiSession gnmiSession;
    private final SessionConfiguration configuration;
    private final SessionCloseDelegate closeDelegate;
    private final ManagedChannel channel;
    private boolean closed;

    public SessionProviderImpl(final SessionConfiguration configuration, final SessionCloseDelegate closeDelegate,
                               final ManagedChannel channel, final GnmiSession gnmiSession) {
        this.closed = false;
        this.gnmiSession = gnmiSession;
        this.gnoiSession = new GnoiSessionImpl(channel);
        this.configuration = configuration;
        this.closeDelegate = closeDelegate;
        this.channel = channel;
    }

    @Override
    public GnmiSession getGnmiSession() {
        if (this.closed) {
            LOG.error("Unable to get gNMI session: {}", CLOSED_SESSION_MESSAGE);
            throw new IllegalStateException(CLOSED_SESSION_MESSAGE);
        } else {
            return gnmiSession;
        }
    }

    @Override
    public GnoiSession getGnoiSession() {
        if (this.closed) {
            LOG.error("Unable to get gNOI session: {}", CLOSED_SESSION_MESSAGE);
            throw new IllegalStateException(CLOSED_SESSION_MESSAGE);
        } else {
            return gnoiSession;
        }
    }

    @Override
    public SessionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public ConnectivityState getChannelState() {
        return channel.getState(true);
    }

    @Override
    public void notifyOnStateChangedOneOff(final ConnectivityState sourceState, final Runnable callback) {
        LOG.trace("Registering one-off channel state change listener");
        channel.notifyWhenStateChanged(sourceState, callback);
    }

    @Override
    public void close() throws InterruptedException {
        closeDelegate.closeSession(this);
        this.closed = true;
    }

    @Override
    public boolean equals(final Object other) {

        if (!(other instanceof SessionProviderImpl)) {
            return false;
        }

        SessionProviderImpl that = (SessionProviderImpl) other;
        return configuration.equals(that.configuration);
    }

    @Override
    public int hashCode() {
        return configuration.hashCode();
    }
}
