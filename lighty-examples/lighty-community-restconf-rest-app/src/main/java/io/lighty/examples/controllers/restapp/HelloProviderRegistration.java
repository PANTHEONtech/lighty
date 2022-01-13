/*
 * Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.restapp;

import io.lighty.core.controller.api.LightyController;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev210321.HelloService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

public class HelloProviderRegistration {
    private HelloProviderRegistration() {
        // Utility class
    }

    public static ObjectRegistration<HelloService> registerRpc(final LightyController lightyController) {
        final var actionProviderService = lightyController.getServices().getRpcProviderService();
        return actionProviderService.registerRpcImplementation(HelloService.class, new HelloProvider());
    }
}
