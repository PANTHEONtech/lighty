/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.codecs.testcases;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import gnmi.Gnmi;
import io.lighty.gnmi.southbound.schema.impl.SchemaException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class GetResponseToNormalizedNodeTestCases extends CodecTestCasesBase {

    private static final Path BASE_JSON_PATH = Paths.get(
            "src/test/resources/codecs/jsons/reference_data.json");
    private static final Gson GSON = new Gson();

    private final String baseJson;

    public GetResponseToNormalizedNodeTestCases()
            throws IOException, YangLoadException, SchemaException, ConfigurationException {
        super();
        this.baseJson = Files.readString(BASE_JSON_PATH);
    }

    /**
     * Returns test case for root schema element.
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>,
            NormalizedNode> rootCase() {

        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .build();
        final String jsonResponse = baseJson;

        final Gnmi.GetResponse getResponse = Gnmi.GetResponse.newBuilder()
                .addNotification(Gnmi.Notification.newBuilder()
                        .addUpdate(Gnmi.Update.newBuilder().setPath(path)
                                .setVal(Gnmi.TypedValue.newBuilder()
                                        .setJsonIetfVal(ByteString.copyFromUtf8(jsonResponse)))))
                .build();

        final ImmutablePair<YangInstanceIdentifier,NormalizedNode> testCase = super.rootElementTestCase();
        return Maps.immutableEntry(ImmutablePair.of(testCase.left, getResponse), testCase.right);
    }

    /**
     * Returns test case for top schema element (openconfig-interfaces:interfaces).
     * @param oneLevelDeeperThanRequest should the gnmi.GetResponse json value which the tested codec parses be rooted
     *                                  one level deeper than YangInstanceIdentifier/Gnmi.Path points to?
     *                                  (codec should be able to deal with that case also, since it is the case with
     *                                  responses from some devices.)
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>,
            NormalizedNode> topElementTestCase(final boolean oneLevelDeeperThanRequest) {

        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces"))
                .build();
        JsonObject interfacesObject = JsonParser.parseString(baseJson).getAsJsonObject()
                .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"));

        if (!oneLevelDeeperThanRequest) {
            JsonObject obj = new JsonObject();
            obj.add(makePrefixString(OC_INTERFACES_ID, "interfaces"),interfacesObject);
            interfacesObject = obj;
        }

        final String jsonResponse = GSON.toJson(interfacesObject);


        final Gnmi.GetResponse getResponse = Gnmi.GetResponse.newBuilder()
                .addNotification(Gnmi.Notification.newBuilder()
                        .addUpdate(Gnmi.Update.newBuilder().setPath(path)
                                .setVal(Gnmi.TypedValue.newBuilder()
                                        .setJsonIetfVal(ByteString.copyFromUtf8(jsonResponse)))))
                .build();

        final ImmutablePair<YangInstanceIdentifier,NormalizedNode> testCase = super.topElementCase();
        return Maps.immutableEntry(ImmutablePair.of(testCase.left, getResponse), testCase.right);
    }

    /**
     * Returns test case for list entry (openconfig-interfaces:interfaces/interface=eth3).
     * @param oneLevelDeeperThanRequest should the gnmi.GetResponse json value which the tested codec parses be rooted
     *                                    one level deeper than YangInstanceIdentifier/Gnmi.Path points to?
     *                                    (codec should be able to deal with that case also, since it is the case with
     *                                    responses from some devices.)
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>, NormalizedNode> listEntryTestCase(
            final boolean oneLevelDeeperThanRequest) {

        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "eth3"))
                .build();

        String jsonResponse = baseJson;
        JsonObject jsonInterfaceEth3 =
                JsonParser.parseString(jsonResponse).getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"))
                        .getAsJsonArray("interface").get(0).getAsJsonObject();

        if (oneLevelDeeperThanRequest) {
            final JsonObject wrapped = new JsonObject();
            final JsonArray array = new JsonArray();
            array.add(jsonInterfaceEth3);
            wrapped.add(String.format("%s:%s", OC_INTERFACES_ID, "interface"), array);
            jsonInterfaceEth3 = wrapped;
        }

        jsonResponse = GSON.toJson(jsonInterfaceEth3);

        final Gnmi.GetResponse getResponse = Gnmi.GetResponse.newBuilder()
                .addNotification(Gnmi.Notification.newBuilder()
                        .addUpdate(Gnmi.Update.newBuilder().setPath(path)
                                .setVal(Gnmi.TypedValue.newBuilder()
                                        .setJsonIetfVal(ByteString.copyFromUtf8(jsonResponse)))))
                .build();

        final ImmutablePair<YangInstanceIdentifier,NormalizedNode> testCase = super.listEntryCase(true);
        return Maps.immutableEntry(ImmutablePair.of(testCase.left, getResponse), testCase.right);
    }

    /**
     * Returns test case for simple container (openconfig-interfaces:interfaces/interface=eth3/config).
     * @param oneLevelDeeperThanRequest should the gnmi.GetResponse json value which the tested codec parses be rooted
     *                                    one level deeper than YangInstanceIdentifier/Gnmi.Path points to?
     *                                    (codec should be able to deal with that case also, since it is the case with
     *                                    responses from some devices.)
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>, NormalizedNode> containerTestCase(
            final boolean oneLevelDeeperThanRequest) {

        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "eth3"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config"))
                .build();

        String jsonResponse = baseJson;
        JsonObject jsonInterfaceConfig =
                JsonParser.parseString(jsonResponse).getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"))
                        .getAsJsonArray("interface")
                        .get(0).getAsJsonObject().getAsJsonObject("config");

        if (!oneLevelDeeperThanRequest) {
            final JsonObject wrapped = new JsonObject();
            wrapped.add(makePrefixString(OC_INTERFACES_ID, "config"), jsonInterfaceConfig);
            jsonInterfaceConfig = wrapped;
        }

        jsonResponse = GSON.toJson(jsonInterfaceConfig);

        final Gnmi.GetResponse getResponse = Gnmi.GetResponse.newBuilder()
                .addNotification(Gnmi.Notification.newBuilder()
                        .addUpdate(Gnmi.Update.newBuilder().setPath(path)
                                .setVal(Gnmi.TypedValue.newBuilder()
                                        .setJsonIetfVal(ByteString.copyFromUtf8(jsonResponse)))))
                .build();

        final ImmutablePair<YangInstanceIdentifier,NormalizedNode> testCase = super.containerCase();
        return Maps.immutableEntry(ImmutablePair.of(testCase.left, getResponse), testCase.right);
    }

    /**
     * Returns test case for augmented container (openconfig-interfaces:interfaces/interface=br0/ethernet/config).
     * @param oneLevelDeeperThanRequest should the gnmi.GetResponse json value which the tested codec parses be rooted
     *                                    one level deeper than YangInstanceIdentifier/Gnmi.Path points to?
     *                                    (codec should be able to deal with that case also, since it is the case with
     *                                    responses from some devices.)
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>,
            NormalizedNode> augmentedContainerCase(final boolean oneLevelDeeperThanRequest) {

        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "br0"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("ethernet"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config"))
                .build();

        String jsonResponse = baseJson;
        JsonObject jsonInterfaceEthConfig =
                JsonParser.parseString(jsonResponse).getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"))
                        .getAsJsonArray("interface")
                        .get(1).getAsJsonObject().getAsJsonObject("ethernet")
                        .getAsJsonObject("config");

        if (!oneLevelDeeperThanRequest) {
            final JsonObject wrapped = new JsonObject();
            wrapped.add(makePrefixString(OC_IF_ETHERNET_ID, "config"), jsonInterfaceEthConfig);
            jsonInterfaceEthConfig = wrapped;
        }

        jsonResponse = GSON.toJson(jsonInterfaceEthConfig);

        final Gnmi.GetResponse getResponse = Gnmi.GetResponse.newBuilder()
                .addNotification(Gnmi.Notification.newBuilder()
                        .addUpdate(Gnmi.Update.newBuilder().setPath(path)
                                .setVal(Gnmi.TypedValue.newBuilder()
                                        .setJsonIetfVal(ByteString.copyFromUtf8(jsonResponse)))))
                .build();

        final ImmutablePair<YangInstanceIdentifier,NormalizedNode> testCase = super.containerAugmentedCase();
        return Maps.immutableEntry(ImmutablePair.of(testCase.left, getResponse), testCase.right);
    }

    /**
     * Returns test case for number leaf (openconfig-interfaces:interfaces/interface=eth3/config/mtu).
     * @param deeperThanRequested if the GetResponse value should be simple type (UintVal) or formatted as json
     *                            "{mtu:1500}".
     *                            (codec should handle both cases).
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>, NormalizedNode> leafNumberTestCase(
            final boolean responseAsJson, final boolean deeperThanRequested) {

        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "eth3"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("mtu"))
                .build();

        String jsonResponse = baseJson;
        JsonElement jsonMtu =
                JsonParser.parseString(jsonResponse).getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"))
                        .getAsJsonArray("interface").get(0)
                        .getAsJsonObject().getAsJsonObject("config").getAsJsonPrimitive("mtu");

        if (!deeperThanRequested) {
            JsonObject obj = new JsonObject();
            obj.add("mtu", jsonMtu);
            jsonMtu = obj;
        }
        jsonResponse = GSON.toJson(jsonMtu);

        final Gnmi.GetResponse getResponse = Gnmi.GetResponse.newBuilder()
                .addNotification(Gnmi.Notification.newBuilder()
                        .addUpdate(Gnmi.Update.newBuilder().setPath(path)
                                .setVal(responseAsJson
                                        ? Gnmi.TypedValue.newBuilder()
                                        .setJsonIetfVal(ByteString.copyFromUtf8(jsonResponse))
                                        : Gnmi.TypedValue.newBuilder()
                                        .setUintVal(jsonMtu.getAsLong()))))
                .build();

        final ImmutablePair<YangInstanceIdentifier,NormalizedNode> testCase = super.leafNumberCase();
        return Maps.immutableEntry(ImmutablePair.of(testCase.left, getResponse), testCase.right);
    }

    /**
     * Returns test case for string leaf (openconfig-interfaces:interfaces/interface=eth3/config/name).
     * @param deeperThanRequested if the GetResponse value should be simple type (StringVal) or formatted as json
     *                            "{name:"admin"}".
     *                            (codec should handle both cases).
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>, NormalizedNode> leafStringTestCase(
            final boolean responseAsJson, final boolean deeperThanRequested) {

        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "eth3"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("name"))
                .build();

        String jsonResponse = baseJson;
        JsonElement jsonConfigName =
                JsonParser.parseString(jsonResponse).getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"))
                        .getAsJsonArray("interface").get(0)
                        .getAsJsonObject().getAsJsonObject("config").getAsJsonPrimitive("name");

        if (!deeperThanRequested) {
            JsonObject obj = new JsonObject();
            obj.add("name", jsonConfigName);
            jsonConfigName = obj;
        }
        jsonResponse = GSON.toJson(jsonConfigName);

        final Gnmi.GetResponse getResponse = Gnmi.GetResponse.newBuilder()
                .addNotification(Gnmi.Notification.newBuilder()
                        .addUpdate(Gnmi.Update.newBuilder().setPath(path)
                                .setVal(responseAsJson
                                        ? Gnmi.TypedValue.newBuilder()
                                        .setJsonIetfVal(ByteString.copyFromUtf8(jsonResponse))
                                        : Gnmi.TypedValue.newBuilder()
                                        .setStringVal(jsonConfigName.getAsString()))))
                .build();

        final ImmutablePair<YangInstanceIdentifier,NormalizedNode> testCase = super.leafStringCase();
        return Maps.immutableEntry(ImmutablePair.of(testCase.left, getResponse), testCase.right);
    }

    /**
     * Returns test case for boolean leaf (openconfig-interfaces:interfaces/interface=eth3/config/loopback-mode).
     * @param deeperThanRequested if the GetResponse value should be simple type (BoolVal) or formatted as json
     *                            "{loopback-mode:false}".
     *                            (codec should handle both cases).
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>, NormalizedNode> leafBooleanTestCase(
            final boolean responseAsJson, final boolean deeperThanRequested) {

        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "eth3"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("loopback-mode"))
                .build();

        String jsonResponse = baseJson;
        JsonElement jsonLoopbackMode =
                JsonParser.parseString(jsonResponse).getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"))
                        .getAsJsonArray("interface").get(0)
                        .getAsJsonObject().getAsJsonObject("config").getAsJsonPrimitive("loopback-mode");

        if (!deeperThanRequested) {
            JsonObject obj = new JsonObject();
            obj.add("loopback-mode", jsonLoopbackMode);
            jsonLoopbackMode = obj;
        }
        jsonResponse = GSON.toJson(jsonLoopbackMode);

        final Gnmi.GetResponse getResponse = Gnmi.GetResponse.newBuilder()
                .addNotification(Gnmi.Notification.newBuilder()
                        .addUpdate(Gnmi.Update.newBuilder().setPath(path)
                                .setVal(responseAsJson
                                        ? Gnmi.TypedValue.newBuilder()
                                        .setJsonIetfVal(ByteString.copyFromUtf8(jsonResponse))
                                        : Gnmi.TypedValue.newBuilder()
                                        .setBoolVal(jsonLoopbackMode.getAsBoolean()))))
                .build();

        final ImmutablePair<YangInstanceIdentifier,NormalizedNode> testCase = super.leafBooleanCase();
        return Maps.immutableEntry(ImmutablePair.of(testCase.left, getResponse), testCase.right);

    }

    /**
     * Returns test case for augmented leaf (openconfig-interfaces:interfaces/interface=br0/
     *  openconfig-ethernet:ethernet/config/openconfig-if-aggregate:aggregate-id).
     * @param deeperThanRequested if the GetResponse value should be simple type (StringVal) or formatted as json
     *                            "{aggregate-id:"admin"}".
     *                            (codec should handle both cases).
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>,
            NormalizedNode> leafAugmentedTestCase(final boolean responseAsJson,
                                                        final boolean deeperThanRequested) {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "br0"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("ethernet"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("aggregate-id"))
                .build();

        String jsonResponse = baseJson;
        JsonElement jsonAggregateId =
                JsonParser.parseString(jsonResponse).getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"))
                        .getAsJsonArray("interface").get(1)
                        .getAsJsonObject().getAsJsonObject("ethernet")
                        .getAsJsonObject("config")
                        .getAsJsonPrimitive("aggregate-id");

        if (!deeperThanRequested) {
            JsonObject obj = new JsonObject();
            obj.add("aggregate-id", jsonAggregateId);
            jsonAggregateId = obj;
        }
        jsonResponse = GSON.toJson(jsonAggregateId);

        final Gnmi.GetResponse getResponse = Gnmi.GetResponse.newBuilder()
                .addNotification(Gnmi.Notification.newBuilder()
                        .addUpdate(Gnmi.Update.newBuilder().setPath(path)
                                .setVal(responseAsJson
                                        ? Gnmi.TypedValue.newBuilder()
                                        .setJsonIetfVal(ByteString.copyFromUtf8(jsonResponse))
                                        : Gnmi.TypedValue.newBuilder()
                                        .setStringVal(jsonAggregateId.getAsString()))))
                .build();

        final ImmutablePair<YangInstanceIdentifier,NormalizedNode> testCase = super.leafAgumentedCase();
        return Maps.immutableEntry(ImmutablePair.of(testCase.left, getResponse), testCase.right);

    }

}
