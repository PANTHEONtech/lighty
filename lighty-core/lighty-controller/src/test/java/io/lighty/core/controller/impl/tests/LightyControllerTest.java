/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.tests;

import io.lighty.core.controller.api.LightyController;
import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.Test;

class LightyControllerTest extends LightyControllerTestBase {

    @Test(groups = "boot")
    void controllerSimpleTest() {
        final LightyController lightyController = getLightyController();
        Assert.assertNotNull(lightyController);
        Assert.assertNotNull(lightyController.getServices());
        Assert.assertNotNull(lightyController.getServices().getDiagStatusService());
        Assert.assertEquals(Collections.emptyList(),
                lightyController.getServices().getDiagStatusService().getAllServiceDescriptors());
        Assert.assertNotNull(lightyController.getServices().getActorSystemProvider());
        Assert.assertNotNull(lightyController.getServices().getActorSystemProvider().getActorSystem());
        Assert.assertNotNull(lightyController.getServices().getDOMSchemaService());
        Assert.assertNotNull(lightyController.getServices().getYangTextSourceExtension());
        Assert.assertNotNull(lightyController.getServices().getDOMNotificationRouter());
        Assert.assertNotNull(lightyController.getServices().getConfigDatastore());
        Assert.assertNotNull(lightyController.getServices().getOperationalDatastore());
        Assert.assertNotNull(lightyController.getServices().getYangParserFactory());
        Assert.assertNotNull(lightyController.getServices().getBindingNormalizedNodeSerializer());
        Assert.assertNotNull(lightyController.getServices().getBindingCodecTreeFactory());
        Assert.assertNotNull(lightyController.getServices().getDOMEntityOwnershipService());
        Assert.assertNotNull(lightyController.getServices().getEntityOwnershipService());
        Assert.assertNotNull(lightyController.getServices().getClusterAdminRPCService());
        Assert.assertNotNull(lightyController.getServices().getClusterSingletonServiceProvider());
        Assert.assertNotNull(lightyController.getServices().getDOMMountPointService());
        Assert.assertNotNull(lightyController.getServices().getDOMNotificationPublishService());
        Assert.assertNotNull(lightyController.getServices().getDOMNotificationService());
        Assert.assertNotNull(lightyController.getServices().getClusteredDOMDataBroker());
        Assert.assertNotNull(lightyController.getServices().getDOMRpcService());
        Assert.assertNotNull(lightyController.getServices().getDOMRpcProviderService());
        Assert.assertNotNull(lightyController.getServices().getRpcProviderService());
        Assert.assertNotNull(lightyController.getServices().getBindingMountPointService());
        Assert.assertNotNull(lightyController.getServices().getNotificationService());
        Assert.assertNotNull(lightyController.getServices().getBindingNotificationPublishService());
        Assert.assertNotNull(lightyController.getServices().getBindingDataBroker());
        Assert.assertNotNull(lightyController.getServices().getAdapterContext());
    }
}
