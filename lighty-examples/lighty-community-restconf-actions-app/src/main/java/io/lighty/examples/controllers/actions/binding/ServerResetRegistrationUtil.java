/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.actions.binding;

import io.lighty.core.controller.api.LightyController;
import org.opendaylight.mdsal.binding.api.ActionSpec;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.Server;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.server.Reset;
import org.opendaylight.yangtools.concepts.Registration;

public final class ServerResetRegistrationUtil {
    private ServerResetRegistrationUtil() {
        // Utility class
    }

    /**
     * The example how to register binding implementation for action 'reset' from 'example-data-center' module
     * using {@code ActionProviderService} obtained from {@code lightyController}.
     *
     * @param lightyController {@code LightyController} instance for easy access to controller services.
     * @return {@code ObjectRegistration} Registration instance of binding action implementation on the controller.
     */
    public static Registration registerBindingAction(final LightyController lightyController) {
        final var actionProviderService = lightyController.getServices().getActionProviderService();
        final var actionSpec = ActionSpec.builder(Server.class).build(Reset.class);
        return actionProviderService.registerImplementation(actionSpec, new ServerResetActionImpl(),
                LogicalDatastoreType.OPERATIONAL);
    }
}
