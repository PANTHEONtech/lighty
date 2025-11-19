/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.guice.service;

import com.google.common.util.concurrent.FluentFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DataStoreServiceImpl implements DataStoreService {

    @Inject
    @Named("BindingDataBroker")
    private DataBroker dataBroker;
    @Inject
    @Named("ClusteredDOMDataBroker")
    private DOMDataBroker domDataBroker;

    public <T extends DataObject> FluentFuture<? extends CommitInfo> writeData(
            final DataObjectIdentifier<T> identifier, final T data) {
        final var writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, identifier, data);
        return writeTransaction.commit();
    }

    @Override
    public <T extends DataObject> FluentFuture<Optional<T>> readFromDataBroker(
            final DataObjectIdentifier<T> identifier) {
        try (var readTransaction = dataBroker.newReadOnlyTransaction()) {
            return readTransaction.read(LogicalDatastoreType.CONFIGURATION, identifier);
        }
    }

    @Override
    public FluentFuture<Optional<NormalizedNode>> readFromDomDataBroker(final YangInstanceIdentifier identifier) {
        try (var domReadTransaction = domDataBroker.newReadOnlyTransaction()) {
            return domReadTransaction.read(LogicalDatastoreType.CONFIGURATION, identifier);
        }
    }
}
