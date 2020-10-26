/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.tests;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.mdsal.dom.api.DOMNotificationListener;
import org.opendaylight.mdsal.dom.api.DOMNotificationPublishService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LightyControllerNotificationTest extends LightyControllerTestBase {

    @Test
    public void domNotificationServiceTest() throws InterruptedException, ExecutionException {
        final LightyController lightyController = getLightyController();

        // setup
//        final SchemaPath schemaPath = SchemaPath.ROOT;
        final SchemaPath schemaPath = SchemaPath.create(true, NetworkTopology.QNAME);
        final DOMNotification testNotification = new DOMNotification() {
            @Override
            public SchemaNodeIdentifier.Absolute getType() {
                return schemaPath.asAbsolute();
            }

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
        }, schemaPath.asAbsolute());

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

