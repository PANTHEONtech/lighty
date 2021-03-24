/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.api;

import java.util.Map.Entry;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * This class deserializes Binding Independent (DOM) objects FROM Binding Aware (BA) objects.
 *
 * @param <BA> Binding Aware object type data, RPC data or Notification data.
 * @deprecated The interface is no longer needed. Used methods are covered by {@link BindingNormalizedNodeSerializer}.
 */
@Deprecated(forRemoval = true)
public interface Deserializer<BA extends DataObject> {

    /**
     * Deserialize Binding Independent identifier FROM Binding Aware identifier.
     *
     * @param identifier Binding Aware identifier to be deserialized
     * @return deserialized Binding Independent identifier
     */
    YangInstanceIdentifier deserializeIdentifier(InstanceIdentifier<BA> identifier);

    String deserializeIdentifier(YangInstanceIdentifier identifier);

    /**
     * Deserialize Binding Independent data FROM Binding Aware data.
     *
     * @param identifier Identifier of Binding Aware data
     * @param data Binding Aware data to be deserialized
     * @return deserialized Binding Independent data with Binding Independent identifier wrapped in {@link Entry}
     */
    Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> convertToNormalizedNode(InstanceIdentifier<BA> identifier,
            BA data);

    /**
     * Deserialize Binding Independent RPC data(input/output) FROM Binding Aware RPC data(input/output).
     *
     * @param rpcData Binding Aware RPC data to be deserialized
     * @return deserialized Binding Independent RPC data
     */
    ContainerNode convertToBindingIndependentRpc(DataContainer rpcData);

    /**
     * Deserialize Binding Independent Notification data(input/output) FROM Binding Aware Notification
     * data(input/output).
     *
     * @param notificationData Binding Aware Notification data to be deserialized
     * @return deserialized Binding Independent Notification data
     */
    ContainerNode convertToBindingIndependentNotification(Notification notificationData);
}
