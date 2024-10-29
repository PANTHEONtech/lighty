/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.tests;

import io.lighty.core.controller.api.LightyController;
import java.util.Optional;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointListener;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LightyControllerMountPointTest extends LightyControllerTestBase {

    @Test
    public void domMountPointServiceTest() throws Exception {
        final LightyController lightyController = getLightyController();
        final DOMMountPointService domMountPointService = lightyController.getServices().getDOMMountPointService();

        // test setup
        final YangInstanceIdentifier testYangIID = TestUtils.createTopologyNodeYIID();
        final int[] listenerMethodsCalled = { 0, 0 };
        domMountPointService.registerProvisionListener(new DOMMountPointListener() {
            @Override
            public void onMountPointCreated(DOMMountPoint mountPoint) {
                Assert.assertEquals(mountPoint.getIdentifier(), testYangIID);
                listenerMethodsCalled[0]++;
            }

            @Override
            public void onMountPointRemoved(final YangInstanceIdentifier path) {
                Assert.assertEquals(path, testYangIID);
                listenerMethodsCalled[1]++;
            }
        });

        // 1. register MP in service
        final DOMMountPointService.DOMMountPointBuilder mountPointBuilder = domMountPointService.createMountPoint(
                testYangIID);
        final ObjectRegistration<DOMMountPoint> mountPointRegistration = mountPointBuilder.register();

        // 2. get MP from service service
        final Optional<DOMMountPoint> registeredMP = domMountPointService.getMountPoint(testYangIID);
        Assert.assertTrue(registeredMP.isPresent());

        // 3. unregister registered MP
        mountPointRegistration.close();

        // 4. check if there isn't registered any MP
        final Optional<DOMMountPoint> unregisterredMP = domMountPointService.getMountPoint(testYangIID);
        Assert.assertFalse(unregisterredMP.isPresent());

        // check if MP listener methods were called
        Assert.assertEquals(listenerMethodsCalled[0], 1);
        Assert.assertEquals(listenerMethodsCalled[1], 1);
    }

}

