/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.tests;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SampleConfigTest {

    @DataProvider
    private Object[] filenames() {
        return new String[]{"sampleConfigSingleNode.json", "sampleConfigCluster.json"};
    }

    @Test(dataProvider = "filenames")
    public void loadTopLevelModelsFromJsonConfig(String filename)
            throws Exception {
        final URL sampleConfigUrl =
                SampleConfigTest.class.getClassLoader().getResource(filename);

        final ControllerConfiguration config =
                ControllerConfigUtils.getConfiguration(Files.newInputStream(Path.of(sampleConfigUrl.toURI())));

        final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        final LightyController lightyController = lightyControllerBuilder
                .from(config)
                .build();
        lightyController.start().get();

        final SchemaContext schemaContext =
                lightyController.getServices().getSchemaContextProvider().getSchemaContext();

        Assert.assertEquals(schemaContext.getModules().size(), 10);

        lightyController.shutdown().get();
    }
}
