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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.Registration;

class LightyControllerDataBrokerTest extends LightyControllerTestBase {
    private Registration registration;

    @AfterEach
    void afterMethod() {
        if (registration != null) {
            registration.close();
        }
    }

    @Test
    void controllerDataBrokerTest() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final LightyController lightyController = getLightyController();
        final org.opendaylight.mdsal.binding.api.DataBroker bindingDataBroker = lightyController.getServices()
                .getBindingDataBroker();
        registration = bindingDataBroker.registerTreeChangeListener(LogicalDatastoreType.OPERATIONAL,
            TestUtils.TOPOLOGY_ID, changes -> {
                for (final var change : changes) {
                    final var rootNode = change.getRootNode();
                    if (countDownLatch.getCount() == 2) {
                        // on first time - write
                        Assertions.assertNull(rootNode.dataBefore());
                        Assertions.assertTrue(rootNode instanceof DataObjectModification.WithDataAfter<Topology>);
                        Assertions.assertNotNull(((
                            DataObjectModification.WithDataAfter<Topology>) rootNode).dataAfter());
                    } else if (countDownLatch.getCount() == 1) {
                        // on second time - delete
                        Assertions.assertNotNull(rootNode.dataBefore());
                        Assertions.assertFalse(rootNode instanceof DataObjectModification.WithDataAfter<Topology>);
                    } else {
                        Assertions.fail("Too many DataTreeChange events, expected two");
                    }
                    countDownLatch.countDown();
                }
            });

        // 1. write to TOPOLOGY model
        TestUtils.writeToTopology(bindingDataBroker, TestUtils.TOPOLOGY_ID, TestUtils.TOPOLOGY);

        // 2. read from TOPOLOGY model
        TestUtils.readFromTopology(bindingDataBroker, TestUtils.TOPOLOGY_NAME, 1);

        // 3. delete from TOPOLOGY model
        final WriteTransaction deleteTransaction = bindingDataBroker.newWriteOnlyTransaction();
        deleteTransaction.delete(LogicalDatastoreType.OPERATIONAL, TestUtils.TOPOLOGY_ID);
        deleteTransaction.commit().get();

        // 4. read from TOPOLOGY model
        TestUtils.readFromTopology(bindingDataBroker, TestUtils.TOPOLOGY_NAME, 0);

        // check data change listener
        countDownLatch.await(10, TimeUnit.SECONDS);
    }
}

