/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.broker;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.gnmi.southbound.mountpoint.ops.GnmiGet;
import io.lighty.gnmi.southbound.mountpoint.ops.GnmiSet;
import io.lighty.gnmi.southbound.mountpoint.transactions.ReadOnlyTx;
import io.lighty.gnmi.southbound.mountpoint.transactions.ReadWriteTx;
import io.lighty.gnmi.southbound.mountpoint.transactions.WriteOnlyTx;
import java.util.concurrent.Executor;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.spi.PingPongMergingDOMDataBroker;
import org.opendaylight.yangtools.yang.common.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnmiDataBroker implements PingPongMergingDOMDataBroker {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiDataBroker.class);

    private final GnmiGet gnmiGet;
    private final GnmiSet gnmiSet;

    public GnmiDataBroker(final GnmiGet getProvider, final GnmiSet setProvider) {
        this.gnmiGet = getProvider;
        this.gnmiSet = setProvider;
    }

    @Override
    public @NonNull DOMTransactionChain createTransactionChain() {
        return new DOMTransactionChain() {
            @Override
            public DOMDataTreeReadTransaction newReadOnlyTransaction() {
                return GnmiDataBroker.this.newReadOnlyTransaction();
            }

            @Override
            public DOMDataTreeReadWriteTransaction newReadWriteTransaction() {
                return GnmiDataBroker.this.newReadWriteTransaction();
            }

            @Override
            public void addCallback(FutureCallback<Empty> callback) {
                DOMTransactionChain.super.addCallback(callback);
            }

            @Override
            public void addCallback(FutureCallback<Empty> callback, Executor executor) {
                DOMTransactionChain.super.addCallback(callback, executor);
            }

            @Override
            public @NonNull ListenableFuture<Empty> future() {
                return createMergingTransactionChain().future();
            }

            @Override
            public DOMDataTreeWriteTransaction newWriteOnlyTransaction() {
                return GnmiDataBroker.this.newWriteOnlyTransaction();
            }

            @Override
            public void close() {
                LOG.debug("Closing {} resources", this.getClass().getSimpleName());
            }
        };
    }

    @Override
    public DOMDataTreeReadTransaction newReadOnlyTransaction() {
        return new ReadOnlyTx(gnmiGet);
    }

    @Override
    public DOMDataTreeWriteTransaction newWriteOnlyTransaction() {
        return new WriteOnlyTx(gnmiSet);
    }

    @Override
    public DOMDataTreeReadWriteTransaction newReadWriteTransaction() {
        return new ReadWriteTx(newReadOnlyTransaction(), newWriteOnlyTransaction());
    }
}
