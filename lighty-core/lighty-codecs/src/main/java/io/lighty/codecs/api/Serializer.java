/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.api;

import java.util.Collection;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * This class serializes Binding Independent (DOM) objects TO Binding Aware (BA) objects.
 *
 * @param <BA>  Binding Aware object type of data, RPC data or Notification data
 * @deprecated The interface is no longer needed. The most used methods are covered by
 * {@link BindingNormalizedNodeSerializer}.
 */
@Deprecated(forRemoval = true)
public interface Serializer<BA extends DataObject> {

    YangInstanceIdentifier convertIdentifier(String identifier);

    /**
     * Serialize Binding Independent data TO Binding Aware data.
     *
     * @param identifier identifier of Binding Independent data
     * @param data Binding Independent data to be serialized
     * @return serialized Binding Aware data
     */
    BA convertToBindingAwareData(YangInstanceIdentifier identifier, NormalizedNode<?, ?> data);

    /**
     * Serialize the Binding Independent {@link MapNode} into Binding Aware data list.
     *
     * @param identifier identifier of Binding Independent data
     * @param mapNode Binding Independent data to be serialized
     * @return {@link DataObject} collection.
     */
    Collection<BA> convertBindingAwareList(YangInstanceIdentifier identifier, MapNode mapNode);

    /**
     * Serialize restconf error to Normalized Node.
     *
     * @param body restconf error input data
     * @return normalized node of the restconf error
     */
    NormalizedNode<?, ?> serializeXMLError(String body);

    /**
     * Serialize Binding Independent RPC data (input/output) TO Binding Aware RPC data (input/output).
     *
     * @param schemaPath
     *            - schema path of RPC
     * @param rpcData
     *            - Binding Independent RPC data to be serialized
     * @return serialized Binding Aware RPC data
     */
    BA convertToBindingAwareRpc(SchemaPath schemaPath, ContainerNode rpcData);

    /**
     * Serialize Binding Independent Notification data TO Binding Aware Notification data.
     *
     * @param schemaPath schema path of Notification
     * @param norificationData Binding Independent Notification data to be serialized
     * @return serialized Binding Aware Notification data
     */
    BA convertToBindingAwareNotification(SchemaPath schemaPath, ContainerNode norificationData);
}
