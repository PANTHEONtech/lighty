/*
 * Copyright (c) 2018-2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

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
