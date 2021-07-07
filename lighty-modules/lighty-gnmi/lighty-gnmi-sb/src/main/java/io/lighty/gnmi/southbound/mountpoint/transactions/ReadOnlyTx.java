/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.transactions;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.lighty.gnmi.southbound.mountpoint.ops.GnmiGet;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadOnlyTx implements DOMDataTreeReadTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(ReadOnlyTx.class);
    private final GnmiGet getProvider;
    private final NodeId nodeId;

    public ReadOnlyTx(final GnmiGet getProvider) {
        this.getProvider = getProvider;
        this.nodeId = getProvider.getNodeId();
    }

    @Override
    public void close() {
        LOG.info("Method not implemented!");
    }

    @Override
    public FluentFuture<Optional<NormalizedNode<?, ?>>> read(LogicalDatastoreType store, YangInstanceIdentifier path) {
        switch (store) {
            case OPERATIONAL:
                return FluentFuture.from(getProvider.readOperationalData(path));
            case CONFIGURATION:
                return FluentFuture.from(getProvider.readConfigurationData(path));
            default:
                LOG.warn("[{}] Read {} on unknown datastore type {}", nodeId, path, store);
                throw new UnsupportedOperationException(String.format(
                        "[%s] Can't read data on path %s from datastore %s, datastore type is unknown!", nodeId, path,
                        store));
        }
    }

    @Override
    public FluentFuture<Boolean> exists(LogicalDatastoreType store, YangInstanceIdentifier path) {
        return read(store, path).transform(optResult -> optResult != null && optResult.isPresent(),
                MoreExecutors.directExecutor());
    }

    @Override
    public @NonNull Object getIdentifier() {
        return this;
    }
}
