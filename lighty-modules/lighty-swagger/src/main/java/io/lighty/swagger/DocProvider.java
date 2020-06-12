/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.swagger;

import io.lighty.swagger.impl.ApiDocGenerator;
import io.lighty.swagger.mountpoints.MountPointSwagger;
import java.util.LinkedList;
import java.util.List;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DocProvider.class);

    private final List<AutoCloseable> toClose = new LinkedList<>();

    public DocProvider(final SchemaService schemaService, final DOMMountPointService mountService) {

        ApiDocGenerator.getInstance().setSchemaService(schemaService);

        final ListenerRegistration<MountProvisionListener> registration = mountService
                .registerProvisionListener(MountPointSwagger.getInstance());
        MountPointSwagger.getInstance().setGlobalSchema(schemaService);
        synchronized (toClose) {
            toClose.add(registration);
        }
        MountPointSwagger.getInstance().setMountService(mountService);

        LOG.debug("Restconf API Explorer started");
    }

    public void close() throws Exception {
        synchronized (toClose) {
            for (final AutoCloseable close : toClose) {
                close.close();
            }
        }
    }
}
