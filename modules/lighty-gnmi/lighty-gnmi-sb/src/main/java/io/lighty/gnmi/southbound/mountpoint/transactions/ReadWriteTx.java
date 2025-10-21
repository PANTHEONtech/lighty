/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.transactions;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class ReadWriteTx implements DOMDataTreeReadWriteTransaction {

    private final DOMDataTreeReadTransaction delegateReadTx;
    private final DOMDataTreeWriteTransaction delegateWriteTx;

    public ReadWriteTx(final DOMDataTreeReadTransaction delegateReadTx,
                       final DOMDataTreeWriteTransaction delegateWriteTx) {
        this.delegateReadTx = delegateReadTx;
        this.delegateWriteTx = delegateWriteTx;
    }

    @Override
    public boolean cancel() {
        return delegateWriteTx.cancel();
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                    final NormalizedNode data) {
        delegateWriteTx.put(store, path, data);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                      final NormalizedNode data) {
        delegateWriteTx.merge(store, path, data);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        delegateWriteTx.delete(store, path);
    }

    @Override
    public @NonNull FluentFuture<?> completionFuture() {
        return delegateWriteTx.commit();
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        return delegateWriteTx.commit();
    }

    @Override
    public FluentFuture<Optional<NormalizedNode>> read(final LogicalDatastoreType store,
                                                             final YangInstanceIdentifier path) {
        return delegateReadTx.read(store, path);
    }

    @Override
    public FluentFuture<Boolean> exists(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        return delegateReadTx.exists(store, path);
    }

    @Override
    public Object getIdentifier() {
        return this;
    }
}
