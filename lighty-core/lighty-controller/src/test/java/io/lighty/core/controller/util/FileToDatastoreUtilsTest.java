/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.ChoiceContainer;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SampleList;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SampleListKey;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.TopLevelContainer;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.choice.container.Snack;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.choice.container.snack.SportsArena;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.container.group.SampleContainer;
import org.opendaylight.yang.svc.v1.http.pantheon.tech.ns.test.models.rev180119.YangModuleInfoImpl;
import org.opendaylight.yangtools.binding.ChildOf;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataRoot;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FileToDatastoreUtilsTest {
    private static final String INITIAL_CONTAINER_PATH = "/data/container-value-1.json";
    private static final String CASE_CONTAINER_PATH = "/data/case-container-value.json";
    private static final String OVERRIDE_CONTAINER_PATH = "/data/container-value-2.xml";
    private static final String OVERRIDE_VALUE_JSON_PATH = "/data/leaf-value-3.json";
    private static final String OVERRIDE_VALUE_XML_PATH = "/data/leaf-value-4.xml";
    private static final String MULTIPLE_TOP_JSON_PATH = "/data/multiple-top-element.json";
    private static final String MULTIPLE_TOP_XML_PATH = "/data/multiple-top-element.xml";
    private static final DataObjectIdentifier<TopLevelContainer> TOP_LEVEL_CONTAINER_ID
            = DataObjectIdentifier.builder(TopLevelContainer.class).build();
    private static final DataObjectIdentifier<SampleList> SAMPLE_LIST_ID1_ID
            = DataObjectIdentifier.builder(SampleList.class, new SampleListKey("ID1")).build();
    private static final DataObjectIdentifier<SampleList> SAMPLE_LIST_ID2_ID
            = DataObjectIdentifier.builder(SampleList.class, new SampleListKey("ID2")).build();

    private static final YangInstanceIdentifier ROOT_YII = YangInstanceIdentifier.empty();

    private static final DataObjectIdentifier<ChoiceContainer> CHOICE_CONTAINER_ID
            = DataObjectIdentifier.builder(ChoiceContainer.class).build();

    private static final YangInstanceIdentifier INNER_CASE_YIID = YangInstanceIdentifier.create(
            NodeIdentifier.create(ChoiceContainer.QNAME),
            NodeIdentifier.create(Snack.QNAME));

    private static final YangInstanceIdentifier INNER_VALUE_YII = YangInstanceIdentifier.create(
            NodeIdentifier.create(TopLevelContainer.QNAME),
            NodeIdentifier.create(SampleContainer.QNAME),
            NodeIdentifier.create(YangModuleInfoImpl.qnameOf("value")));

    private static final long TIMEOUT_MILLIS = 60_000;

    private LightyController lightyController;
    private DataBroker dataBroker;

    @BeforeClass
    public void startUp() throws Exception {
        lightyController = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(
                        Set.of(YangModuleInfoImpl.getInstance())))
                .build();
        assertTrue(lightyController.start().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        dataBroker = lightyController.getServices().getBindingDataBroker();
    }

    @AfterClass
    public void tearDown() {
        assertTrue(lightyController.shutdown(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testTopLevelNode() throws Exception {
        // Import inner-case and test choice node
        importFile(CASE_CONTAINER_PATH, INNER_CASE_YIID, ImportFileFormat.JSON);
        ChoiceContainer choiceContainer = readDataFromDatastore(CHOICE_CONTAINER_ID);
        assertEquals(((SportsArena) choiceContainer.getSnack()).getInnerCase().getFoo(), "data");
        assertEquals(((SportsArena) choiceContainer.getSnack()).getInnerCase()
                .getSampleContainer().getValue(), Uint32.ONE);
        assertEquals(((SportsArena) choiceContainer.getSnack()).getInnerCase()
                .getSampleContainer().getName(), "name");

        // Import first JSON file, new top level container, expecting value = 1
        importFile(INITIAL_CONTAINER_PATH, ROOT_YII, ImportFileFormat.JSON);
        TopLevelContainer topLevelContainer = readDataFromDatastore(TOP_LEVEL_CONTAINER_ID);
        assertEquals(topLevelContainer.getSampleContainer().getValue().intValue(), 1);

        //Import second file, overrides whole top level container, expecting value = 2
        importFile(OVERRIDE_CONTAINER_PATH, ROOT_YII, ImportFileFormat.XML);
        topLevelContainer = readDataFromDatastore(TOP_LEVEL_CONTAINER_ID);
        assertEquals(topLevelContainer.getSampleContainer().getValue().intValue(), 2);

        // Import third file, overrides only inner leaf, expecting value = 3
        importFile(OVERRIDE_VALUE_JSON_PATH, INNER_VALUE_YII, ImportFileFormat.JSON);
        topLevelContainer = readDataFromDatastore(TOP_LEVEL_CONTAINER_ID);
        assertEquals(topLevelContainer.getSampleContainer().getValue().intValue(), 3);

        // Import fourth file, overrides only inner leaf, expecting value = 4
        importFile(OVERRIDE_VALUE_XML_PATH, INNER_VALUE_YII, ImportFileFormat.XML);
        topLevelContainer = readDataFromDatastore(TOP_LEVEL_CONTAINER_ID);
        assertEquals(topLevelContainer.getSampleContainer().getValue().intValue(), 4);
    }

    @Test
    public void testMultipleTopElement() throws Exception {
        // Import multiple top element in JSON file, Expected value 5, ID1 value 1, ID2 value 2
        importFile(MULTIPLE_TOP_JSON_PATH, ROOT_YII, ImportFileFormat.JSON);
        TopLevelContainer topLevelContainer = readDataFromDatastore(TOP_LEVEL_CONTAINER_ID);
        assertEquals(topLevelContainer.getSampleContainer().getValue().intValue(), 5);

        SampleList sampleListId1 = readDataFromDatastore(SAMPLE_LIST_ID1_ID);
        assertEquals(sampleListId1.getValue().intValue(), 1);
        SampleList sampleListId2 = readDataFromDatastore(SAMPLE_LIST_ID2_ID);
        assertEquals(sampleListId2.getValue().intValue(), 2);

        // Import multiple top element in XML file, Expected value 6, ID1 value 3, ID2 value 4
        importFile(MULTIPLE_TOP_XML_PATH, ROOT_YII, ImportFileFormat.XML);
        topLevelContainer = readDataFromDatastore(TOP_LEVEL_CONTAINER_ID);
        assertEquals(topLevelContainer.getSampleContainer().getValue().intValue(), 6);

        sampleListId1 = readDataFromDatastore(SAMPLE_LIST_ID1_ID);
        assertEquals(sampleListId1.getValue().intValue(), 3);
        sampleListId2 = readDataFromDatastore(SAMPLE_LIST_ID2_ID);
        assertEquals(sampleListId2.getValue().intValue(), 4);
    }

    private <T extends ChildOf<? extends DataRoot>> T readDataFromDatastore(
            final DataObjectIdentifier<T> identifier) throws Exception {
        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction()) {
            return readTransaction.read(LogicalDatastoreType.CONFIGURATION, identifier)
                    .get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).orElseThrow();
        }
    }

    private void importFile(final String path, final YangInstanceIdentifier yii, final ImportFileFormat format)
            throws Exception {
        FileToDatastoreUtils.importConfigDataFile(FileToDatastoreUtils.class.getResourceAsStream(path),
                yii,
                format,
                lightyController.getServices().getDOMSchemaService().getGlobalContext(),
                lightyController.getServices().getClusteredDOMDataBroker(),
                true);
    }

}
