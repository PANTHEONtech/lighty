/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.transactions;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWriteTx implements DOMDataTreeWriteTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractWriteTx.class);
    protected List<ImmutablePair<YangInstanceIdentifier, NormalizedNode<?, ?>>> putList;
    protected List<ImmutablePair<YangInstanceIdentifier, NormalizedNode<?, ?>>> mergeList;
    protected List<YangInstanceIdentifier> deleteList;
    protected NodeId nodeId;
    private boolean finished;

    protected AbstractWriteTx(final NodeId nodeId) {
        this.nodeId = nodeId;
        putList = new ArrayList<>();
        mergeList = new ArrayList<>();
        deleteList = new ArrayList<>();
        finished = false;
    }

    @Override
    public final synchronized FluentFuture<CommitInfo> commit() {
        checkNotFinished();
        finished = true;
        return performCommit();
    }

    protected synchronized boolean isFinished() {
        return finished;
    }

    protected abstract FluentFuture<CommitInfo> performCommit();

    @Override
    public synchronized boolean cancel() {
        if (isFinished()) {
            return  false;
        }
        finished = true;
        return true;
    }

    @Override
    public synchronized void put(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        checkEditableDatastore(store);
        if (containsOnlyNonVisibleData(path, data)) {
            LOG.debug("Ignoring put for {} and data {}. Resulting data structure is empty.", path, data);
        } else {
            putList.add(ImmutablePair.of(path, data));
        }
    }

    @Override
    public synchronized void merge(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        checkEditableDatastore(store);
        if (containsOnlyNonVisibleData(path, data)) {
            LOG.debug("Ignoring merge for {} and data {}. Resulting data structure is empty.", path, data);
        } else {
            mergeList.add(ImmutablePair.of(path, data));
        }
    }

    @Override
    public synchronized void delete(LogicalDatastoreType store, YangInstanceIdentifier path) {
        checkEditableDatastore(store);
        deleteList.add(path);

    }

    private void checkNotFinished() {
        Preconditions.checkState(!isFinished(), "[%s] Transaction %s already finished",
                nodeId.getValue(), getIdentifier());
    }

    private void checkEditableDatastore(final LogicalDatastoreType store) {
        checkNotFinished();
        Preconditions.checkArgument(store == LogicalDatastoreType.CONFIGURATION,
                "Datastore %s is not editable!", store);
    }

    private boolean containsOnlyNonVisibleData(final YangInstanceIdentifier path,
                                               final NormalizedNode<?, ?> data) {
        return path.getPathArguments().size() == 1 && data instanceof MixinNode;
    }

    @Override
    public @NonNull Object getIdentifier() {
        return this;
    }
}
