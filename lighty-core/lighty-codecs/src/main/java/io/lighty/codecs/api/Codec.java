/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.api;

import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Codec to serailize/deserialize Binding Independent data, RPC data, Notification data to/from
 * Binding Aware data, RPC data, Notification data.
 *
 * @param <BA> - type of Binding Aware data, RPC data or Notification data
 */
public interface Codec<BA extends DataObject> extends Serializer<BA>, Deserializer<BA> {

    /**
     * Get used codec.
     *
     * @return codec
     */
    BindingToNormalizedNodeCodec getCodec();

    NodeConverter withJson();

    NodeConverter withXml();

    SchemaContext getSchemaContext();
}
