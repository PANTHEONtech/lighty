/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.gnmi;

import com.google.common.base.Strings;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.lighty.modules.gnmi.simulatordevice.utils.UsernamePasswordAuth;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationInterceptor implements ServerInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationInterceptor.class);
    private static final Key<String> PASSWORD_KEY = Key.of("password", Metadata.ASCII_STRING_MARSHALLER);
    private static final Key<String> USERNAME_KEY = Key.of("username", Metadata.ASCII_STRING_MARSHALLER);

    private final UsernamePasswordAuth usernamePasswordAuth;

    public AuthenticationInterceptor(@NonNull final UsernamePasswordAuth usernamePasswordAuth) {
        this.usernamePasswordAuth = usernamePasswordAuth;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            final ServerCall<ReqT, RespT> serverCall, final Metadata metadata,
            final ServerCallHandler<ReqT, RespT> serverCallHandler) {

        final String receivedPassword = metadata.get(PASSWORD_KEY);
        final String receivedUsername = metadata.get(USERNAME_KEY);

        if (Strings.isNullOrEmpty(receivedUsername) || Strings.isNullOrEmpty(receivedPassword)) {
            LOG.info("Denied request: No Authentication header present in metadata");
            serverCall.close(Status.UNAUTHENTICATED.withDescription("No authentication header"), metadata);
            return new ServerCall.Listener<>() {};

        } else if (usernamePasswordAuth.authenticate(receivedUsername, receivedPassword)) {
            LOG.info("Authentication Service accepted user [{}] as authenticated", usernamePasswordAuth.getUsername());
            return serverCallHandler.startCall(serverCall, metadata);
        }

        LOG.info("Denied request: Wrong username or password");
        serverCall.close(Status.UNAUTHENTICATED.withDescription("Wrong username or password"), metadata);
        return new ServerCall.Listener<>() {};
    }
}
