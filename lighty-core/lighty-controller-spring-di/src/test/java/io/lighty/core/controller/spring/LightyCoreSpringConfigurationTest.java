/*
 * Copyright (c) 2019 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.spring;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModuleRegistryService;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.netty.channel.EventLoopGroup;
import io.netty.util.Timer;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.datastore.admin.ClusterAdminRpcService;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationPublishService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Initializes Spring Boot application and check whether all the lighty.io beans has been correctly autowired.
 * <p/>
 * <p/>
 * Test does not succeed in IDE (Intellij IDEA) if it is started from root directory together with all other tests - it
 * can not create ApplicationContext properly.
 */
@SpringBootTest
public class LightyCoreSpringConfigurationTest extends AbstractJUnit4SpringContextTests {

    private static final Logger LOG = LoggerFactory.getLogger(LightyCoreSpringConfigurationTest.class);

    @Autowired
    LightyController lightyControllerTestProperty;

    @Autowired
    LightyModuleRegistryService lightyModuleRegistryServiceTestProperty;

    @Autowired
    DiagStatusService diagStatusServiceTestProperty;

    @Autowired
    ActorSystemProvider actorSystemProviderTestProperty;

    @Autowired
    DOMSchemaService domSchemaServiceTestProperty;

    @Autowired
    DOMSchemaService.YangTextSourceExtension domYangTextSourceProviderTestProperty;

    @Autowired
    DOMMountPointService domMountPointServiceTestProperty;

    @Autowired
    DOMNotificationPublishService domNotificationPublishServiceTestProperty;

    @Autowired
    DOMNotificationService domNotificationServiceTestProperty;

    @Autowired
    DOMNotificationRouter domNotificationSubscriptionListenerRegistryTestProperty;

    @Autowired
    @Qualifier("ConfigDatastore")
    DistributedDataStoreInterface configDatastoreTestProperty;

    @Autowired
    @Qualifier("OperationalDatastore")
    DistributedDataStoreInterface operationalDatastoreTestProperty;

    @Autowired
    DOMDataBroker clusteredDOMDataBrokerTestProperty;

    @Autowired
    DOMRpcService domRpcServiceTestProperty;

    @Autowired
    DOMRpcProviderService domRpcProviderServiceTestProperty;

    @Autowired
    BindingNormalizedNodeSerializer bindingNormalizedNodeSerializerTestProperty;

    @Autowired
    BindingCodecTreeFactory bindingCodecTreeFactoryTestProperty;

    @Autowired
    DOMEntityOwnershipService domEntityOwnershipServiceTestProperty;

    @Autowired
    EntityOwnershipService entityOwnershipServiceTestProperty;

    @Autowired
    ClusterAdminRpcService clusterAdminRPCServiceTestProperty;

    @Autowired
    ClusterSingletonServiceProvider clusterSingletonServiceProviderTestProperty;

    @Autowired
    RpcProviderService rpcProviderRegistryTestProperty;

    @Autowired
    MountPointService bindingMountPointServiceTestProperty;

    @Autowired
    NotificationService notificationServiceTestProperty;

    @Autowired
    NotificationPublishService bindingNotificationPublishServiceTestProperty;

    @Autowired
    DataBroker bindingDataBrokerTestProperty;

    @Autowired
    @Qualifier("BossGroup")
    EventLoopGroup bossGroupTestProperty;

    @Autowired
    @Qualifier("WorkerGroup")
    EventLoopGroup workerGroupTestProperty;

    @Autowired
    Timer timerTestProperty;

    @Test
    public void testLightyBeansExists() {
        assertNotNull(lightyControllerTestProperty);
        assertNotNull(lightyModuleRegistryServiceTestProperty);
        assertNotNull(diagStatusServiceTestProperty);
        assertNotNull(actorSystemProviderTestProperty);
        assertNotNull(domSchemaServiceTestProperty);
        assertNotNull(domYangTextSourceProviderTestProperty);
        assertNotNull(domMountPointServiceTestProperty);
        assertNotNull(domNotificationPublishServiceTestProperty);
        assertNotNull(domNotificationServiceTestProperty);
        assertNotNull(domNotificationSubscriptionListenerRegistryTestProperty);
        assertNotNull(configDatastoreTestProperty);
        assertNotNull(operationalDatastoreTestProperty);
        assertNotNull(clusteredDOMDataBrokerTestProperty);
        assertNotNull(domRpcServiceTestProperty);
        assertNotNull(domRpcProviderServiceTestProperty);
        assertNotNull(bindingNormalizedNodeSerializerTestProperty);
        assertNotNull(bindingCodecTreeFactoryTestProperty);
        assertNotNull(domEntityOwnershipServiceTestProperty);
        assertNotNull(entityOwnershipServiceTestProperty);
        assertNotNull(clusterAdminRPCServiceTestProperty);
        assertNotNull(clusterSingletonServiceProviderTestProperty);
        assertNotNull(rpcProviderRegistryTestProperty);
        assertNotNull(bindingMountPointServiceTestProperty);
        assertNotNull(notificationServiceTestProperty);
        assertNotNull(bindingNotificationPublishServiceTestProperty);
        assertNotNull(bindingDataBrokerTestProperty);
        assertNotNull(bossGroupTestProperty);
        assertNotNull(workerGroupTestProperty);
        assertNotNull(timerTestProperty);
    }

    @TestConfiguration
    static class TestConfig extends LightyCoreSpringConfiguration {

        @Override
        public LightyController initLightyController() throws LightyLaunchException, InterruptedException {
            try {
                LOG.info("Building LightyController Core");
                final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
                final LightyController lightyController = lightyControllerBuilder
                        .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(Collections.emptySet()))
                        .build();
                LOG.info("Starting LightyController");
                final ListenableFuture<Boolean> started = lightyController.start();
                started.get();
                LOG.info("LightyController Core started");

                return lightyController;
            } catch (ConfigurationException | ExecutionException e) {
                throw new LightyLaunchException("Could not init LightyController", e);
            }
        }

        @Override
        @SuppressWarnings("checkstyle:illegalCatch")
        public void shutdownLightyController(LightyController lightyController) throws LightyLaunchException {
            try {
                LOG.info("Shutting down LightyController ...");
                lightyController.shutdown();
            } catch (Exception e) {
                throw new LightyLaunchException("Could not shutdown LightyController", e);
            }
        }
    }
}
