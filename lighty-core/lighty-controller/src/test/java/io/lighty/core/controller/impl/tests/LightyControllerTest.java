/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
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
        Assert.assertNotNull(lightyController.getServices().getDiagStatusService());
        Assert.assertEquals(Collections.emptyList(),
                lightyController.getServices().getDiagStatusService().getAllServiceDescriptors());
        Assert.assertNotNull(lightyController.getServices().getActorSystemProvider());
        Assert.assertNotNull(lightyController.getServices().getActorSystemProvider().getActorSystem());
        Assert.assertNotNull(lightyController.getServices().getSchemaContextProvider());
        Assert.assertNotNull(lightyController.getServices().getDOMSchemaService());
        Assert.assertNotNull(lightyController.getServices().getDOMYangTextSourceProvider());
        Assert.assertNotNull(lightyController.getServices().getDOMNotificationSubscriptionListenerRegistry());
        Assert.assertNotNull(lightyController.getServices().getConfigDatastore());
        Assert.assertNotNull(lightyController.getServices().getOperationalDatastore());
        Assert.assertNotNull(lightyController.getServices().getDOMDataTreeShardingService());
        Assert.assertNotNull(lightyController.getServices().getDOMDataTreeService());
        Assert.assertNotNull(lightyController.getServices().getDOMDataTreeService());
        Assert.assertNotNull(lightyController.getServices().getDistributedShardFactory());
        Assert.assertNotNull(lightyController.getServices().getDistributedShardFactory());
        Assert.assertNotNull(lightyController.getServices().getBindingNormalizedNodeSerializer());
        Assert.assertNotNull(lightyController.getServices().getBindingCodecTreeFactory());
        Assert.assertNotNull(lightyController.getServices().getDOMEntityOwnershipService());
        Assert.assertNotNull(lightyController.getServices().getEntityOwnershipService());
        Assert.assertNotNull(lightyController.getServices().getClusterAdminRPCService());
        Assert.assertNotNull(lightyController.getServices().getClusterSingletonServiceProvider());
        Assert.assertNotNull(lightyController.getServices().getEventExecutor());
        Assert.assertNotNull(lightyController.getServices().getBossGroup());
        Assert.assertNotNull(lightyController.getServices().getWorkerGroup());
        Assert.assertNotNull(lightyController.getServices().getThreadPool());
        Assert.assertNotNull(lightyController.getServices().getScheduledThreaPool());
        Assert.assertNotNull(lightyController.getServices().getTimer());
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
        // Test deprecated services
        Assert.assertNotNull(lightyController.getServices().getControllerNotificationProviderService());
        Assert.assertNotNull(lightyController.getServices().getControllerDOMNotificationSubscriptionListenerRegistry());
        Assert.assertNotNull(lightyController.getServices().getControllerDOMMountPointService());
        Assert.assertNotNull(lightyController.getServices().getControllerDOMNotificationPublishService());
        Assert.assertNotNull(lightyController.getServices().getControllerDOMNotificationService());
        Assert.assertNotNull(lightyController.getServices().getControllerClusteredDOMDataBroker());
        Assert.assertNotNull(lightyController.getServices().getControllerPingPongDataBroker());
        Assert.assertNotNull(lightyController.getServices().getControllerDOMRpcService());
        Assert.assertNotNull(lightyController.getServices().getControllerDOMRpcProviderService());
        Assert.assertNotNull(lightyController.getServices().getControllerRpcProviderRegistry());
        Assert.assertNotNull(lightyController.getServices().getControllerBindingMountPointService());
        Assert.assertNotNull(lightyController.getServices().getControllerBindingNotificationService());
        Assert.assertNotNull(lightyController.getServices().getControllerBindingDataBroker());
        Assert.assertNotNull(lightyController.getServices().getControllerBindingPingPongDataBroker());
        Assert.assertNotNull(lightyController.getServices().getControllerBindingNotificationPublishService());
    }
}
