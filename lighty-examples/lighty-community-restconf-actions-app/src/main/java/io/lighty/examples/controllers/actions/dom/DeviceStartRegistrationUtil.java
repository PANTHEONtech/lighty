/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.actions.dom;

import io.lighty.core.controller.api.LightyController;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMActionInstance;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.Device;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.device.Start;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public final class DeviceStartRegistrationUtil {
    private DeviceStartRegistrationUtil() {
        // Utility class
    }

    /**
     * The example how to register the DOM implementation of action 'start' from 'example-data-center' module
     * using {@code DOMActionProviderService} obtained from {@code lightyController}.
     *
     * @param lightyController {@code LightyController} instance for easy access to controller services.
     * @return {@code ObjectRegistration} Registration instance of the DOM action implementation on the controller.
     */
    public static Registration registerDOMAction(
            final LightyController lightyController) {
        final var domActionProviderService = lightyController.getServices().getDOMActionProviderService();
        final var domDataTreeIdentifier = new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL,
                YangInstanceIdentifier.of(Device.QNAME));
        return domActionProviderService.registerActionImplementation(new DeviceStartActionImpl(),
                DOMActionInstance.of(Absolute.of(Device.QNAME, Start.QNAME), domDataTreeIdentifier));
    }
}
