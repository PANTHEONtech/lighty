/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.bgp.deployer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.core.controller.api.LightyController;
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
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

class ModuleInitTest {
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
            final DataObjectIdentifier<RoutingPolicy> routingPolicyID = DataObjectIdentifier.builderOfInherited(
                    OpenconfigRoutingPolicyData.class, RoutingPolicy.class).build();
            final Optional<RoutingPolicy> routingPolicy = readTransaction.read(LogicalDatastoreType.CONFIGURATION,
                    routingPolicyID).get(WAIT_TIME, TimeUnit.MILLISECONDS);
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
            final DataObjectIdentifier<NetworkInstances> networkInstancesID = DataObjectIdentifier.builderOfInherited(
                    OpenconfigNetworkInstanceData.class, NetworkInstances.class).build();
            final Optional<NetworkInstances> networkInstance = readTransaction.read(LogicalDatastoreType.CONFIGURATION,
                    networkInstancesID).get(WAIT_TIME, TimeUnit.MILLISECONDS);
            assertTrue(networkInstance.isPresent());
            assertNotNull(networkInstance.get().nonnullNetworkInstance()
                    .get(new NetworkInstanceKey("global-bgp")));
        }
    }

    @AfterAll
    static void shutdown() {
        final boolean bgpShutdown = bgpModule.shutdown(WAIT_TIME, TimeUnit.MILLISECONDS);
        final boolean controllerShutdown = controller.shutdown(WAIT_TIME, TimeUnit.MILLISECONDS);
        assertTrue(bgpShutdown);
        assertTrue(controllerShutdown);
    }
}
