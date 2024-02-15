/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.common.models.tests;

import com.fasterxml.jackson.databind.node.ArrayNode;
import io.lighty.core.common.models.ModuleId;
import io.lighty.core.common.models.YangModuleUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class YangModelUtilsTests {

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

    @DataProvider(name = "equalsTestData")
    public static Object[][] gatEqualsTestData() {
        ModuleId moduleIdx = new ModuleId("Test","namespace","modulex", "2018-04-23");
        ModuleId moduleIdy = new ModuleId("Test", "namespace","modulex", "2018-04-23");
        ModuleId moduleIdz = new ModuleId("namespace","modulex", "2018-04-23");
        ModuleId moduleIdw = new ModuleId("namespace","modulew", "2018-04-23");
        return new Object[][] {
                //reflexive
                {moduleIdx, moduleIdx, true},

                //symmetric
                {moduleIdx, moduleIdy, true},
                {moduleIdy, moduleIdx, true},

                //transitive
                {moduleIdx, moduleIdy, true},
                {moduleIdy, moduleIdz, true},
                {moduleIdx, moduleIdz, true},

                //consistent
                {moduleIdx, moduleIdx, true},
                {moduleIdx, moduleIdx, true},
                {moduleIdx, moduleIdw, false},
                {moduleIdx, moduleIdw, false},
                {moduleIdx, "data", false},
                {moduleIdx, "data", false},
                {moduleIdx, null, false},
                {moduleIdx, null, false},
        };
    }

    @DataProvider(name = "moduleFilterTestData")
    public static Object[][] getModuleFilterTestData() {
        return new Object[][] {
            {
                new HashSet<>(Arrays.asList(
                    new ModuleId("CONTROLLER","urn:TBD:params:xml:ns:yang:network-topology", "network-topology",
                            "2013-10-21"))),
                new HashSet<>(Arrays.asList("network-topology", "ietf-inet-types"))
            },
            {
                new HashSet<>(Arrays.asList(
                    new ModuleId("CONTROLLER", "urn:ietf:params:xml:ns:yang:ietf-yang-types", "ietf-yang-types",
                            "2013-07-15"))),
                new HashSet<>(Arrays.asList("ietf-yang-types"))
            },
            {
                new HashSet<>(Arrays.asList(
                    new ModuleId("urn:ietf:params:xml:ns:yang:ietf-inet-types", "ietf-inet-types", "2013-07-15"))),
                new HashSet<>(Arrays.asList("ietf-inet-types"))
            },
        };
    }

    @DataProvider(name = "moduleIdStringInits")
    public static Object[][] gatModuleIdStringInits() {
        return new Object[][] {
                //valid inits
                {TEST_NAMESPACE, TEST_NAME, TEST_REVISION, true},
                {"", TEST_NAME, TEST_REVISION, true},

                //invalid inits
                {TEST_NAMESPACE, TEST_NAME, "", false},
                {TEST_NAMESPACE, TEST_NAME, null, false},
                {null, TEST_NAME, TEST_REVISION, false},
                {TEST_NAMESPACE, null, TEST_REVISION, false},
                {TEST_NAMESPACE, "", TEST_REVISION, false},
        };
    }

    @Test(dataProvider = "equalsTestData")
    public void moduleIdEqualsTest(final ModuleId moduleId, final Object other, final boolean expectedResult) {
        Assert.assertTrue(moduleId.equals(other) == expectedResult);
        if (other != null) {
            Assert.assertTrue(moduleId.hashCode() == other.hashCode() == expectedResult);
        } else {
            Assert.assertFalse(expectedResult);
        }
    }

    @Test(dataProvider = "moduleIdStringInits")
    @SuppressWarnings("checkstyle:illegalCatch")
    public void testCreateInvalidModuleIdsFromStrings(final String namespace, final String name, final String revision,
            final boolean expected) {
        try {
            ModuleId testModule = ModuleId.from(namespace, name, revision);
            Assert.assertTrue(testModule.getQName().equals(QName.create(namespace, revision, name)) == expected);
        } catch (Exception e) {
            Assert.assertTrue(expected == false);
        }
    }

    /*
     * This test requires test dependency:
     * org.opendaylight.mdsal.model/ietf-topology
     */
    @Test
    public void testLoadAllModules() {
        List<String> expectedModuleNames = new ArrayList<>();
        expectedModuleNames.add("network-topology");
        expectedModuleNames.add("ietf-yang-types");
        expectedModuleNames.add("ietf-inet-types");

        Set<YangModuleInfo> allModelsFromClasspath = YangModuleUtils.getAllModelsFromClasspath();
        Assert.assertNotNull(allModelsFromClasspath);
        for (String expectedModuleName : expectedModuleNames) {
            long foundModelCount = allModelsFromClasspath.stream()
                    .filter(m -> m.getName().getLocalName().equals(expectedModuleName))
                    .count();
            Assert.assertTrue(foundModelCount > 0, expectedModuleName + " not found !");
        }
    }

    /*
     * This test requires test dependency:
     * org.opendaylight.mdsal.model/ietf-topology
     */
    @Test(dataProvider = "moduleFilterTestData")
    public void testLoadFilteredModules(final Set<ModuleId> filter, final Set<String> expectedModuleNames) {
        Set<YangModuleInfo> filteredModelsFromClasspath = YangModuleUtils.getModelsFromClasspath(filter);
        for (String expectedModuleName : expectedModuleNames) {
            long foundModelCount = filteredModelsFromClasspath.stream()
                    .filter(m -> m.getName().getLocalName().equals(expectedModuleName))
                    .count();
            Assert.assertTrue(foundModelCount > 0, expectedModuleName + " not found !");
        }
    }

    /*
     * This test requires test dependencies:
     * org.opendaylight.mdsal.binding.model.iana/iana-if-type
     */
    @Test
    public void testGenerateJSONModelSetConfiguration() {
        ArrayNode arrayNode = YangModuleUtils.generateJSONModelSetConfiguration(YANG_MODELS);
        Assert.assertNotNull(arrayNode);
        Assert.assertTrue(arrayNode.size() == 3);
    }
}
