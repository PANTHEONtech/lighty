/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.common.models.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ArrayNode;
import io.lighty.core.common.models.ModuleId;
import io.lighty.core.common.models.YangModuleUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;

class YangModelUtilsTests {

    private static final String TEST_NAMESPACE = "urn:ietf:params:xml:ns:yang:ietf-inet-types";
    private static final String TEST_NAME = "ietf-inet-types";
    private static final String TEST_REVISION = "2013-07-15";
    private static final Set<YangModuleInfo> YANG_MODELS = Set.of(
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev230126
                .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220
                .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715
                .YangModuleInfoImpl.getInstance()
    );

    static Stream<Arguments> equalsTestData() {
        ModuleId moduleIdx = new ModuleId("Test","namespace","modulex", "2018-04-23");
        ModuleId moduleIdy = new ModuleId("Test", "namespace","modulex", "2018-04-23");
        ModuleId moduleIdz = new ModuleId("namespace","modulex", "2018-04-23");
        ModuleId moduleIdw = new ModuleId("namespace","modulew", "2018-04-23");
        return Stream.of(
                //reflexive
                Arguments.of(moduleIdx, moduleIdx, true),
                //symmetric
                Arguments.of(moduleIdx, moduleIdy, true),
                Arguments.of(moduleIdy, moduleIdx, true),
                //transitive
                Arguments.of(moduleIdx, moduleIdy, true),
                Arguments.of(moduleIdy, moduleIdz, true),
                Arguments.of(moduleIdx, moduleIdz, true),
                //consistent
                Arguments.of(moduleIdx, moduleIdx, true),
                Arguments.of(moduleIdx, moduleIdw, false),
                Arguments.of(moduleIdx, "data", false),
                Arguments.of(moduleIdx, null, false)
            );
    }

    static Stream<Arguments> moduleFilterTestData() {
        return Stream.of(
            Arguments.of(
                new HashSet<>(List.of(
                    new ModuleId("CONTROLLER", "urn:TBD:params:xml:ns:yang:network-topology", "network-topology",
                            "2013-10-21"))),
                new HashSet<>(List.of("network-topology", "ietf-inet-types"))
            ),
            Arguments.of(
                new HashSet<>(List.of(
                    new ModuleId("CONTROLLER", "urn:ietf:params:xml:ns:yang:ietf-yang-types", "ietf-yang-types",
                            "2013-07-15"))),
                new HashSet<>(List.of("ietf-yang-types"))
            ),
            Arguments.of(
                new HashSet<>(List.of(
                    new ModuleId("urn:ietf:params:xml:ns:yang:ietf-inet-types", "ietf-inet-types", "2013-07-15"))),
                new HashSet<>(List.of("ietf-inet-types"))
            )
        );
    }

    static Stream<Arguments> moduleIdStringInits() {
        return Stream.of(
                //valid inits
            Arguments.of(TEST_NAMESPACE, TEST_NAME, TEST_REVISION, true),
            Arguments.of("", TEST_NAME, TEST_REVISION, true),
            Arguments.of(TEST_NAMESPACE, TEST_NAME, null, true),

                //invalid inits
            Arguments.of(TEST_NAMESPACE, TEST_NAME, "", false),
            Arguments.of(null, TEST_NAME, TEST_REVISION, false),
            Arguments.of(TEST_NAMESPACE, null, TEST_REVISION, false),
            Arguments.of(TEST_NAMESPACE, "", TEST_REVISION, false)
        );
    }

    @ParameterizedTest
    @MethodSource("equalsTestData")
    void moduleIdEqualsTest(ModuleId moduleId, Object other, boolean expectedResult) {
        assertEquals(expectedResult, moduleId.equals(other));
        if (other != null) {
            assertTrue(moduleId.hashCode() == other.hashCode() == expectedResult);
        } else {
            assertFalse(expectedResult);
        }
    }

    @ParameterizedTest
    @MethodSource("moduleIdStringInits")
    @SuppressWarnings("checkstyle:illegalCatch")
    void testCreateInvalidModuleIdsFromStrings(String namespace, String name, String revision, boolean expected) {
        try {
            ModuleId testModule = ModuleId.from(namespace, name, revision);
            assertEquals(expected, testModule.getQName().equals(QName.create(namespace, revision, name)) == expected);
        } catch (Exception e) {
            assertFalse(expected);
        }
    }

    /*
     * This test requires test dependency:
     * org.opendaylight.mdsal.model/ietf-topology
     */
    @Test
    void testLoadAllModules() {
        List<String> expectedModuleNames = new ArrayList<>();
        expectedModuleNames.add("network-topology");
        expectedModuleNames.add("ietf-yang-types");
        expectedModuleNames.add("ietf-inet-types");

        Set<YangModuleInfo> allModelsFromClasspath = YangModuleUtils.getAllModelsFromClasspath();
        assertNotNull(allModelsFromClasspath);
        for (String expectedModuleName : expectedModuleNames) {
            long foundModelCount = allModelsFromClasspath.stream()
                    .filter(m -> m.getName().getLocalName().equals(expectedModuleName))
                    .count();
            assertTrue(foundModelCount > 0, expectedModuleName + " not found !");
        }
    }

    /*
     * This test requires test dependency:
     * org.opendaylight.mdsal.model/ietf-topology
     */
    @ParameterizedTest
    @MethodSource("moduleFilterTestData")
    void testLoadFilteredModules(Set<ModuleId> filter, Set<String> expectedModuleNames) {
        Set<YangModuleInfo> filteredModelsFromClasspath = YangModuleUtils.getModelsFromClasspath(filter);
        for (String expectedModuleName : expectedModuleNames) {
            long foundModelCount = filteredModelsFromClasspath.stream()
                    .filter(m -> m.getName().getLocalName().equals(expectedModuleName))
                    .count();
            assertTrue(foundModelCount > 0, expectedModuleName + " not found !");
        }
    }

    /*
     * This test requires test dependencies:
     * org.opendaylight.mdsal.binding.model.iana/iana-if-type
     */
    @Test
    void testGenerateJSONModelSetConfiguration() {
        ArrayNode arrayNode = YangModuleUtils.generateJSONModelSetConfiguration(YANG_MODELS);
        assertNotNull(arrayNode);
        assertEquals(3, arrayNode.size());
    }
}