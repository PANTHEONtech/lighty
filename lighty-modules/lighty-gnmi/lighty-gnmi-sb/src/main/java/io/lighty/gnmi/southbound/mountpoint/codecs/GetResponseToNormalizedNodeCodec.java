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
import com.google.gson.JsonParser;
import gnmi.Gnmi;
import gnmi.Gnmi.Update;
import io.lighty.gnmi.southbound.schema.provider.SchemaContextProvider;
import io.lighty.modules.gnmi.commons.util.DataConverter;
import io.lighty.modules.gnmi.commons.util.JsonUtils;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Default codec which transforms (Gnmi.GetResponse, YangInstanceIdentifier) to NormalizedNode.
 */
public class GetResponseToNormalizedNodeCodec implements BiCodec<Gnmi.GetResponse, YangInstanceIdentifier,
        Optional<NormalizedNode<?, ?>>> {

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
    public Optional<NormalizedNode<?, ?>> apply(Gnmi.GetResponse response, YangInstanceIdentifier identifier)
            throws GnmiCodecException {
        for (Gnmi.Notification notification : response.getNotificationList()) {
            for (Gnmi.Update update : notification.getUpdateList()) {
                // Json to NormalizedNode
                final NormalizedNode<?, ?> codecResult = updateToNormalizedNode(update, identifier);
                /*
                If the serialized normalized node is of type AugmentationNode we need to return the child
                 because the AugmentationNode has no QName so later post processing (for example restconf)
                  can correctly deal with that.
                 */
                if (codecResult instanceof AugmentationNode) {
                    final AugmentationNode node = (AugmentationNode) codecResult;
                    if (node.getIdentifier().getPossibleChildNames().size() == 1
                            && node.getValue().size() == 1) {
                        return Optional.of(node.getValue().stream().findFirst().get());

                    }
                } else {
                    return Optional.of(codecResult);
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private NormalizedNode<?, ?> updateToNormalizedNode(final Update update, final YangInstanceIdentifier identifier)
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
                if (!identifier.isEmpty() && isResponseJsonDeeperThanRequested(identifier, responseJson)) {
                    responseJson = isMapEntryPath(identifier)
                        ? JsonUtils.wrapJsonWithArray(responseJson,
                        identifier.getLastPathArgument().getNodeType().getLocalName(), gson)
                        : JsonUtils.wrapJsonWithObject(responseJson,
                            identifier.getLastPathArgument().getNodeType().getLocalName(), gson);
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

    private static boolean isResponseJsonDeeperThanRequested(final YangInstanceIdentifier identifier,
                                                             final String responseJson) {
        final String lastPathArgName = identifier.getLastPathArgument().getNodeType().getLocalName();
        final JsonElement jsonObject = new JsonParser().parse(responseJson);
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

    @SuppressWarnings("IllegalCatch")
    private NormalizedNode<?, ?> resolveJsonResponse(YangInstanceIdentifier identifier, String inputJson)
            throws GnmiCodecException {
        try {
            return DataConverter.nodeFromJsonString(identifier,inputJson,
                    schemaContextProvider.getSchemaContext());
        } catch (Exception e) {
            throw new GnmiCodecException(String.format("Failed to deserialize json response %s",
                    inputJson), e);
        }
    }

    private static boolean isMapEntryPath(final YangInstanceIdentifier yid) {
        return yid.getLastPathArgument() instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates;
    }

}
