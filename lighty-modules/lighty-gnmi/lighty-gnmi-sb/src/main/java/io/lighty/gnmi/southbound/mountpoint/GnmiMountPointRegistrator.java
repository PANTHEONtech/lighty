/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint;

import com.google.common.base.Preconditions;
import io.lighty.gnmi.southbound.identifier.IdentifierUtils;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.spi.FixedDOMSchemaService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnmiMountPointRegistrator implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiMountPointRegistrator.class);

    private final DOMMountPointService mountPointService;
    private final Map<NodeId, ObjectRegistration<DOMMountPoint>> registeredMountPoints;

    public GnmiMountPointRegistrator(@NonNull final DOMMountPointService mountService) {
        this.mountPointService = mountService;
        this.registeredMountPoints = new ConcurrentHashMap<>();
    }


    public void registerMountPoint(final Node node, final EffectiveModelContext schemaContext,
                                   final DOMDataBroker dataBroker) {
        Preconditions.checkState(!registeredMountPoints.containsKey(node.getNodeId()),
                "Mount point for node %s already exists!", node.getNodeId().getValue());
        final DOMMountPointService.DOMMountPointBuilder builder = mountPointService
                .createMountPoint(IdentifierUtils.nodeidToYii(node.getNodeId()));
        builder.addService(DOMSchemaService.class, FixedDOMSchemaService.of(schemaContext));
        builder.addService(DOMDataBroker.class, dataBroker);
        final ObjectRegistration<DOMMountPoint> registration = builder.register();
        registeredMountPoints.put(node.getNodeId(), registration);
        LOG.info("Mount point for node {} created: {}", node.getNodeId().getValue(), registration);
    }

    public void unregisterMountPoint(final NodeId nodeId) {
        final Optional<ObjectRegistration<DOMMountPoint>> optRegistration =
                Optional.ofNullable(registeredMountPoints.get(nodeId));

        if (optRegistration.isPresent()) {
            try {
                optRegistration.get().close();
            } finally {
                LOG.debug("Mountpoint for node {} removed", nodeId.getValue());
                registeredMountPoints.remove(nodeId);
            }
        } else {
            LOG.warn("Mount point for node {} is not initialized, cant remove", nodeId.getValue());
        }
    }

    @Override
    public void close() throws Exception {
        LOG.debug("Closing Mount Points");
        for (AutoCloseable closable : registeredMountPoints.values()) {
            closable.close();
        }
    }
}
