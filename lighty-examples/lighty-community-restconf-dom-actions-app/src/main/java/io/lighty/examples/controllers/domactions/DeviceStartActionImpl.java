/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.domactions;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.mdsal.dom.api.DOMActionImplementation;
import org.opendaylight.mdsal.dom.api.DOMActionResult;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.SimpleDOMActionResult;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.device.start.Input;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.device.start.Output;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

/**
 * The example implementation of action 'start' from 'example-data-center' module.
 */
final class DeviceStartActionImpl implements DOMActionImplementation {
    private static final QName INPUT_LEAF_QNAME = QName.create(Input.QNAME, "start-at").intern();
    private static final QName OUTPUT_LEAF_QNAME = QName.create(Output.QNAME, "start-finished-at").intern();

    @Override
    public ListenableFuture<? extends DOMActionResult> invokeAction(final Absolute type,
            final DOMDataTreeIdentifier path, final ContainerNode input) {
        final var inputValue = input.getChild(NodeIdentifier.create(INPUT_LEAF_QNAME))
                .map(NormalizedNode::getValue).orElseThrow();
        return Futures.immediateFuture(new SimpleDOMActionResult(Builders.containerBuilder()
                .withNodeIdentifier(NodeIdentifier.create(Output.QNAME))
                .withChild(Builders.leafBuilder().withNodeIdentifier(NodeIdentifier.create(OUTPUT_LEAF_QNAME))
                        .withValue(inputValue)
                        .build())
                .build()));
    }
}
