package io.lighty.core.common.models.tests;

import io.lighty.core.common.models.YangModuleUtils;
import io.lighty.core.common.models.YangModulesTool;
import java.util.Set;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.testng.annotations.Test;

public class YangModulesToolTest {

    @Test
    public void testPrintModelInfo() {
        YangModulesTool.main(new String[]{});
    }

    @Test
    public void testPrintConfiguration() {
        final Set<YangModuleInfo> allModelsFromClasspath = YangModuleUtils.getAllModelsFromClasspath();
        YangModulesTool.printConfiguration(allModelsFromClasspath);
    }
}
