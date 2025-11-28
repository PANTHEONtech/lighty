/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.guice.service;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface DataStoreService {

    <T extends DataObject> FluentFuture<? extends CommitInfo> writeData(DataObjectIdentifier<T> identifier, T data);

    <T extends DataObject> FluentFuture<Optional<T>> readFromDataBroker(DataObjectIdentifier<T> identifier);

    FluentFuture<Optional<NormalizedNode>> readFromDomDataBroker(YangInstanceIdentifier identifier);

}
