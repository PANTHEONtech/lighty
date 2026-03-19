/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.lighty.core.controller.api.LightyController;
import java.util.HashSet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class LightyControllerTest extends LightyControllerTestBase {

    @Test
    @Tag("boot")
    void controllerSimpleTest() {
        final LightyController lightyController = getLightyController();

        assertNotNull(lightyController);
        assertNotNull(lightyController.getServices());
        assertNotNull(lightyController.getServices().getDiagStatusService());

        assertEquals(new HashSet<>(),
                lightyController.getServices().getDiagStatusService().getAllServiceDescriptors());

        assertNotNull(lightyController.getServices().getActorSystemProvider());
        assertNotNull(lightyController.getServices().getActorSystemProvider().getActorSystem());
        assertNotNull(lightyController.getServices().getDOMSchemaService());
        assertNotNull(lightyController.getServices().getYangTextSourceExtension());
        assertNotNull(lightyController.getServices().getDOMNotificationRouter());
        assertNotNull(lightyController.getServices().getConfigDatastore());
        assertNotNull(lightyController.getServices().getOperationalDatastore());
        assertNotNull(lightyController.getServices().getYangParserFactory());
        assertNotNull(lightyController.getServices().getBindingNormalizedNodeSerializer());
        assertNotNull(lightyController.getServices().getBindingCodecTreeFactory());
        assertNotNull(lightyController.getServices().getDOMEntityOwnershipService());
        assertNotNull(lightyController.getServices().getEntityOwnershipService());
        assertNotNull(lightyController.getServices().getClusterAdminRPCService());
        assertNotNull(lightyController.getServices().getClusterSingletonServiceProvider());
        assertNotNull(lightyController.getServices().getDOMMountPointService());
        assertNotNull(lightyController.getServices().getDOMNotificationPublishService());
        assertNotNull(lightyController.getServices().getDOMNotificationService());
        assertNotNull(lightyController.getServices().getClusteredDOMDataBroker());
        assertNotNull(lightyController.getServices().getDOMRpcService());
        assertNotNull(lightyController.getServices().getDOMRpcProviderService());
        assertNotNull(lightyController.getServices().getRpcProviderService());
        assertNotNull(lightyController.getServices().getBindingMountPointService());
        assertNotNull(lightyController.getServices().getNotificationService());
        assertNotNull(lightyController.getServices().getBindingNotificationPublishService());
        assertNotNull(lightyController.getServices().getBindingDataBroker());
        assertNotNull(lightyController.getServices().getAdapterContext());
    }
}