/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.broker;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import io.lighty.gnmi.southbound.mountpoint.ops.GnmiGet;
import io.lighty.gnmi.southbound.mountpoint.ops.GnmiSet;
import io.lighty.gnmi.southbound.mountpoint.transactions.ReadOnlyTx;
import io.lighty.gnmi.southbound.mountpoint.transactions.ReadWriteTx;
import io.lighty.gnmi.southbound.mountpoint.transactions.WriteOnlyTx;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.mdsal.dom.spi.PingPongMergingDOMDataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnmiDataBroker implements PingPongMergingDOMDataBroker {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiDataBroker.class);
    private static final String NOT_IMPLEMENTED = "Method not implemented";

    private final GnmiGet gnmiGet;
    private final GnmiSet gnmiSet;

    public GnmiDataBroker(final GnmiGet getProvider, final GnmiSet setProvider) {
        this.gnmiGet = getProvider;
        this.gnmiSet = setProvider;
    }

    @Override
    public @NonNull DOMTransactionChain createTransactionChain(DOMTransactionChainListener listener) {
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
            public DOMDataTreeWriteTransaction newWriteOnlyTransaction() {
                return GnmiDataBroker.this.newWriteOnlyTransaction();
            }

            @Override
            public void close() {
                LOG.warn(NOT_IMPLEMENTED);
            }
        };
    }

    @Override
    public @NonNull ClassToInstanceMap<DOMDataBrokerExtension> getExtensions() {
        return ImmutableClassToInstanceMap.of();
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
