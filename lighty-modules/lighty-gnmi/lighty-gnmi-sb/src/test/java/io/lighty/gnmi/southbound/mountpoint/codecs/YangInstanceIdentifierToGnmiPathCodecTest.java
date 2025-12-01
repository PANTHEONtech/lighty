/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.codecs;

import gnmi.Gnmi;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import io.lighty.gnmi.southbound.lightymodule.util.GnmiConfigUtils;
import io.lighty.gnmi.southbound.mountpoint.codecs.testcases.YangInstanceIdentifiertoPathTestCases;
import io.lighty.gnmi.southbound.schema.impl.SchemaException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class YangInstanceIdentifierToGnmiPathCodecTest {

    private static final Path TEST_PATH = Paths.get("src/test/resources/not/unique/model/elements");
    private static final String NAMESPACE_ROOT1 = "tag:lighty.io.2021:yang:test:v1:gnmi:converter:root1";
    private static final String NAMESPACE_ROOT2 = "tag:lighty.io,2021:yang:test:v1:gnmi:converter:root2";
    private static final String ROOT_MODULE_NAME_1 = "root-model-1";
    private static final String ROOT_MODULE_NAME_2 = "root-model-2";
    private static final String ROOT_CONTAINER = "root-container";

    @Test
    public void yangInstanceIdentifierToPathCodecTest()
            throws SchemaException, YangLoadException, ConfigurationException {
        YangInstanceIdentifiertoPathTestCases testCases = new YangInstanceIdentifiertoPathTestCases();
        testCodec(true, testCases);
        testCodec(false, testCases);
    }

    private void testCodec(final boolean shouldApplyModulePrefixToTopElement,
                           final YangInstanceIdentifiertoPathTestCases testCases) {
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

    @Test
    public void yangInstanceIdentifierToPathCodecWithNotUniqueNameForRootElement()
            throws SchemaException, YangLoadException, ConfigurationException {
        //Init YangInstanceIdentifierToPathCodec with test schema context
        final GnmiConfiguration gnmiConfiguration = new GnmiConfiguration();
        gnmiConfiguration.setYangModulesInfo(GnmiConfigUtils.OPENCONFIG_YANG_MODELS);
        Assertions.assertNotNull(gnmiConfiguration.getYangModulesInfo());
        final TestSchemaContextProvider contextProvider = TestSchemaContextProvider.createInstance(TEST_PATH,
                gnmiConfiguration.getYangModulesInfo());

        final YangInstanceIdentifierToPathCodec pathCodec
                = new YangInstanceIdentifierToPathCodec(contextProvider, true);
        // Test parse YIID path and choosing correct module for not unique name for root element
        final YangInstanceIdentifier root1YIID = YangInstanceIdentifier.builder()
                .node(QName.create(NAMESPACE_ROOT1, ROOT_CONTAINER))
                .node(QName.create(NAMESPACE_ROOT1, "data"))
                .build();
        final Gnmi.Path pathRoot1 = pathCodec.apply(root1YIID);
        final Gnmi.PathElem root1Elem = pathRoot1.getElem(0);
        final String[] splitRoot1Elem = root1Elem.getName().split(":");
        Assertions.assertEquals(2, splitRoot1Elem.length);
        Assertions.assertEquals(ROOT_MODULE_NAME_1, splitRoot1Elem[0]);
        Assertions.assertEquals(ROOT_CONTAINER, splitRoot1Elem[1]);

        // Parse YIID path and verify choosing correct module for not unique name in root element with
        // unique last element
        final YangInstanceIdentifier root2YIID = YangInstanceIdentifier.builder()
                .node(QName.create(NAMESPACE_ROOT2, ROOT_CONTAINER))
                .node(QName.create(NAMESPACE_ROOT2, "element"))
                .build();
        final Gnmi.Path pathRoot2 = pathCodec.apply(root2YIID);
        final Gnmi.PathElem root2Elem = pathRoot2.getElem(0);
        final String[] splitRoot2Elem = root2Elem.getName().split(":");
        Assertions.assertEquals(2, splitRoot2Elem.length);
        Assertions.assertEquals(ROOT_MODULE_NAME_2, splitRoot2Elem[0]);
        Assertions.assertEquals(ROOT_CONTAINER, splitRoot2Elem[1]);
    }
}
