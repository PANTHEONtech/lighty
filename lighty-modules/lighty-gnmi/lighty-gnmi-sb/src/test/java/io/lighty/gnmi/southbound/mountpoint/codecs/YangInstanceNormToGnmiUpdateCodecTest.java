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
import io.lighty.gnmi.southbound.mountpoint.codecs.testcases.YangInstanceNormToGnmiUpdateTestCases;
import io.lighty.gnmi.southbound.schema.impl.SchemaException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.testng.Assert;

public class YangInstanceNormToGnmiUpdateCodecTest {

    private static YangInstanceNormToGnmiUpdateTestCases testCases;
    private static YangInstanceNormToGnmiUpdateCodec codec;

    @BeforeAll
    public static void init() throws IOException, YangLoadException, SchemaException {
        testCases = new YangInstanceNormToGnmiUpdateTestCases();
        codec = new YangInstanceNormToGnmiUpdateCodec(
                testCases.getSchemaContextProvider(),
                new YangInstanceIdentifierToPathCodec(testCases.getSchemaContextProvider(),
                        false), new Gson());
    }

    @Test
    public void topLevelElementTest() throws GnmiCodecException {
        final Map.Entry<ImmutablePair<YangInstanceIdentifier, NormalizedNode<?, ?>>, Gnmi.Update> prepared =
                testCases.topElementTestCase();
        final Gnmi.Update result = codec.apply(prepared.getKey().left, prepared.getKey().right);
        assertUpdateEquals(prepared.getValue(), result);
    }

    @Test
    public void listEntryCase() throws GnmiCodecException {
        final Map.Entry<ImmutablePair<YangInstanceIdentifier, NormalizedNode<?, ?>>, Gnmi.Update> prepared =
                testCases.listEntryTestCase();
        final Gnmi.Update result = codec.apply(prepared.getKey().left, prepared.getKey().right);
        assertUpdateEquals(prepared.getValue(), result);
    }

    @Test
    public void containerCase() throws GnmiCodecException {
        Map.Entry<ImmutablePair<YangInstanceIdentifier, NormalizedNode<?, ?>>, Gnmi.Update> prepared =
                testCases.containerTestCase();
        Gnmi.Update result = codec.apply(prepared.getKey().left, prepared.getKey().right);
        assertUpdateEquals(prepared.getValue(), result);
        // ------Augmented case:----------
        prepared = testCases.augmentedContainerCase();
        result = codec.apply(prepared.getKey().left, prepared.getKey().right);
        assertUpdateEquals(prepared.getValue(), result);

    }

    @Test
    public void simpleValuesTest() throws GnmiCodecException {
        // ------Boolean case:----------
        Map.Entry<ImmutablePair<YangInstanceIdentifier, NormalizedNode<?, ?>>, Gnmi.Update> prepared =
                testCases.leafBooleanTestCase();
        Gnmi.Update result = codec.apply(prepared.getKey().left, prepared.getKey().right);
        assertUpdateEquals(prepared.getValue(), result);
        // ------Number case:----------
        prepared = testCases.leafNumberTestCase();
        result = codec.apply(prepared.getKey().left, prepared.getKey().right);
        assertUpdateEquals(prepared.getValue(), result);
        // ------String case:----------
        prepared = testCases.leafStringTestCase();
        result = codec.apply(prepared.getKey().left, prepared.getKey().right);
        assertUpdateEquals(prepared.getValue(), result);
        // ------Augmented case:----------
        prepared = testCases.leafAugmentedTestCase();
        result = codec.apply(prepared.getKey().left, prepared.getKey().right);
        assertUpdateEquals(prepared.getValue(), result);
    }

    private static void assertUpdateEquals(Gnmi.Update expected, Gnmi.Update result) {
        Assertions.assertEquals(expected.getPath(), result.getPath());
        assertValueMatch(expected.getVal(), result.getVal());
    }

    private static void assertValueMatch(Gnmi.TypedValue first, Gnmi.TypedValue second) {
        switch (first.getValueCase()) {
            case STRING_VAL:
                Assertions.assertEquals(first.getStringVal(), second.getStringVal());
                break;
            case INT_VAL:
                Assertions.assertEquals(first.getIntVal(), second.getIntVal());
                break;
            case UINT_VAL:
                Assertions.assertEquals(first.getUintVal(), second.getUintVal());
                break;
            case BOOL_VAL:
                Assertions.assertEquals(first.getBoolVal(), second.getBoolVal());
                break;
            case JSON_IETF_VAL:
                Assertions.assertTrue(jsonMatch(first.getJsonIetfVal().toStringUtf8(),
                        second.getJsonIetfVal().toStringUtf8()));
                break;
            default:
                Assert.fail("Unexpected value");
        }

    }

    public static boolean jsonMatch(final String first, final String second) {
        final JsonParser parser = new JsonParser();
        final JsonElement jsonA = parser.parse(first);
        final JsonElement jsonB = parser.parse(second);
        return jsonA.equals(jsonB);
    }

}
