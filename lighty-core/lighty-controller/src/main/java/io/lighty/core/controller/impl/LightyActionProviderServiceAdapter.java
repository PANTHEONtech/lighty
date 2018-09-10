/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl;

import java.util.Set;
import org.opendaylight.mdsal.binding.api.ActionProviderService;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.Action;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class LightyActionProviderServiceAdapter implements
        org.opendaylight.controller.md.sal.binding.api.ActionProviderService {

    private final ActionProviderService delegator;

    LightyActionProviderServiceAdapter(final ActionProviderService actionProviderService) {
        this.delegator = actionProviderService;
    }

    @Override
    public <O extends DataObject, P extends InstanceIdentifier<O>, T extends Action<P, ?, ?>, S extends T>
    ObjectRegistration<S> registerImplementation(final Class<T> actionInterface, final S implementation,
            final LogicalDatastoreType datastore, final Set<DataTreeIdentifier<O>> validNodes) {
        return this.delegator.registerImplementation(actionInterface, implementation, datastore, validNodes);
    }
}

