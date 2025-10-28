/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.codecs;

import com.google.gson.Gson;
import gnmi.Gnmi;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.gnmi.southbound.mountpoint.codecs.testcases.GetResponseToNormalizedNodeTestCases;
import io.lighty.gnmi.southbound.schema.impl.SchemaException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class GetResponseToNormalizedNodeCodecTest {

    private static GetResponseToNormalizedNodeTestCases testCases;
    private static GetResponseToNormalizedNodeCodec codec;

    @BeforeAll
    public static void init() throws YangLoadException, IOException, SchemaException, ConfigurationException {
        testCases = new GetResponseToNormalizedNodeTestCases();
        final Gson gson = new Gson();
        codec = new GetResponseToNormalizedNodeCodec(testCases.getSchemaContextProvider(), gson);
    }

    @Test
    public void rootElementCase() throws GnmiCodecException {
        final Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>, NormalizedNode> prepared =
                testCases.rootCase();
        final Optional<NormalizedNode> result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
    }

    @Test
    public void topLevelElementTest() throws GnmiCodecException {
        // Test if json response is at the same level as identifier
        Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>, NormalizedNode> prepared =
                testCases.topElementTestCase(false);
        Optional<NormalizedNode> result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
        // Test if json response is one level deeper as identifier
        prepared = testCases.topElementTestCase(true);
        result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
    }

    @Test
    public void containerTest() throws GnmiCodecException {
        // Test container if response is at the same level as requested
        Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>, NormalizedNode> prepared =
                testCases.containerTestCase(false);
        Optional<NormalizedNode> result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
        // Test container if response is one level deeper than requested
        prepared = testCases.containerTestCase(true);
        result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
        // ------Augmented cases:----------
        prepared = testCases.augmentedContainerCase(false);
        result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
        // Test container if response is one level deeper than requested
        prepared = testCases.augmentedContainerCase(true);
        result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
    }

    @Test
    public void listEntryTest() throws GnmiCodecException {
        // Test list entry if response is at the same level as requested
        Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>, NormalizedNode> prepared =
                testCases.listEntryTestCase(false);
        Optional<NormalizedNode> result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
        // Test list entry if response is one level deeper than requested
        prepared = testCases.listEntryTestCase(true);
        result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
    }

    /*
        Tests codec on leaf nodes. GetResponse contains json_ietf_val value, which may/may not be rooted one level
         deeper than requested.
     */
    @Test
    public void leafJsonTest() throws GnmiCodecException {
        // ------Number cases:----------
        // Test leaf value if response is json in format "{leaf:value}"
        Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>, NormalizedNode> prepared =
                testCases.leafNumberTestCase(true, false);
        Optional<NormalizedNode> result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
        // Test leaf value if response is json in format "value"
        prepared = testCases.leafNumberTestCase(true, true);
        result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);

        // ------String cases:----------
        // Test leaf value if response is json in format "{leaf:value}"
        prepared = testCases.leafStringTestCase(true, false);
        result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
        // Test leaf value if response is json in format "value"
        prepared = testCases.leafStringTestCase(true, true);
        assertEqualsCodecResult(prepared.getValue(), result);

        // ------Boolean cases:----------
        // Test leaf value if response is json in format "{leaf:value}"
        prepared = testCases.leafBooleanTestCase(true, false);
        result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
        // Test leaf value if response is json in format "value"
        prepared = testCases.leafBooleanTestCase(true, true);
        result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);

        // ------Augmented cases:----------
        // Test leaf value if response is json in format "{leaf:value}"
        prepared = testCases.leafAugmentedTestCase(true, false);
        result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
        prepared = testCases.leafAugmentedTestCase(true, true);
        result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
    }

    /*
        Tests codec on leaf nodes. GetResponse contains specific type (e.g. for boolean bool_val is set).
     */
    @Test
    public void leafNonJsonTest() throws GnmiCodecException {
        // ------Number case:----------
        Map.Entry<ImmutablePair<YangInstanceIdentifier, Gnmi.GetResponse>, NormalizedNode> prepared =
                testCases.leafNumberTestCase(false, true);
        Optional<NormalizedNode> result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);

        // ------String case:----------
        // Test leaf value if response is json in format "{leaf:value}"
        prepared = testCases.leafStringTestCase(false, true);
        result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);

        // ------Boolean case:----------
        // Test leaf value if response is json in format "{leaf:value}"
        prepared = testCases.leafBooleanTestCase(false, true);
        result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);

        // ------Augmented case:----------
        prepared = testCases.leafAugmentedTestCase(false, true);
        result = codec.apply(prepared.getKey().right, prepared.getKey().left);
        assertEqualsCodecResult(prepared.getValue(), result);
    }

    private static void assertEqualsCodecResult(final NormalizedNode expected,
                                                final Optional<NormalizedNode> result) {
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(expected, result.get());
    }

}
