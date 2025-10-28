/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnmi.session.impl;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.ManagedChannel;
import io.lighty.modules.gnmi.connector.configuration.SessionConfiguration;
import io.lighty.modules.gnmi.connector.gnmi.session.api.GnmiSession;
import io.lighty.modules.gnmi.connector.security.GnmiCallCredentials;

/**
 * This factory provides creation of {@link GnmiSession} instance.
 */
public class GnmiSessionFactoryImpl implements GnmiSessionFactory {

    /**
     * Creates new {@link GnmiSession} instance.
     *
     * @param configuration if session configuration contains username/password, this will create {@link GnmiSession}
     *                      with this credentials in metadata.
     * @param channel       {@link ManagedChannel}
     * @return {@link GnmiSession}
     */
    @Override
    public GnmiSession createGnmiSession(final SessionConfiguration configuration, final ManagedChannel channel) {
        if (configuration.getUsername() != null && configuration.getPassword() != null) {
            final GnmiCallCredentials gnmiCallCredentials
                    = new GnmiCallCredentials(configuration.getUsername(), configuration.getPassword());
            return createGnmiSession(channel, gnmiCallCredentials);
        }
        return createGnmiSession(channel);
    }

    @VisibleForTesting
    public GnmiSessionImpl createGnmiSession(final ManagedChannel channel) {
        return new GnmiSessionImpl(channel);
    }

    @VisibleForTesting
    public GnmiSessionImpl createGnmiSession(final ManagedChannel channel,
        final GnmiCallCredentials gnmiCallCredentials) {

        return new GnmiSessionImpl(channel, gnmiCallCredentials);
    }
}
