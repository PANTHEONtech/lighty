/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.restconf.community.impl.tests;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.security.Principal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.restconf.api.FormattableBody;
import org.opendaylight.restconf.api.QueryParameters;
import org.opendaylight.restconf.api.query.PrettyPrintParam;
import org.opendaylight.restconf.server.api.AbstractServerRequest;
import org.opendaylight.restconf.server.api.ServerException;
import org.opendaylight.restconf.server.api.TransportSession;
import org.opendaylight.restconf.server.api.YangErrorsBody;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AbstractServerRequest} which eventually completes.
 */
@NonNullByDefault
public final class CompletingServerRequest<T> extends AbstractServerRequest<T> {
    private static final Logger LOG = LoggerFactory.getLogger(CompletingServerRequest.class);

    private final CompletableFuture<T> future = new CompletableFuture<>();

    public CompletingServerRequest() {
        this(PrettyPrintParam.TRUE);
    }

    public CompletingServerRequest(final PrettyPrintParam prettyPrint) {
        this(QueryParameters.of(), prettyPrint);
    }

    public CompletingServerRequest(final QueryParameters queryParameters) {
        this(queryParameters, PrettyPrintParam.TRUE);
    }

    public CompletingServerRequest(final QueryParameters queryParameters, final PrettyPrintParam defaultPrettyPrint) {
        super(queryParameters, defaultPrettyPrint);
    }

    public T getResult() throws ServerException, InterruptedException, TimeoutException {
        return getResult(5, TimeUnit.SECONDS);
    }

    @SuppressWarnings("checkstyle:avoidHidingCauseException")
    public T getResult(final int timeout, final TimeUnit unit)
        throws ServerException, InterruptedException, TimeoutException {
        try {
            return future.get(timeout, unit);
        } catch (ExecutionException e) {
            LOG.debug("Request failed", e);
            final var cause = e.getCause();
            Throwables.throwIfInstanceOf(cause, ServerException.class);
            Throwables.throwIfUnchecked(cause);
            throw new UncheckedExecutionException(cause);
        }
    }

    @Override
    public @Nullable Principal principal() {
        return null;
    }

    @Override
    public @Nullable TransportSession session() {
        return null;
    }

    @Override
    protected void onSuccess(final T result) {
        future.complete(result);
    }

    @Override
    protected void onFailure(final YangErrorsBody errors) {
        future.completeExceptionally(new ServerException(errors.errors(), null, "reconstructed for testing"));
    }

    @Override
    public void completeWith(final ErrorTag errorTag, final FormattableBody body) {
        throw new UnsupportedOperationException();
    }
}