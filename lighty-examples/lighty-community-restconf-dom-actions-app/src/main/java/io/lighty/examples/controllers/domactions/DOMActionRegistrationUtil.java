/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.domactions;

import io.lighty.core.controller.api.LightyController;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMActionImplementation;
import org.opendaylight.mdsal.dom.api.DOMActionInstance;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.Device;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.device.Start;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;

final class DOMActionRegistrationUtil {
    private DOMActionRegistrationUtil() {
        // hidden on purpose
    }

    /**
     * The example how to register {@code actionImplementation} which operates on data at {@code actionParentYII} path
     * using {@DOMActionProviderService} obtained from {@code lightyController}.
     *
     * @param lightyController {@code LightyController} instance for easy access to controller services.
     * @param actionImplementation The implementation of DOM action to be registered.
     * @param actionParentYII The data path on which action operates.
     * @return {@code ObjectRegistration} Registration instance of the DOM action implementation on the controller.
     */
    static ObjectRegistration<DOMActionImplementation> registerDOMAction(final LightyController lightyController,
            final DOMActionImplementation actionImplementation, final YangInstanceIdentifier actionParentYII) {
        final DOMActionProviderService domActionProviderService = lightyController.getServices()
                .getDOMActionProviderService();
        final DOMDataTreeIdentifier domDataTreeIdentifier = new DOMDataTreeIdentifier(
                LogicalDatastoreType.OPERATIONAL, actionParentYII);
        return domActionProviderService.registerActionImplementation(actionImplementation, DOMActionInstance.of(
                SchemaNodeIdentifier.Absolute.of(Device.QNAME, Start.QNAME), domDataTreeIdentifier));
    }
}
