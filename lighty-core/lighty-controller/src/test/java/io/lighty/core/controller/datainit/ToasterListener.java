/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.datainit;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataObjectWritten;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToasterListener implements DataTreeChangeListener<Toaster> {

    private static final Logger LOG = LoggerFactory.getLogger(ToasterListener.class);

    private CountDownLatch listenerLatch;
    private int expectedDarknessFactor;

    public ToasterListener(CountDownLatch latch, int factor) {
        listenerLatch = latch;
        expectedDarknessFactor = factor;
    }

    @Override
    public void onDataTreeChanged(@NonNull List<DataTreeModification<Toaster>> changes) {
        LOG.debug("Got onDataTreeChanged!");
        for (DataTreeModification<Toaster> modification : changes) {
            final var rootNode = modification.getRootNode();
            if (rootNode instanceof DataObjectWritten<Toaster> written) {
                LOG.debug("Data tree changed: new write modification");
                final var value = written.dataAfter();
                int darknessFactor = value.getDarknessFactor().intValue();
                if (darknessFactor == expectedDarknessFactor) {
                    listenerLatch.countDown();
                }
            }
        }
    }
}

