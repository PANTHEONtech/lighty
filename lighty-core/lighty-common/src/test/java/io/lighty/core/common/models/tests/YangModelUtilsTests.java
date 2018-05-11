/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.common.models.tests;

import io.lighty.core.common.models.ModuleId;
import io.lighty.core.common.models.YangModuleUtils;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class YangModelUtilsTests {

    @DataProvider(name = "equalsTestData")
    public static Object[][] gatEqualsTestData() {
        ModuleId moduleIdx = new ModuleId("namespace","modulex", "2018-04-23");
        ModuleId moduleIdy = new ModuleId("namespace","modulex", "2018-04-23");
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
                        new HashSet<>(Arrays.asList(new ModuleId[] { new ModuleId("urn:TBD:params:xml:ns:yang:network-topology", "network-topology", "2013-10-21") })),
                        new HashSet<>(Arrays.asList(new String[] { "network-topology", "ietf-inet-types"}))
                },
                {
                        new HashSet<>(Arrays.asList(new ModuleId[] { new ModuleId("urn:TBD:params:xml:ns:yang:network-topology", "ietf-yang-types", "2013-07-15") })),
                        new HashSet<>(Arrays.asList(new String[] { "ietf-yang-types" }))
                },
                {
                        new HashSet<>(Arrays.asList(new ModuleId[] { new ModuleId("urn:TBD:params:xml:ns:yang:network-topology", "ietf-inet-types", "2013-07-15") })),
                        new HashSet<>(Arrays.asList(new String[] { "ietf-inet-types" }))
                },

        };
    }

    @Test(dataProvider = "equalsTestData")
    public void moduleIdEqualsTest(ModuleId moduleId, Object other, boolean expectedResult) {
        Assert.assertTrue(moduleId.equals(other) == expectedResult);
        if (other != null) {
            Assert.assertTrue((moduleId.hashCode() == other.hashCode()) == expectedResult);
        } else {
            Assert.assertFalse(expectedResult);
        }
    }

    /**
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
            long foundModelCount = allModelsFromClasspath.stream().filter(m -> m.getName().equals(expectedModuleName)).count();
            Assert.assertTrue(foundModelCount > 0, expectedModuleName + " not found !");
        }
    }

    /**
     * This test requires test dependency:
     * org.opendaylight.mdsal.model/ietf-topology
     */
    @Test(dataProvider = "moduleFilterTestData")
    public void testLoadFilteredModules(Set<ModuleId> filter, Set<String> expectedModuleNames) {
        Set<YangModuleInfo> filteredModelsFromClasspath = YangModuleUtils.getModelsFromClasspath(filter);
        for (String expectedModuleName : expectedModuleNames) {
            long foundModelCount = filteredModelsFromClasspath.stream().filter(m -> m.getName().equals(expectedModuleName)).count();
            Assert.assertTrue(foundModelCount > 0, expectedModuleName + " not found !");
        }
    }

}