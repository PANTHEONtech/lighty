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
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.server.ResetInput;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.server.ResetOutput;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.server.ResetOutputBuilder;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * The example binding implementation of action 'reset' from 'example-data-center' module.
 */
public final class ServerResetActionImpl implements Reset {
    @Override
    public ListenableFuture<RpcResult<ResetOutput>> invoke(KeyedInstanceIdentifier<Server, ServerKey> path,
             ResetInput input) {
        var value = new ResetOutputBuilder().setResetFinishedAt(input.getResetAt()).build();
        return RpcResultBuilder.success(value).buildFuture();
    }
}
