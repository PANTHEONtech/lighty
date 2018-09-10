/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl;

import java.util.Set;
import org.opendaylight.mdsal.binding.api.ActionService;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.yangtools.yang.binding.Action;
import org.opendaylight.yangtools.yang.binding.DataObject;

class LightyActionServiceAdapter implements org.opendaylight.controller.md.sal.binding.api.ActionService {

    private final ActionService actionService;

    LightyActionServiceAdapter(final ActionService actionService) {
        this.actionService = actionService;
    }

    @Override
    public <O extends DataObject, T extends Action<?, ?, ?>> T getActionHandle(final Class<T> actionInterface, final Set<
            DataTreeIdentifier<O>> validNodes) {
        return this.actionService.getActionHandle(actionInterface, validNodes);
    }
}

