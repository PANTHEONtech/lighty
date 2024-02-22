/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Set;
import org.opendaylight.mdsal.dom.api.DOMActionResult;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.SimpleDOMActionResult;
import org.opendaylight.netconf.api.messages.NetconfMessage;
import org.opendaylight.netconf.client.mdsal.api.ActionTransformer;
import org.opendaylight.netconf.client.mdsal.api.RemoteDeviceCommunicator;
import org.opendaylight.netconf.client.mdsal.api.RemoteDeviceServices.Actions.Normalized;
import org.opendaylight.yangtools.yang.common.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public final class LightyDOMActionService implements Normalized {

    private final ActionTransformer messageTransformer;
    private final RemoteDeviceCommunicator communicator;

    public LightyDOMActionService(final ActionTransformer messageTransformer,
            final RemoteDeviceCommunicator communicator) {
        this.messageTransformer = messageTransformer;
        this.communicator = communicator;
    }

    @Override
    public ListenableFuture<? extends DOMActionResult> invokeAction(final Absolute type,
            final DOMDataTreeIdentifier path, final ContainerNode input) {
        final NetconfMessage actionRequest = this.messageTransformer.toActionRequest(type, path, input);
        final SettableFuture<DOMActionResult> settableFuture = SettableFuture.create();
        final ListenableFuture<RpcResult<NetconfMessage>> responseFuture = this.communicator.sendRequest(actionRequest,
                type.lastNodeIdentifier());
        Futures.addCallback(responseFuture, new FutureCallback<RpcResult<NetconfMessage>>() {

            @Override
            public void onSuccess(final RpcResult<NetconfMessage> result) {
                Preconditions.checkNotNull(result);
                if (result.getErrors().isEmpty()) {
                    final DOMActionResult actionResult = LightyDOMActionService.this.messageTransformer.toActionResult(
                            type, result.getResult());
                    settableFuture.set(actionResult);
                } else {
                    final SimpleDOMActionResult simpleDOMActionResult = new SimpleDOMActionResult(result.getErrors());
                    settableFuture.set(simpleDOMActionResult);
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                settableFuture.set(new SimpleDOMActionResult(Set.of(new ActionRpcError(cause))));
            }
        }, MoreExecutors.directExecutor());
        return settableFuture;
    }

    private static final class ActionRpcError implements RpcError {

        private final Throwable cause;

        private ActionRpcError(final Throwable cause) {
            this.cause = cause;
        }

        @Override
        public ErrorSeverity getSeverity() {
            return ErrorSeverity.ERROR;
        }

        @Override
        public ErrorTag getTag() {
            return null;
        }

        @Override
        public String getApplicationTag() {
            return null;
        }

        @Override
        public String getMessage() {
            return this.cause.getMessage();
        }

        @Override
        public String getInfo() {
            return null;
        }

        @Override
        public Throwable getCause() {
            return this.cause;
        }

        @Override
        public ErrorType getErrorType() {
            return ErrorType.APPLICATION;
        }
    }
}
