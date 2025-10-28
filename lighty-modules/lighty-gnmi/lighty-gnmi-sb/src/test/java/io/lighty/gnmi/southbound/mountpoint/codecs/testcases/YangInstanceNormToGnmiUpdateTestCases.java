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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import gnmi.Gnmi;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.gnmi.southbound.schema.impl.SchemaException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class YangInstanceNormToGnmiUpdateTestCases extends CodecTestCasesBase {
    private static final Path BASE_JSON_PATH = Paths.get(
            "src/test/resources/codecs/jsons/reference_data_prefixed.json");
    private static final Gson GSON = new Gson();

    private final String baseJson;

    public YangInstanceNormToGnmiUpdateTestCases()
            throws YangLoadException, SchemaException, IOException, ConfigurationException {
        super();
        this.baseJson = Files.readString(BASE_JSON_PATH);
    }

    /**
     * Returns test case for top schema element (openconfig-interfaces:interfaces).
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, NormalizedNode>, Gnmi.Update> topElementTestCase() {

        final JsonObject jsonInterfaces =
                JsonParser.parseString(baseJson)
                        .getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"));
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces"))
                .build();
        final Gnmi.Update expectedUpdate = Gnmi.Update.newBuilder().setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                .setJsonIetfVal(ByteString.copyFromUtf8(GSON.toJson(jsonInterfaces)))).build();

        return Maps.immutableEntry(super.topElementCase(),expectedUpdate);
    }

    /**
     * Returns test case for list entry (openconfig-interfaces:interfaces/interface=eth3).
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, NormalizedNode>, Gnmi.Update> listEntryTestCase() {

        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "eth3"))
                .build();

        final JsonObject jsonInterfaceEth3 =
                JsonParser.parseString(baseJson).getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"))
                        .getAsJsonArray("interface").get(0).getAsJsonObject();

        final Gnmi.Update expectedUpdate = Gnmi.Update.newBuilder().setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                        .setJsonIetfVal(ByteString.copyFromUtf8(GSON.toJson(jsonInterfaceEth3)))).build();

        return Maps.immutableEntry(super.listEntryCase(false),expectedUpdate);
    }

    /**
     * Returns test case for simple container (openconfig-interfaces:interfaces/interface=eth3/config).
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, NormalizedNode>, Gnmi.Update> containerTestCase() {

        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "eth3"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config"))
                .build();

        final JsonObject jsonInterfaceEthConfig =
                JsonParser.parseString(baseJson).getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"))
                        .getAsJsonArray("interface")
                        .get(0).getAsJsonObject().getAsJsonObject("config");

        final Gnmi.Update expectedUpdate = Gnmi.Update.newBuilder().setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                        .setJsonIetfVal(ByteString.copyFromUtf8(GSON.toJson(jsonInterfaceEthConfig))))
                .build();

        return Maps.immutableEntry(super.containerCase(),expectedUpdate);
    }

    /**
     * Returns test case for augmented container (openconfig-interfaces:interfaces/interface=br0/ethernet/config).
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, NormalizedNode>, Gnmi.Update> augmentedContainerCase() {

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


        final JsonObject jsonInterfaceEthConfig =
                JsonParser.parseString(baseJson).getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"))
                        .getAsJsonArray("interface")
                        .get(1).getAsJsonObject().getAsJsonObject(makePrefixString(OC_IF_ETHERNET_ID, "ethernet"))
                        .getAsJsonObject("config");

        final Gnmi.Update expectedUpdate = Gnmi.Update.newBuilder().setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                        .setJsonIetfVal(ByteString.copyFromUtf8(GSON.toJson(jsonInterfaceEthConfig))))
                .build();

        return Maps.immutableEntry(super.containerAugmentedCase(),expectedUpdate);
    }

    /**
     * Returns test case for number leaf (openconfig-interfaces:interfaces/interface=eth3/config/mtu).
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, NormalizedNode>, Gnmi.Update> leafNumberTestCase() {

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

        final JsonElement jsonMtu =
                JsonParser.parseString(baseJson).getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"))
                        .getAsJsonArray("interface").get(0)
                        .getAsJsonObject().getAsJsonObject("config").getAsJsonPrimitive("mtu");

        final Gnmi.Update expectedUpdate = Gnmi.Update.newBuilder().setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                        .setIntVal(jsonMtu.getAsInt()))
                .build();

        return Maps.immutableEntry(super.leafNumberCase(), expectedUpdate);
    }

    /**
     * Returns test case for string leaf (openconfig-interfaces:interfaces/interface=eth3/config/name).
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, NormalizedNode>, Gnmi.Update> leafStringTestCase() {

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

        final JsonElement jsonConfigName =
                JsonParser.parseString(baseJson).getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"))
                        .getAsJsonArray("interface").get(0)
                        .getAsJsonObject().getAsJsonObject("config").getAsJsonPrimitive("name");

        final Gnmi.Update expectedUpdate = Gnmi.Update.newBuilder().setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                        .setStringVal(jsonConfigName.getAsString()))
                .build();

        return Maps.immutableEntry(super.leafStringCase(), expectedUpdate);
    }

    /**
     * Returns test case for boolean leaf (openconfig-interfaces:interfaces/interface=eth3/config/loopback-mode).
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, NormalizedNode>, Gnmi.Update> leafBooleanTestCase() {

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

        final JsonElement jsonLoopbackMode =
                JsonParser.parseString(baseJson).getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"))
                        .getAsJsonArray("interface").get(0)
                        .getAsJsonObject().getAsJsonObject("config").getAsJsonPrimitive("loopback-mode");

        final Gnmi.Update expectedUpdate = Gnmi.Update.newBuilder().setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                        .setBoolVal(jsonLoopbackMode.getAsBoolean()))
                .build();

        return Maps.immutableEntry(super.leafBooleanCase(), expectedUpdate);

    }

    /**
     * Returns test case for augmented leaf (openconfig-interfaces:interfaces/interface=br0/ethernet/config/
     *  aggregate-id).
     * @return test case ((inputs to codec), expected output).
     */
    public Map.Entry<ImmutablePair<YangInstanceIdentifier, NormalizedNode>, Gnmi.Update> leafAugmentedTestCase() {
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

        final JsonElement jsonAggregateId =
                JsonParser.parseString(baseJson).getAsJsonObject()
                        .getAsJsonObject(makePrefixString(OC_INTERFACES_ID, "interfaces"))
                        .getAsJsonArray("interface").get(1)
                        .getAsJsonObject().getAsJsonObject(makePrefixString(OC_IF_ETHERNET_ID, "ethernet"))
                        .getAsJsonObject("config")
                        .getAsJsonPrimitive(makePrefixString(OC_IF_AGGREGATE_ID, "aggregate-id"));

        final Gnmi.Update expectedUpdate = Gnmi.Update.newBuilder().setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                        .setStringVal(jsonAggregateId.getAsString()))
                .build();

        return Maps.immutableEntry(super.leafAgumentedCase(), expectedUpdate);

    }
}
