/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.codecs;

import gnmi.Gnmi;
import io.lighty.gnmi.southbound.mountpoint.codecs.testcases.YangInstanceIdentifiertoPathTestCases;
import io.lighty.gnmi.southbound.schema.impl.SchemaException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class YangInstanceIdentifierToGnmiPathCodecTest {

    private static YangInstanceIdentifiertoPathTestCases testCases;

    @BeforeAll
    public static void init() throws YangLoadException, SchemaException {
        testCases = new YangInstanceIdentifiertoPathTestCases();
    }

    @Test
    public void yangInstanceIdentifierToPathCodecTest() {
        testCodec(true);
        testCodec(false);
    }

    private void testCodec(final boolean shouldApplyModulePrefixToTopElement) {
        final YangInstanceIdentifierToPathCodec codec = new YangInstanceIdentifierToPathCodec(
                testCases.getSchemaContextProvider(), shouldApplyModulePrefixToTopElement);
        //Test root element
        Map.Entry<YangInstanceIdentifier, Gnmi.Path> identifierToPathExpected =
                testCases.rootElementCase();
        Gnmi.Path result = codec.apply(identifierToPathExpected.getKey());
        Assertions.assertEquals(identifierToPathExpected.getValue(), result);
        // Test whole top element
        identifierToPathExpected = testCases.topElementTestCase(shouldApplyModulePrefixToTopElement);
        result = codec.apply(identifierToPathExpected.getKey());
        Assertions.assertEquals(identifierToPathExpected.getValue(), result);
        // Test list entry
        identifierToPathExpected = testCases.listEntryTestCase(shouldApplyModulePrefixToTopElement);
        result = codec.apply(identifierToPathExpected.getKey());
        Assertions.assertEquals(identifierToPathExpected.getValue(), result);
        // Test leaf
        identifierToPathExpected = testCases.leafTestCase(shouldApplyModulePrefixToTopElement);
        result = codec.apply(identifierToPathExpected.getKey());
        Assertions.assertEquals(identifierToPathExpected.getValue(), result);
        // Test augmented
        identifierToPathExpected = testCases.augmentedTestCase(shouldApplyModulePrefixToTopElement);
        result = codec.apply(identifierToPathExpected.getKey());
        Assertions.assertEquals(identifierToPathExpected.getValue(), result);
    }

}
