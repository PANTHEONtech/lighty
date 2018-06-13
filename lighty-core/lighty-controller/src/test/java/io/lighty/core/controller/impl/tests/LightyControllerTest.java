/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl.tests;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.infrautils.ready.SystemReadyMonitor;
import org.opendaylight.infrautils.ready.SystemState;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LightyControllerTest extends LightyControllerTestBase {

    @Test(groups = "boot")
    public void controllerSimpleTest() {
        LightyController lightyController = getLightyController();
        Assert.assertNotNull(lightyController);
        Assert.assertNotNull(lightyController.getServices());
        final SystemReadyMonitor monitor = lightyController.getServices().getSystemReadyMonitor();
        Assert.assertNotNull(monitor);
        Assert.assertEquals(monitor.getSystemState(), SystemState.ACTIVE);
        Assert.assertNotNull(lightyController.getServices().getActorSystemProvider());
        Assert.assertNotNull(lightyController.getServices().getActorSystemProvider().getActorSystem());
        Assert.assertNotNull(lightyController.getServices().getDOMSchemaService());
        Assert.assertNotNull(lightyController.getServices().getDOMYangTextSourceProvider());
        Assert.assertNotNull(lightyController.getServices().getSchemaContextProvider());
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
        Assert.assertNotNull(lightyController.getServices().getSchemaService());
        Assert.assertNotNull(lightyController.getServices().getYangTextSourceProvider());
        Assert.assertNotNull(lightyController.getServices().getBindingCodecTreeFactory());
        Assert.assertNotNull(lightyController.getServices().getBindingNormalizedNodeSerializer());
        Assert.assertNotNull(lightyController.getServices().getDOMEntityOwnershipService());
        Assert.assertNotNull(lightyController.getServices().getEntityOwnershipService());
        Assert.assertNotNull(lightyController.getServices().getClusterAdminRPCService());
        Assert.assertNotNull(lightyController.getServices().getClusterSingletonServiceProvider());
        Assert.assertNotNull(lightyController.getServices().getRpcProviderRegistry());
        Assert.assertNotNull(lightyController.getServices().getBindingMountPointService());
        Assert.assertNotNull(lightyController.getServices().getBindingNotificationService());
        Assert.assertNotNull(lightyController.getServices().getBindingNotificationPublishService());
        Assert.assertNotNull(lightyController.getServices().getNotificationProviderService());
        Assert.assertNotNull(lightyController.getServices().getNotificationService());
        Assert.assertNotNull(lightyController.getServices().getBindingDataBroker());
        Assert.assertNotNull(lightyController.getServices().getBindingPingPongDataBroker());
    }

    @Test(dependsOnGroups = "boot")
    public void controllerDataBrokerTest() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        LightyController lightyController = getLightyController();
        DataBroker bindingDataBroker = lightyController.getServices().getBindingDataBroker();
        bindingDataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, TestUtils.TOPOLOGY_IID,
                change -> {
                    if (countDownLatch.getCount() == 2) {
                        // on first time - write
                        Assert.assertEquals(change.getOriginalData().size(), 0);
                        Assert.assertEquals(change.getCreatedData().size(), 1);
                    } else if (countDownLatch.getCount() == 1) {
                        // on second time - delete
                        Assert.assertEquals(change.getOriginalData().size(), 1);
                        Assert.assertEquals(change.getCreatedData().size(), 0);
                    } else {
                        Assert.fail("Too many DataTreeChange events, expected two");
                    }
                    countDownLatch.countDown();
                }, AsyncDataBroker.DataChangeScope.SUBTREE);

        //1. write to TOPOLOGY model
        TestUtils.writeToTopology(bindingDataBroker, TestUtils.TOPOLOGY_IID, TestUtils.TOPOLOGY);

        //2. read from TOPOLOGY model
        TestUtils.readFromTopology(bindingDataBroker, TestUtils.TOPOLOGY_ID, 1);

        //3. delete from TOPOLOGY model
        WriteTransaction deleteTransaction = bindingDataBroker.newWriteOnlyTransaction();
        deleteTransaction.delete(LogicalDatastoreType.OPERATIONAL, TestUtils.TOPOLOGY_IID);
        deleteTransaction.submit().get();

        //4. read from TOPOLOGY model
        TestUtils.readFromTopology(bindingDataBroker, TestUtils.TOPOLOGY_ID, 0);

        // check data change listener
        countDownLatch.await(5, TimeUnit.SECONDS);
    }

    @Test(dependsOnGroups = "boot")
    public void domMountPointServiceTest() throws Exception {
        LightyController lightyController = getLightyController();
        final DOMMountPointService domMountPointService =
                lightyController.getServices().getDOMMountPointService();

        // test setup
        final YangInstanceIdentifier testYangIID = TestUtils.createTopologyNodeYIID();
        final int[] listenerMethodsCalled = {0, 0};
        domMountPointService.registerProvisionListener(new MountProvisionListener() {
            @Override
            public void onMountPointCreated(final YangInstanceIdentifier path) {
                Assert.assertEquals(path, testYangIID);
                listenerMethodsCalled[0]++;
            }

            @Override
            public void onMountPointRemoved(final YangInstanceIdentifier path) {
                Assert.assertEquals(path, testYangIID);
                listenerMethodsCalled[1]++;
            }
        });

        //1. register MP in service
        final DOMMountPointService.DOMMountPointBuilder mountPointBuilder =
                domMountPointService.createMountPoint(testYangIID);
        final ObjectRegistration<DOMMountPoint> mountPointRegistration = mountPointBuilder.register();

        //2. get MP from service service
        final Optional<DOMMountPoint> registeredMP = domMountPointService.getMountPoint(testYangIID);
        Assert.assertTrue(registeredMP.isPresent());

        //3. unregister registered MP
        mountPointRegistration.close();

        //4. check if there isn't registered any MP
        final Optional<DOMMountPoint> unregisterredMP = domMountPointService.getMountPoint(testYangIID);
        Assert.assertFalse(unregisterredMP.isPresent());

        // check if MP listener methods were called
        Assert.assertEquals(listenerMethodsCalled[0], 1);
        Assert.assertEquals(listenerMethodsCalled[1], 1);
    }

    @Test(dependsOnGroups = "boot")
    public void domNotificationServiceTest() throws InterruptedException, ExecutionException {
        LightyController lightyController = getLightyController();

        // setup
        final SchemaPath schemaPath = SchemaPath.ROOT;
        final DOMNotification testNotification = new DOMNotification() {
            @Nonnull
            @Override
            public SchemaPath getType() {
                return schemaPath;
            }

            @Nonnull
            @Override
            public ContainerNode getBody() {
                return ImmutableContainerNodeBuilder.create().build();
            }
        };
        final int[] listenerMethodsCalled = {0};

        //1. register DOMNotificationListener
        DOMNotificationService domNotificationService =
                lightyController.getServices().getDOMNotificationService();
        domNotificationService.registerNotificationListener(notification -> {
            Assert.assertEquals(notification, testNotification);
            listenerMethodsCalled[0]++;
        }, schemaPath);

        //2. put, offer notification
        DOMNotificationPublishService domNotificationPublishService =
                lightyController.getServices().getDOMNotificationPublishService();
        ListenableFuture<?> putListenFuture =
                domNotificationPublishService.putNotification(testNotification);
        putListenFuture.get();
        final ListenableFuture<?> offerListenFuture =
                domNotificationPublishService.offerNotification(testNotification);
        offerListenFuture.get();

        //3. check received notifications
        Assert.assertEquals(listenerMethodsCalled[0], 2);
    }
}
