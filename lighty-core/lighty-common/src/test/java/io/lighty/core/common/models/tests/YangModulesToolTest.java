package io.lighty.core.common.models.tests;

import io.lighty.core.common.models.YangModuleUtils;
import io.lighty.core.common.models.YangModulesTool;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Set;

public class YangModulesToolTest {

    @Test
    public void testPrintModelInfo() {
        try {
            YangModulesTool.main(new String[]{});
        } catch (Exception e) {
            Assert.fail("YangModulesTool Main failed.", e);
        }
    }

    @Test
    public void testPrintConfiguration() {
        final Set<YangModuleInfo> allModelsFromClasspath = YangModuleUtils.getAllModelsFromClasspath();
        try {
            YangModulesTool.printConfiguration(allModelsFromClasspath);
        } catch (Exception e) {
            Assert.fail("YangModulesTool printConfiguration failed.", e);
        }
    }
}
