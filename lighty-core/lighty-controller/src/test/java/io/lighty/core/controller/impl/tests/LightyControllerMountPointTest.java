/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.tests;

import io.lighty.core.controller.api.LightyController;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointListener;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

class LightyControllerMountPointTest extends LightyControllerTestBase {

    @Test
    void domMountPointServiceTest() throws Exception {
        final LightyController lightyController = getLightyController();
        final DOMMountPointService domMountPointService = lightyController.getServices().getDOMMountPointService();

        // test setup
        final YangInstanceIdentifier testYangIID = TestUtils.createTopologyNodeYIID();
        final int[] listenerMethodsCalled = { 0, 0 };
        domMountPointService.registerProvisionListener(new DOMMountPointListener() {
            @Override
            public void onMountPointCreated(DOMMountPoint mountPoint) {
                Assertions.assertEquals(mountPoint.getIdentifier(), testYangIID);
                listenerMethodsCalled[0]++;
            }

            @Override
            public void onMountPointRemoved(final YangInstanceIdentifier path) {
                Assertions.assertEquals(path, testYangIID);
                listenerMethodsCalled[1]++;
            }
        });

        // 1. register MP in service
        final DOMMountPointService.DOMMountPointBuilder mountPointBuilder = domMountPointService.createMountPoint(
                testYangIID);
        final ObjectRegistration<DOMMountPoint> mountPointRegistration = mountPointBuilder.register();

        // 2. get MP from service service
        final Optional<DOMMountPoint> registeredMP = domMountPointService.getMountPoint(testYangIID);
        Assertions.assertTrue(registeredMP.isPresent());

        // 3. unregister registered MP
        mountPointRegistration.close();

        // 4. check if there isn't registered any MP
        final Optional<DOMMountPoint> unregisterredMP = domMountPointService.getMountPoint(testYangIID);
        Assertions.assertFalse(unregisterredMP.isPresent());

        // check if MP listener methods were called
        Assertions.assertEquals(listenerMethodsCalled[0], 1);
        Assertions.assertEquals(listenerMethodsCalled[1], 1);
    }

}

