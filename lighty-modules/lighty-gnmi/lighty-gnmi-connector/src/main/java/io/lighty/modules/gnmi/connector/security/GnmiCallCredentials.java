/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.security;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import java.util.concurrent.Executor;

public class GnmiCallCredentials extends CallCredentials {

    private static final String PASSWORD_KEY = "password";
    private static final String USERNAME_KEY = "username";

    private final String username;
    private final String password;

    public GnmiCallCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor,
            MetadataApplier metadataApplier) {
        appExecutor.execute(() -> {
            var headers = new Metadata();
            headers.put(Metadata.Key.of(PASSWORD_KEY, Metadata.ASCII_STRING_MARSHALLER), password);
            headers.put(Metadata.Key.of(USERNAME_KEY, Metadata.ASCII_STRING_MARSHALLER), username);
            metadataApplier.apply(headers);
        });
    }

    @Override
    public void thisUsesUnstableApi() {
        // Yes, indeed
    }
}
