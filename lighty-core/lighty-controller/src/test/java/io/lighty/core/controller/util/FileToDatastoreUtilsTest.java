/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.core.controller.impl.util.FileToDatastoreUtils;
import io.lighty.core.controller.impl.util.FileToDatastoreUtils.ImportFileFormat;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.TopLevelContainer;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.container.group.SampleContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FileToDatastoreUtilsTest {

    private static final String INITIAL_CONTAINER_PATH = "/data/container-value-1.json";
    private static final String OVERRIDE_CONTAINER_PATH = "/data/container-value-2.xml";
    private static final String OVERRIDE_VALUE_JSON_PATH = "/data/leaf-value-3.json";
    private static final String OVERRIDE_VALUE_XML_PATH = "/data/leaf-value-4.xml";
    private static final InstanceIdentifier<TopLevelContainer> TOP_LEVEL_CONTAINER_IID
            = InstanceIdentifier.create(TopLevelContainer.class);

    private static final YangInstanceIdentifier ROOT_YII = YangInstanceIdentifier.empty();
    private static final YangInstanceIdentifier INNER_VALUE_YII = YangInstanceIdentifier.create(
            YangInstanceIdentifier.NodeIdentifier.create(TopLevelContainer.QNAME),
            YangInstanceIdentifier.NodeIdentifier.create(SampleContainer.QNAME),
            YangInstanceIdentifier.NodeIdentifier.create($YangModuleInfoImpl.qnameOf("value")));

    private static final long TIMEOUT_MILLIS = 20_000;

    private LightyController lightyController;
    private DataBroker dataBroker;

    @BeforeClass
    public void startUp() throws Exception {
        lightyController = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(
                        Set.of($YangModuleInfoImpl.getInstance())))
                .build();
        assertTrue(lightyController.start().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        dataBroker = lightyController.getServices().getBindingDataBroker();
    }

    @AfterClass
    public void tearDown() throws Exception {
        assertTrue(lightyController.shutdown().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testTopLevelNode() throws Exception {
        // Import first JSON file, new top level container, expecting value = 1
        importFile(INITIAL_CONTAINER_PATH, ROOT_YII, ImportFileFormat.JSON);
        TopLevelContainer topLevelContainer = readDataFromDatastore(TOP_LEVEL_CONTAINER_IID);
        assertEquals(topLevelContainer.getSampleContainer().getValue().intValue(), 1);

        //Import second file, overrides whole top level container, expecting value = 2
        importFile(OVERRIDE_CONTAINER_PATH, ROOT_YII, ImportFileFormat.XML);
        topLevelContainer = readDataFromDatastore(TOP_LEVEL_CONTAINER_IID);
        assertEquals(topLevelContainer.getSampleContainer().getValue().intValue(), 2);

        // Import third file, overrides only inner leaf, expecting value = 3
        importFile(OVERRIDE_VALUE_JSON_PATH, INNER_VALUE_YII, ImportFileFormat.JSON);
        topLevelContainer = readDataFromDatastore(TOP_LEVEL_CONTAINER_IID);
        assertEquals(topLevelContainer.getSampleContainer().getValue().intValue(), 3);

        // Import fourth file, overrides only inner leaf, expecting value = 4
        importFile(OVERRIDE_VALUE_XML_PATH, INNER_VALUE_YII, ImportFileFormat.XML);
        topLevelContainer = readDataFromDatastore(TOP_LEVEL_CONTAINER_IID);
        assertEquals(topLevelContainer.getSampleContainer().getValue().intValue(), 4);
    }


    private <T extends DataObject> T readDataFromDatastore(final InstanceIdentifier<T> instanceIdentifier)
            throws Exception {
        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction()) {
            return readTransaction.read(LogicalDatastoreType.CONFIGURATION, instanceIdentifier)
                    .get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).orElseThrow();
        }
    }

    private void importFile(final String path, final YangInstanceIdentifier yii, final ImportFileFormat format)
            throws Exception {
        FileToDatastoreUtils.importConfigDataFile(FileToDatastoreUtils.class.getResourceAsStream(path),
                yii,
                format,
                lightyController.getServices().getEffectiveModelContextProvider().getEffectiveModelContext(),
                lightyController.getServices().getClusteredDOMDataBroker(),
                true);
    }

}
