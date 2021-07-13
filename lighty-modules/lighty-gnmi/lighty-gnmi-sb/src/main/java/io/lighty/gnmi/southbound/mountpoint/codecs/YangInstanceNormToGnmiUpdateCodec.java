/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.codecs;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.ByteString;
import gnmi.Gnmi;
import io.lighty.gnmi.southbound.schema.provider.SchemaContextProvider;
import io.lighty.modules.gnmi.commons.util.DataConverter;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default codec which transforms (YangInstanceIdentifier, NormalizedNode) to Gnmi.Update.
 */
public class YangInstanceNormToGnmiUpdateCodec implements
        BiCodec<YangInstanceIdentifier, NormalizedNode<?, ?>, Gnmi.Update> {

    private static final Logger LOG = LoggerFactory.getLogger(YangInstanceNormToGnmiUpdateCodec.class);
    private final YangInstanceIdentifierToPathCodec toPathCodec;
    private final SchemaContextProvider schemaContextProvider;
    private final Gson gson;

    public YangInstanceNormToGnmiUpdateCodec(final SchemaContextProvider schemaContextProvider,
            final YangInstanceIdentifierToPathCodec toPathCodec, final Gson gson) {
        this.schemaContextProvider = schemaContextProvider;
        this.toPathCodec = toPathCodec;
        this.gson = gson;
    }

    @Override
    public Gnmi.Update apply(YangInstanceIdentifier identifier, NormalizedNode<?, ?> node)
            throws GnmiCodecException {

        Gnmi.Path path = toPathCodec.apply(identifier);
        final Gnmi.Update.Builder updateBuilder = Gnmi.Update.newBuilder().setPath(path);
        LOG.debug("Converting NormalizedNode {} with identifier {} to json", node, identifier);
        final String json = toJson(identifier, node);
        LOG.debug("Converted NormalizedNode {} with identifier {} to json string: {}", node, identifier, json);
        if (isListEntry(node)) {
            updateBuilder.setVal(Gnmi.TypedValue.newBuilder().setJsonIetfVal(ByteString.copyFromUtf8(json)));
        /*
         Json result is rooted at the same level as schemaPath for non list entries, e.g
         interfaces/interface=admin/config + NormalizedNode config = "{"config":{data}}".
         But the Gnmi.Update requires "{data}", hence the unwrap.
        */
        } else if (isContainer(node)) {
            //Unwrap json
            updateBuilder.setVal(Gnmi.TypedValue.newBuilder()
                    .setJsonIetfVal(ByteString.copyFromUtf8(unwrapContainer(json))));
        } else if (isLeaf(node)) {
            final JsonPrimitive jsonPrimitive = unwrapPrimitive(json);
            // Boolean value case
            if (jsonPrimitive.isBoolean()) {
                updateBuilder.setVal(Gnmi.TypedValue.newBuilder()
                        .setBoolVal(jsonPrimitive.getAsBoolean()));
                // Number value case
            } else if (jsonPrimitive.isNumber()) {
                Number number = jsonPrimitive.getAsNumber();
                Gnmi.TypedValue gnmiVal = isDecimal(number)
                        ? Gnmi.TypedValue.newBuilder().setFloatVal(number.floatValue()).build()
                        : Gnmi.TypedValue.newBuilder().setIntVal(number.intValue()).build();
                updateBuilder.setVal(gnmiVal);
                // String value case
            } else if (jsonPrimitive.isString()) {
                updateBuilder.setVal(Gnmi.TypedValue.newBuilder()
                        .setStringVal(jsonPrimitive.getAsString()));
            }
        } else {
            throw new GnmiCodecException(String.format("Unsupported type of node %s", node));
        }

        return updateBuilder.build();
    }

    @SuppressWarnings("IllegalCatch")
    public String toJson(final YangInstanceIdentifier identifier, final NormalizedNode<?, ?> data)
            throws GnmiCodecException {
        try {
            return DataConverter.jsonStringFromNormalizedNodes(identifier, data,
                    schemaContextProvider.getSchemaContext());
        } catch (Exception e) {
            throw new GnmiCodecException(String.format("Failed to serialize node %s to JSON", data), e);
        }
    }

    private String unwrapContainer(final String json) throws GnmiCodecException {
        final JsonElement jsonElement = new JsonParser().parse(json);
        if (!jsonElement.isJsonObject()) {
            throw new GnmiCodecException("Can't unwrap non json object");
        }
        Set<Entry<String, JsonElement>> jsonEntry = jsonElement.getAsJsonObject().entrySet();
        if (jsonEntry.size() == 1) {
            final JsonElement elem = jsonEntry.stream().findFirst().get().getValue();
            return gson.toJson(elem);
        } else if (jsonEntry.size() == 0) {
            return json;
        } else {
            throw new GnmiCodecException("Can't unwrap json object with multiple inner values");
        }
    }

    private static JsonPrimitive unwrapPrimitive(final String json) throws GnmiCodecException {
        final JsonElement jsonElement = new JsonParser().parse(json);
        if (!jsonElement.isJsonObject()) {
            throw new GnmiCodecException(String.format("Json %s is not a json object", json));
        }
        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.entrySet().size() == 1) {
            final JsonElement value = jsonObject.entrySet().stream().findFirst().get().getValue();
            if (value.isJsonPrimitive()) {
                return value.getAsJsonPrimitive();
            } else {
                throw new GnmiCodecException(String.format("Json %s is not in form \"{Leaf_name:primitive_val}\" ",
                        json));
            }
        } else {
            throw new GnmiCodecException(String.format("Json %s is not in form \"{Leaf_name:value}\","
                    + " multiple entries not permitted ", json));
        }
    }

    private static boolean isListEntry(final NormalizedNode<?, ?> node) {
        return node instanceof MapEntryNode;
    }

    private static boolean isLeaf(final NormalizedNode<?, ?> node) {
        return node instanceof LeafNode;
    }

    private static boolean isContainer(final NormalizedNode<?, ?> node) {
        return node instanceof ContainerNode;
    }

    private static boolean isDecimal(final Number number) {
        return number.doubleValue() % 1 != 0;
    }

}
