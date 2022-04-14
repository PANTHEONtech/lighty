/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.bgp.deployer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.bgp.config.BgpConfigUtils;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.OpenconfigNetworkInstanceData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.OpenconfigRoutingPolicyData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModuleInitTest {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleInitTest.class);
    private static final long WAIT_TIME = 20_000;

    private static BgpModule bgpModule;
    private static LightyServices lightyServices;
    private static LightyController controller;

    @BeforeAll
    static void setup() throws ConfigurationException, InterruptedException, ExecutionException, TimeoutException {
        final ControllerConfiguration controllerConfiguration
                = ControllerConfigUtils.getDefaultSingleNodeConfiguration(BgpConfigUtils.ALL_BGP_MODELS);
        controller = new LightyControllerBuilder().from(controllerConfiguration).build();
        assertTrue(controller.start().get(WAIT_TIME, TimeUnit.MILLISECONDS));
        lightyServices = controller.getServices();
        bgpModule = new BgpModule(lightyServices);
        assertTrue(bgpModule.start().get(WAIT_TIME, TimeUnit.MILLISECONDS));
    }

    /*
     Are policies written to datastore upon startup?
    */
    @Test
    void routingPoliciesInDatastore() throws InterruptedException, ExecutionException, TimeoutException {
        final DataBroker bindingDataBroker = lightyServices.getBindingDataBroker();
        try (ReadTransaction readTransaction = bindingDataBroker.newReadOnlyTransaction()) {
            final InstanceIdentifier<RoutingPolicy> routingPolicyIID = InstanceIdentifier.builderOfInherited(
                    OpenconfigRoutingPolicyData.class, RoutingPolicy.class).build();
            final Optional<RoutingPolicy> routingPolicy = readTransaction.read(LogicalDatastoreType.CONFIGURATION,
                    routingPolicyIID).get(WAIT_TIME, TimeUnit.MILLISECONDS);
            assertTrue(routingPolicy.isPresent());
            // We are importing 2 routing policies (default-import + default-export)
            assertEquals(2, routingPolicy.get().getPolicyDefinitions().nonnullPolicyDefinition().size());
        }
    }

    /*
     Is network instance global-bgp written to datastore upon startup?
    */
    @Test
    void networkInstanceInDatastore() throws InterruptedException, ExecutionException, TimeoutException {
        final DataBroker bindingDataBroker = lightyServices.getBindingDataBroker();
        try (ReadTransaction readTransaction = bindingDataBroker.newReadOnlyTransaction()) {
            final InstanceIdentifier<NetworkInstances> networkInstancesIID = InstanceIdentifier.builderOfInherited(
                    OpenconfigNetworkInstanceData.class, NetworkInstances.class).build();
            final Optional<NetworkInstances> networkInstance = readTransaction.read(LogicalDatastoreType.CONFIGURATION,
                    networkInstancesIID).get(WAIT_TIME, TimeUnit.MILLISECONDS);
            assertTrue(networkInstance.isPresent());
            assertNotNull(networkInstance.get().nonnullNetworkInstance()
                    .get(new NetworkInstanceKey("global-bgp")));
        }
    }

    @AfterAll
    static void shutdown() {
        final boolean bgpShutdown = shutdownModule(bgpModule);
        final boolean controllerShutdown = shutdownModule(controller);
        assertTrue(bgpShutdown);
        assertTrue(controllerShutdown);
    }

    @SuppressWarnings({"checkstyle:illegalCatch"})
    private static boolean shutdownModule(final LightyModule module) {
        try {
            return module.shutdown().get(WAIT_TIME, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOG.error("Shutdown of {} module failed", module.getClass().getName(), e);
            return false;
        }
    }

}
