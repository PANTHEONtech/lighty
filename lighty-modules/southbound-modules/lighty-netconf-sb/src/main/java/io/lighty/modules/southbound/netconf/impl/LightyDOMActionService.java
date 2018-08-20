/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.modules.southbound.netconf.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMActionResult;
import org.opendaylight.mdsal.dom.api.DOMActionServiceExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.SimpleDOMActionResult;
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.sal.connect.api.MessageTransformer;
import org.opendaylight.netconf.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class LightyDOMActionService implements DOMActionService {

    private final MessageTransformer<NetconfMessage> messageTransformer;
    private final RemoteDeviceCommunicator<NetconfMessage> communicator;

    public LightyDOMActionService(final MessageTransformer<NetconfMessage> messageTransformer,
            final RemoteDeviceCommunicator<NetconfMessage> communicator, final SchemaContext schemaContext) {
        this.messageTransformer = messageTransformer;
        this.communicator = communicator;
    }

    @Override
    public FluentFuture<? extends DOMActionResult> invokeAction(final SchemaPath type, final DOMDataTreeIdentifier path,
            final ContainerNode input) {
        final NetconfMessage actionRequest = this.messageTransformer.toActionRequest(type, path, input);
        final SettableFuture<DOMActionResult> settableFuture = SettableFuture.create();
        final ListenableFuture<RpcResult<NetconfMessage>> responseFuture = this.communicator.sendRequest(actionRequest,
                type.getLastComponent());
        Futures.addCallback(responseFuture, new FutureCallback<RpcResult<NetconfMessage>>() {

            @Override
            public void onSuccess(final RpcResult<NetconfMessage> result) {
                Preconditions.checkNotNull(result);
                if (result.getErrors().isEmpty()) {
                    final DOMActionResult actionResult = LightyDOMActionService.this.messageTransformer.toActionResult(
                            type, actionRequest);
                    settableFuture.set(actionResult);
                } else {
                    final SimpleDOMActionResult simpleDOMActionResult = new SimpleDOMActionResult(result.getErrors());
                    settableFuture.set(simpleDOMActionResult);
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                new ActionRpcError(t);
            }
        }, Executors.newSingleThreadExecutor());
        return settableFuture;
    }

    @Override
    public ClassToInstanceMap<DOMActionServiceExtension> getExtensions() {
        return MutableClassToInstanceMap.create();
    }

    private final class ActionRpcError implements RpcError {

        private final Throwable t;

        private ActionRpcError(final Throwable t) {
            this.t = t;
        }

        @Override
        public ErrorSeverity getSeverity() {
            return ErrorSeverity.ERROR;
        }

        @Override
        public String getTag() {
            return null;
        }

        @Override
        public String getApplicationTag() {
            return null;
        }

        @Override
        public String getMessage() {
            return this.t.getMessage();
        }

        @Override
        public String getInfo() {
            return null;
        }

        @Override
        public Throwable getCause() {
            return this.t;
        }

        @Override
        public ErrorType getErrorType() {
            return ErrorType.APPLICATION;
        }
    }
}
