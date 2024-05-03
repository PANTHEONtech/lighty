/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;

public class SampleConfigTest {

    private static final long TIME_OUT = 30;

    @Test
    public void loadTopLevelSingleNodeModelsFromJsonConfig() throws Exception {
        final LightyController lightyController = getLightyController("sampleConfigSingleNode.json");
        assertTrue(lightyController.start().get(TIME_OUT, TimeUnit.SECONDS));

        final int loadedModulesSize = lightyController.getServices().getDOMSchemaService().getGlobalContext()
            .getModules().size();
        assertTrue(lightyController.shutdown(TIME_OUT, TimeUnit.SECONDS));

        assertEquals(loadedModulesSize, 17);
    }

    @Test
    public void loadTopLevelClusterModelsFromJsonConfig() throws Exception {
        final LightyController lightyController = getLightyController("sampleConfigCluster.json");
        assertTrue(lightyController.start().get(TIME_OUT, TimeUnit.SECONDS));

        final int loadedModulesSize = lightyController.getServices().getDOMSchemaService().getGlobalContext()
            .getModules().size();
        assertTrue(lightyController.shutdown(TIME_OUT, TimeUnit.SECONDS));

        assertEquals(loadedModulesSize, 17);
    }

    private LightyController getLightyController(final String resource) throws Exception {
        final URL sampleConfigUrl = SampleConfigTest.class.getClassLoader().getResource(resource);
        final ControllerConfiguration config = ControllerConfigUtils.getConfiguration(
                Files.newInputStream(Path.of(sampleConfigUrl.toURI())));

        return new LightyControllerBuilder().from(config).build();
    }
}
