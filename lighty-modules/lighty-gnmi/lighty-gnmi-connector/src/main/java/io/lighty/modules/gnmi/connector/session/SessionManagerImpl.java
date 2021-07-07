/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.session;

import com.google.common.base.Preconditions;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.lighty.modules.gnmi.connector.configuration.SessionConfiguration;
import io.lighty.modules.gnmi.connector.gnmi.session.impl.GnmiSessionFactory;
import io.lighty.modules.gnmi.connector.security.Security;
import io.lighty.modules.gnmi.connector.session.api.SessionCloseDelegate;
import io.lighty.modules.gnmi.connector.session.api.SessionManager;
import io.lighty.modules.gnmi.connector.session.api.SessionProvider;
import io.lighty.modules.gnmi.connector.session.api.SessionProviderImpl;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionManagerImpl implements SessionCloseDelegate, SessionManager {

    private static final Logger LOG = LoggerFactory.getLogger(SessionManagerImpl.class);
    private static final long CHANNEL_TERMINATION_MILLIS = 5_000;
    private final Security security;
    private final HashMap<SessionConfiguration, ManagedChannel> channelCache;
    private final HashMap<SessionConfiguration, Integer> openSessionsCounter;
    private final GnmiSessionFactory gnmiSessionFactory;

    public SessionManagerImpl(final Security security, final GnmiSessionFactory gnmiSessionFactory) {
        this.security = Objects.requireNonNull(security, "Missing certificates configuration!");
        this.gnmiSessionFactory = gnmiSessionFactory;
        this.channelCache = new HashMap<>();
        this.openSessionsCounter = new HashMap<>();
    }

    @Override
    public synchronized SessionProvider createSession(final SessionConfiguration sessionConfiguration) {
        Preconditions.checkArgument(Objects.nonNull(sessionConfiguration));

        // look for channel in cache
        ManagedChannel channel = channelCache.get(sessionConfiguration);
        if (channel == null) {
            // create new channel if is not cached
            final NettyChannelBuilder builder = NettyChannelBuilder.forAddress(sessionConfiguration.getAddress());
            if (sessionConfiguration.isUsePlainText()) {
                builder.usePlaintext();
            } else {
                try {
                    builder.sslContext(this.security.getSslContext());
                } catch (final SSLException e) {
                    throw new RuntimeException("Failed to create SSL Context!", e);
                }
            }
            channel = builder.build();

            // store new channel to cache
            channelCache.put(sessionConfiguration, channel);
        }

        // increase counter for channel
        final Integer sessionCount = openSessionsCounter.getOrDefault(sessionConfiguration, 0);
        openSessionsCounter.put(sessionConfiguration, sessionCount + 1);

        return new SessionProviderImpl(sessionConfiguration, this, channel,
                gnmiSessionFactory.createGnmiSession(sessionConfiguration, channel));
    }

    @Override
    public synchronized void closeSession(final SessionProvider session) throws InterruptedException {
        // decrease number of sessions per channel
        // if no session is open for channel then close channel
        final Integer sessionCount = openSessionsCounter.get(session.getConfiguration());
        if (sessionCount > 1) {
            openSessionsCounter.put(session.getConfiguration(), sessionCount - 1);
        } else {
            openSessionsCounter.remove(session.getConfiguration());
            final ManagedChannel channel = channelCache.remove(session.getConfiguration());
            final boolean res = channel.shutdown().awaitTermination(CHANNEL_TERMINATION_MILLIS, TimeUnit.MILLISECONDS);
            if (!res) {
                throw new RuntimeException(String.format("Shutdown of session to server %s failed",
                        session.getConfiguration().getAddress()));
            }
        }
    }

    @Override
    public Map<SessionConfiguration, ManagedChannel> getChannelCache() {
        return Collections.unmodifiableMap(channelCache);
    }

    @Override
    public Map<SessionConfiguration, Integer> getOpenSessionsCounter() {
        return Collections.unmodifiableMap(openSessionsCounter);
    }
}
