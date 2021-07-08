/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.device.connection;

import io.lighty.gnmi.southbound.device.session.listener.GnmiConnectionStatusListener;
import io.lighty.gnmi.southbound.device.session.provider.GnmiSessionProvider;
import io.lighty.gnmi.southbound.schema.provider.SchemaContextProvider;
import io.lighty.modules.gnmi.connector.gnmi.session.api.GnmiSession;
import io.lighty.modules.gnmi.connector.session.api.SessionProvider;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiNode;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.connection.parameters.ExtensionsParameters;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

/**
 * Holds gNMI session of one connected gNMI device.
 */
public class DeviceConnection implements GnmiSessionProvider, SchemaContextProvider, AutoCloseable {

    private final SessionProvider sessionProvider;
    private final GnmiConnectionStatusListener connectionStatusListener;
    private final Node node;
    private final ConfigurableParameters configurableParameters;
    private EffectiveModelContext schemaContext;

    public DeviceConnection(final SessionProvider sessionProvider,
                            final GnmiConnectionStatusListener connectionStatusListener, final Node node) {
        this.sessionProvider = sessionProvider;
        this.connectionStatusListener = connectionStatusListener;
        this.node = node;
        final ExtensionsParameters extensionsParameters = resolveExtensionsParameters();
        configurableParameters = new ConfigurableParameters(extensionsParameters);
    }

    private ExtensionsParameters resolveExtensionsParameters() {
        final GnmiNode gnmiNode = node.augmentation(GnmiNode.class);

        return gnmiNode == null ? null : gnmiNode.getExtensionsParameters();
    }

    public ConfigurableParameters getConfigurableParameters() {
        return configurableParameters;
    }

    public EffectiveModelContext getSchemaContext() {
        return schemaContext;
    }

    public void setSchemaContext(final EffectiveModelContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    public GnmiSession getGnmiSession() {
        return sessionProvider.getGnmiSession();
    }

    @Override
    public void close() throws Exception {
        sessionProvider.close();
        connectionStatusListener.close();
    }

    public NodeId getIdentifier() {
        return node.getNodeId();
    }
}
