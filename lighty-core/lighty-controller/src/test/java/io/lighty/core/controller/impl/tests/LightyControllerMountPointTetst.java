/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl.tests;

import io.lighty.core.controller.api.LightyController;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointListener;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LightyControllerMountPointTetst extends LightyControllerTestBase {

    @Test
    public void domMountPointServiceTest() throws Exception {
        final LightyController lightyController = getLightyController();
        final DOMMountPointService domMountPointService = lightyController.getServices().getDOMMountPointService();

        // test setup
        final YangInstanceIdentifier testYangIID = TestUtils.createTopologyNodeYIID();
        final int[] listenerMethodsCalled = { 0, 0 };
        domMountPointService.registerProvisionListener(new DOMMountPointListener() {
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

        // 1. register MP in service
        final DOMMountPointService.DOMMountPointBuilder mountPointBuilder = domMountPointService.createMountPoint(
                testYangIID);
        final ObjectRegistration<DOMMountPoint> mountPointRegistration = mountPointBuilder.register();

        // 2. get MP from service service
        final com.google.common.base.Optional<DOMMountPoint> registeredMP = domMountPointService.getMountPoint(
                testYangIID);
        Assert.assertTrue(registeredMP.isPresent());

        // 3. unregister registered MP
        mountPointRegistration.close();

        // 4. check if there isn't registered any MP
        final com.google.common.base.Optional<DOMMountPoint> unregisterredMP = domMountPointService.getMountPoint(
                testYangIID);
        Assert.assertFalse(unregisterredMP.isPresent());

        // check if MP listener methods were called
        Assert.assertEquals(listenerMethodsCalled[0], 1);
        Assert.assertEquals(listenerMethodsCalled[1], 1);
    }

}

