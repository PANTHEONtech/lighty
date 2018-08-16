/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl.tests;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.mdsal.dom.api.DOMNotificationPublishService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LightyControllerNotificationTest extends LightyControllerTestBase {

    @Test
    public void domNotificationServiceTest() throws InterruptedException, ExecutionException {
        final LightyController lightyController = getLightyController();

        // setup
        final SchemaPath schemaPath = SchemaPath.ROOT;
        final DOMNotification testNotification = new DOMNotification() {
            @Nonnull
            @Override
            public SchemaPath getType() {
                return schemaPath;
            }

            @Nonnull
            @Override
            public ContainerNode getBody() {
                return ImmutableContainerNodeBuilder.create().build();
            }
        };
        final int[] listenerMethodsCalled = { 0 };

        // 1. register DOMNotificationListener
        final DOMNotificationService domNotificationService = lightyController.getServices()
                .getDOMNotificationService();
        domNotificationService.registerNotificationListener(notification -> {
            Assert.assertEquals(notification, testNotification);
            listenerMethodsCalled[0]++;
        }, schemaPath);

        // 2. put, offer notification
        final DOMNotificationPublishService domNotificationPublishService = lightyController.getServices()
                .getDOMNotificationPublishService();
        final ListenableFuture<?> putListenFuture = domNotificationPublishService.putNotification(testNotification);
        putListenFuture.get();
        final ListenableFuture<?> offerListenFuture = domNotificationPublishService.offerNotification(testNotification);
        offerListenFuture.get();

        // 3. check received notifications
        Assert.assertEquals(listenerMethodsCalled[0], 2);
    }
}

