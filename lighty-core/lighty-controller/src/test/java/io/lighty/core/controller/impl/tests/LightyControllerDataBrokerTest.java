/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.tests;

import io.lighty.core.controller.api.LightyController;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LightyControllerDataBrokerTest extends LightyControllerTestBase {
    @Test
    public void controllerDataBrokerTest() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final LightyController lightyController = getLightyController();
        final org.opendaylight.mdsal.binding.api.DataBroker bindingDataBroker = lightyController.getServices()
                .getBindingDataBroker();
        bindingDataBroker.registerDataTreeChangeListener(
            DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, TestUtils.TOPOLOGY_IID), changes -> {
                for (final DataTreeModification<Topology> change : changes) {
                    if (countDownLatch.getCount() == 2) {
                        // on first time - write
                        Assert.assertNull(change.getRootNode().getDataBefore());
                        Assert.assertNotNull(change.getRootNode().getDataAfter());
                    } else if (countDownLatch.getCount() == 1) {
                        // on second time - delete
                        Assert.assertNotNull(change.getRootNode().getDataBefore());
                        Assert.assertNull(change.getRootNode().getDataAfter());
                    } else {
                        Assert.fail("Too many DataTreeChange events, expected two");
                    }
                    countDownLatch.countDown();
                }
            });

        // 1. write to TOPOLOGY model
        TestUtils.writeToTopology(bindingDataBroker, TestUtils.TOPOLOGY_IID, TestUtils.TOPOLOGY);

        // 2. read from TOPOLOGY model
        TestUtils.readFromTopology(bindingDataBroker, TestUtils.TOPOLOGY_ID, 1);

        // 3. delete from TOPOLOGY model
        final WriteTransaction deleteTransaction = bindingDataBroker.newWriteOnlyTransaction();
        deleteTransaction.delete(LogicalDatastoreType.OPERATIONAL, TestUtils.TOPOLOGY_IID);
        deleteTransaction.commit().get();

        // 4. read from TOPOLOGY model
        TestUtils.readFromTopology(bindingDataBroker, TestUtils.TOPOLOGY_ID, 0);

        // check data change listener
        countDownLatch.await(10, TimeUnit.SECONDS);
    }
}

