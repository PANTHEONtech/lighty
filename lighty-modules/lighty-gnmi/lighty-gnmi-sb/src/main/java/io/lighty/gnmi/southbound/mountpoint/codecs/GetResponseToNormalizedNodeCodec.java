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
import gnmi.Gnmi;
import gnmi.Gnmi.Update;
import io.lighty.gnmi.southbound.schema.provider.SchemaContextProvider;
import io.lighty.modules.gnmi.commons.util.DataConverter;
import io.lighty.modules.gnmi.commons.util.JsonUtils;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.Module;

/**
 * Default codec which transforms (Gnmi.GetResponse, YangInstanceIdentifier) to NormalizedNode.
 */
public class GetResponseToNormalizedNodeCodec implements BiCodec<Gnmi.GetResponse, YangInstanceIdentifier,
        Optional<NormalizedNode>> {

    private final SchemaContextProvider schemaContextProvider;
    private final Gson gson;

    public GetResponseToNormalizedNodeCodec(final SchemaContextProvider schemaContextProvider,
            final Gson gson) {
        this.schemaContextProvider = schemaContextProvider;
        this.gson = gson;
    }

    /**
     * Apply codec, the result of transformation is NormalizedNode.
     * It also works for cases when the response is rooted one level deeper than requested.
     * ----Example:----
     * Client requested : interfaces/interface=br0/config
     * Device #1 response : "{"openconfig-interfaces:config":{data}}" - we are expecting this
     * Device #2 response : "{data}" - in this case we need to do the wrapping so it becomes #1
     *
     * @param response   get response from device.
     * @param identifier yang identifier from which we constructed the get request.
     * @return normalized nodes parsed from get response.
     * @throws GnmiCodecException if parsing failed.
     */
    @Override
    public Optional<NormalizedNode> apply(Gnmi.GetResponse response, YangInstanceIdentifier identifier)
            throws GnmiCodecException {
        for (Gnmi.Notification notification : response.getNotificationList()) {
            if (!notification.getUpdateList().isEmpty()) {
                // Json to NormalizedNode
                final var update = notification.getUpdateList().get(0);
                final var codecResult = updateToNormalizedNode(update, identifier);
                /*
                If the serialized normalized node is of type AugmentationNode we need to return the child
                 because the AugmentationNode has no QName so later post processing (for example restconf)
                  can correctly deal with that.
                 */
                return Optional.of(codecResult);
            }
        }
        return Optional.empty();
    }

    private NormalizedNode updateToNormalizedNode(final Update update, final YangInstanceIdentifier identifier)
        throws GnmiCodecException {
        switch (update.getVal().getValueCase()) {
            case JSON_VAL:
            case JSON_IETF_VAL:
                String responseJson = update.getVal().getValueCase() == Gnmi.TypedValue.ValueCase.JSON_VAL
                    ? update.getVal().getJsonVal().toStringUtf8()
                    : update.getVal().getJsonIetfVal().toStringUtf8();

                /*
                 Check if response is rooted deeper than requested, if yes, wrap it so it is rooted at
                 the same level as identifier last path arg points to.
                */
                if (!identifier.isEmpty()) {
 
                    final String lastPathArgName = identifier.getLastPathArgument().getNodeType().getLocalName();
                    JsonElement jsonObject = JsonParser.parseString(responseJson);

                    if (isResponseJsonDeeperThanRequested(lastPathArgName, jsonObject)) {
                    final QName lastName = identifier.getLastPathArgument().getNodeType();
                    final Module moduleByQName =
                            DataConverter.findModuleByQName(lastName, schemaContextProvider.getSchemaContext())
                                    .orElseThrow(() -> new GnmiCodecException(
                                            String.format("Unable to find module of node %s", lastName)));

                    final String wrapWith = String.format("%s:%s", moduleByQName.getName(),
                            lastName.getLocalName());
                    if (identifier.getLastPathArgument() instanceof NodeIdentifierWithPredicates) {
                        final NodeIdentifierWithPredicates lastPathArgument
                                = (NodeIdentifierWithPredicates) identifier.getLastPathArgument();
                        responseJson = JsonUtils.wrapJsonWithArray(responseJson, wrapWith, gson, lastPathArgument,
                            schemaContextProvider.getSchemaContext());
                    } else {
                        responseJson = JsonUtils.wrapJsonWithObject(responseJson, wrapWith, gson);
                    }
                } else if (isResponseJsonWithoutNamespace(jsonObject)) {
                    // Add missing namespace to the response from the request.
                    JsonElement responseJsonWithNamespace = addNamespaceToResponseJson(jsonObject, identifier, schemaContextProvider);
                    responseJson = responseJsonWithNamespace.toString();
                }
            }
            return resolveJsonResponse(identifier, responseJson);
                /*
                 In the case of primitive values, only the value is present in response.
                 Since json parser works only with object, always wrap the value (wrapPrimitive()).
                */
            case STRING_VAL:
                return resolveJsonResponse(identifier, JsonUtils.wrapPrimitive(
                    identifier.getLastPathArgument().getNodeType().getLocalName(),
                    update.getVal().getStringVal(), gson));
            case UINT_VAL:
                return resolveJsonResponse(identifier, JsonUtils.wrapPrimitive(
                    identifier.getLastPathArgument().getNodeType().getLocalName(),
                    update.getVal().getUintVal(), gson));
            case INT_VAL:
                return resolveJsonResponse(identifier, JsonUtils.wrapPrimitive(
                    identifier.getLastPathArgument().getNodeType().getLocalName(),
                    update.getVal().getIntVal(), gson));
            case FLOAT_VAL:
                return resolveJsonResponse(identifier, JsonUtils.wrapPrimitive(
                    identifier.getLastPathArgument().getNodeType().getLocalName(),
                    update.getVal().getFloatVal(), gson));
            case BOOL_VAL:
                return resolveJsonResponse(identifier, JsonUtils.wrapPrimitive(
                    identifier.getLastPathArgument().getNodeType().getLocalName(),
                    update.getVal().getBoolVal(), gson));
            default:
                throw new GnmiCodecException(String.format("Unsupported response type %s of response %s",
                    update.getVal().getValueCase(), update));
        }
    }

    private static boolean isResponseJsonDeeperThanRequested(final String lastPathArgName,
                                                              final JsonElement jsonObject) {
        if (!jsonObject.isJsonObject()) {
            return true;
        }
        for (Map.Entry<String, JsonElement> entry : jsonObject.getAsJsonObject().entrySet()) {
            // Don't consider model prefix, if present
            String nameOfElement = entry.getKey();
            if (entry.getKey().contains(":")) {
                String[] split = nameOfElement.split(":");
                nameOfElement = split[split.length - 1];
            }
            if (nameOfElement.contains(lastPathArgName)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isResponseJsonWithoutNamespace(JsonElement jsonElement) {
        Map.Entry<String, JsonElement> firstElement = jsonElement.getAsJsonObject().entrySet().iterator().next();
        if (firstElement.getKey().contains(":")) {
            return false;
        } else {
            return true;
        }
    }

    private static JsonElement addNamespaceToResponseJson(JsonElement jsonElement, 
                                                                final YangInstanceIdentifier identifier, 
                                                                final SchemaContextProvider schemaContextProvider)
                                                                throws GnmiCodecException {
        final QName lastName = identifier.getLastPathArgument().getNodeType();
        final Module moduleByQName =
                DataConverter.findModuleByQName(lastName, schemaContextProvider.getSchemaContext())
                        .orElseThrow(() -> new GnmiCodecException(
                                String.format("Unable to find module of node %s", lastName)));

        final String moduleNameWithNamespace = String.format("%s:%s", moduleByQName.getName(),
                lastName.getLocalName());
        
        Map.Entry<String, JsonElement> entry = jsonElement.getAsJsonObject().entrySet().iterator().next();
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(moduleNameWithNamespace, entry.getValue());
        return jsonObject;
    }

    @SuppressWarnings("IllegalCatch")
    private NormalizedNode resolveJsonResponse(YangInstanceIdentifier identifier, String inputJson)
            throws GnmiCodecException {
        try {
            return DataConverter.nodeFromJsonString(identifier,inputJson,
                    schemaContextProvider.getSchemaContext());
        } catch (Exception e) {
            throw new GnmiCodecException(String.format("Failed to deserialize json response %s",
                    inputJson), e);
        }
    }
}
