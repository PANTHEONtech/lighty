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

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class ModuleInitTest extends TestBase {

    /*
     Are policies written to datastore upon startup?
    */
    @Test
    void routingPoliciesInDatastore() throws InterruptedException, ExecutionException, TimeoutException {
        final DataBroker bindingDataBroker = lightyServices.getBindingDataBroker();
        try (ReadTransaction readTransaction = bindingDataBroker.newReadOnlyTransaction()) {
            final Optional<RoutingPolicy> routingPolicy = readTransaction.read(LogicalDatastoreType.CONFIGURATION,
                    InstanceIdentifier.create(RoutingPolicy.class)).get(WAIT_TIME, TimeUnit.MILLISECONDS);
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
            final Optional<NetworkInstances> networkInstance = readTransaction.read(LogicalDatastoreType.CONFIGURATION,
                    InstanceIdentifier.create(NetworkInstances.class)).get(WAIT_TIME, TimeUnit.MILLISECONDS);
            assertTrue(networkInstance.isPresent());
            assertNotNull(networkInstance.get().nonnullNetworkInstance()
                    .get(new NetworkInstanceKey("global-bgp")));
        }
    }

}
