/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl;

import com.google.common.collect.ClassToInstanceMap;
import java.util.Set;
import org.opendaylight.mdsal.dom.api.DOMActionImplementation;
import org.opendaylight.mdsal.dom.api.DOMActionInstance;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMActionProviderServiceExtension;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

/**
 * Delegator for DOMActionProviderService
 *
 */
class LightyDOMActionProviderServiceDelegator implements org.opendaylight.controller.md.sal.dom.api.DOMActionProviderService {

    private final DOMActionProviderService domActionProviderService;

    LightyDOMActionProviderServiceDelegator(final DOMActionProviderService domActionProviderService) {
        this.domActionProviderService = domActionProviderService;
    }

    @Override
    public <T extends DOMActionImplementation> ObjectRegistration<T> registerActionImplementation(
            final T implementation, final Set<DOMActionInstance> instances) {
        return this.domActionProviderService.registerActionImplementation(implementation, instances);
    }

    @Override
    public ClassToInstanceMap<DOMActionProviderServiceExtension> getExtensions() {
        return this.domActionProviderService.getExtensions();
    }
}

