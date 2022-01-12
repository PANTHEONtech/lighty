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
