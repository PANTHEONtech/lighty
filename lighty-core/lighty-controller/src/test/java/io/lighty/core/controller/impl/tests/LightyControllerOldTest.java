/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl.tests;

import io.lighty.core.controller.api.LightyController;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LightyControllerOldTest extends LightyControllerTestBase {

    @Test
    public void controllerDataBrokerOldTest() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final LightyController lightyController = getLightyController();
        final DataBroker bindingDataBroker = lightyController.getServices().getBindingDataBrokerOld();
        bindingDataBroker.registerDataTreeChangeListener(
                new org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier(
                        org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL,
                        TestUtils.TOPOLOGY_IID),
                new org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener<Topology>() {

                    @Override
                    public void onDataTreeChanged(final Collection<
                            org.opendaylight.controller.md.sal.binding.api.DataTreeModification<Topology>> changes) {
                        for (final org.opendaylight.controller.md.sal.binding.api.DataTreeModification<
                                Topology> change : changes) {
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
                    }
                });

        // 1. write to TOPOLOGY model
        TestUtils.writeToTopology(bindingDataBroker, TestUtils.TOPOLOGY_IID, TestUtils.TOPOLOGY);

        // 2. read from TOPOLOGY model
        TestUtils.readFromTopology(bindingDataBroker, TestUtils.TOPOLOGY_ID, 1);

        // 3. delete from TOPOLOGY model
        final org.opendaylight.controller.md.sal.binding.api.WriteTransaction deleteTransaction = bindingDataBroker
                .newWriteOnlyTransaction();
        deleteTransaction.delete(org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL,
                TestUtils.TOPOLOGY_IID);
        deleteTransaction.submit().get();

        // 4. read from TOPOLOGY model
        TestUtils.readFromTopology(bindingDataBroker, TestUtils.TOPOLOGY_ID, 0);

        // check data change listener
        countDownLatch.await(5, TimeUnit.SECONDS);
    }
}

