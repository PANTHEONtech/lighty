/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.security;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import java.util.concurrent.Executor;

public class GnmiCallCredentials extends CallCredentials {

    private static final String PASSWORD_KEY = "password";
    private static final String USERNAME_KEY = "username";

    private final String username;
    private final String password;

    public GnmiCallCredentials(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void applyRequestMetadata(final RequestInfo requestInfo, final Executor appExecutor,
                                     final MetadataApplier metadataApplier) {
        appExecutor.execute(() -> {
            final Metadata headers = new Metadata();
            headers.put(Key.of(PASSWORD_KEY, Metadata.ASCII_STRING_MARSHALLER), password);
            headers.put(Key.of(USERNAME_KEY, Metadata.ASCII_STRING_MARSHALLER), username);
            metadataApplier.apply(headers);
        });
    }

    @Override
    public void thisUsesUnstableApi() {
        // Yes, indeed
    }
}
