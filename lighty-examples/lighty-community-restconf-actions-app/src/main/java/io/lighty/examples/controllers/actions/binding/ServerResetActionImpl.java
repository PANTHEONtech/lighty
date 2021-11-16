/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.actions.binding;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.Server;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.ServerKey;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.server.Reset;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.server.reset.Input;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.server.reset.Output;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.server.reset.OutputBuilder;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * The example binding implementation of action 'reset' from 'example-data-center' module.
 */
public final class ServerResetActionImpl implements Reset {
    @Override
    public ListenableFuture<RpcResult<Output>> invoke(final KeyedInstanceIdentifier<Server, ServerKey> path,
            final Input input) {
        final var value = new OutputBuilder().setResetFinishedAt(input.getResetAt()).build();
        return RpcResultBuilder.success(value).buildFuture();
    }
}
