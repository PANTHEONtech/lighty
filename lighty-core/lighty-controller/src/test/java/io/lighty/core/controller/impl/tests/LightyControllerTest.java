/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl.tests;

import io.lighty.core.controller.api.LightyController;
import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LightyControllerTest extends LightyControllerTestBase {

    @Test(groups = "boot")
    public void controllerSimpleTest() {
        final LightyController lightyController = getLightyController();
        Assert.assertNotNull(lightyController);
        Assert.assertNotNull(lightyController.getServices());
        Assert.assertNotNull(lightyController.getServices().getActorSystemProvider());
        Assert.assertNotNull(lightyController.getServices().getActorSystemProvider().getActorSystem());
        Assert.assertNotNull(lightyController.getServices().getDOMSchemaService());
        Assert.assertNotNull(lightyController.getServices().getDOMYangTextSourceProvider());
        Assert.assertNotNull(lightyController.getServices().getSchemaContextProvider());
        Assert.assertNotNull(lightyController.getServices().getLightyDiagStatusService());
        Assert.assertEquals(Collections.emptyList(),
                lightyController.getServices().getLightyDiagStatusService().getAllServiceDescriptors());
        Assert.assertNotNull(lightyController.getServices().getConfigDatastore());
        Assert.assertNotNull(lightyController.getServices().getOperationalDatastore());
        Assert.assertNotNull(lightyController.getServices().getClusteredDOMDataBroker());
        Assert.assertNotNull(lightyController.getServices().getDOMDataTreeService());
        Assert.assertNotNull(lightyController.getServices().getDOMDataTreeShardingService());
        Assert.assertNotNull(lightyController.getServices().getDistributedShardFactory());
        Assert.assertNotNull(lightyController.getServices().getDOMMountPointService());
        Assert.assertNotNull(lightyController.getServices().getDOMNotificationPublishService());
        Assert.assertNotNull(lightyController.getServices().getDOMNotificationService());
        Assert.assertNotNull(lightyController.getServices().getDOMNotificationSubscriptionListenerRegistry());
        Assert.assertNotNull(lightyController.getServices().getPingPongDataBroker());
        Assert.assertNotNull(lightyController.getServices().getDOMRpcService());
        Assert.assertNotNull(lightyController.getServices().getDOMRpcProviderService());
        Assert.assertNotNull(lightyController.getServices().getBindingCodecTreeFactory());
        Assert.assertNotNull(lightyController.getServices().getBindingNormalizedNodeSerializer());
        Assert.assertNotNull(lightyController.getServices().getDOMEntityOwnershipService());
        Assert.assertNotNull(lightyController.getServices().getEntityOwnershipService());
        Assert.assertNotNull(lightyController.getServices().getClusterAdminRPCService());
        Assert.assertNotNull(lightyController.getServices().getClusterSingletonServiceProvider());
        Assert.assertNotNull(lightyController.getServices().getRpcProviderRegistry());
        Assert.assertNotNull(lightyController.getServices().getBindingMountPointService());
        Assert.assertNotNull(lightyController.getServices().getNotificationService());
        Assert.assertNotNull(lightyController.getServices().getBindingNotificationPublishService());
        Assert.assertNotNull(lightyController.getServices().getControllerNotificationProviderService());
        Assert.assertNotNull(lightyController.getServices().getNotificationService());
        Assert.assertNotNull(lightyController.getServices().getBindingDataBroker());
        Assert.assertNotNull(lightyController.getServices().getBindingPingPongDataBroker());
    }
}
