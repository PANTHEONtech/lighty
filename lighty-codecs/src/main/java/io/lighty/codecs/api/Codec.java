/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.codecs.api;

import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
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
