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

import io.lighty.codecs.util.exception.DeserializationException;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.core.controller.impl.util.FileToDatastoreUtils;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.TopLevelContainer;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.container.group.SampleContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.testng.annotations.Test;

public class FileToDatastoreUtilsTest {

    private static final String INITIAL_CONTAINER_PATH = "/data/container-value-1.json";
    private static final String OVERRIDE_CONTAINER_PATH = "/data/container-value-2.xml";
    private static final String OVERRIDE_VALUE_JSON_PATH = "/data/leaf-value-3.json";
    private static final String OVERRIDE_VALUE_XML_PATH = "/data/leaf-value-4.xml";

    private static final YangInstanceIdentifier ROOT_YII = YangInstanceIdentifier.empty();
    private static final YangInstanceIdentifier INNER_VALUE_YII = YangInstanceIdentifier.create(
            YangInstanceIdentifier.NodeIdentifier.create(TopLevelContainer.QNAME),
            YangInstanceIdentifier.NodeIdentifier.create(SampleContainer.QNAME),
            YangInstanceIdentifier.NodeIdentifier.create($YangModuleInfoImpl.qnameOf("value")));

    private static final long TIMEOUT_MILLIS = 20_000;

    private LightyController lightyController;

    @Test
    public void testTopLevelNode() throws Exception {
        lightyController = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(
                        Set.of($YangModuleInfoImpl.getInstance())))
                .build();
        assertTrue(lightyController.start().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));

        // Import first JSON file, new top level container, expecting value = 1
        importFile(lightyController.getServices(), INITIAL_CONTAINER_PATH, ROOT_YII,
                FileToDatastoreUtils.ImportFileFormat.JSON);
        //Retrieve data from datastore
        TopLevelContainer topLevelContainer = readDataFromDatastore(
                TopLevelContainer.class, lightyController.getServices().getBindingDataBroker());
        assertEquals(topLevelContainer.getSampleContainer().getValue().intValue(), 1);


        //Import second file, overrides whole top level container, expecting value = 2
        importFile(lightyController.getServices(), OVERRIDE_CONTAINER_PATH, ROOT_YII,
                FileToDatastoreUtils.ImportFileFormat.XML);
        //Retrieve data from datastore
        topLevelContainer = readDataFromDatastore(
                TopLevelContainer.class, lightyController.getServices().getBindingDataBroker());
        assertEquals(topLevelContainer.getSampleContainer().getValue().intValue(), 2);

        // Import third file, overrides only inner leaf, expecting value = 3
        importFile(lightyController.getServices(), OVERRIDE_VALUE_JSON_PATH,
                INNER_VALUE_YII, FileToDatastoreUtils.ImportFileFormat.JSON);
        //Retrieve data from datastore
        topLevelContainer = readDataFromDatastore(
                TopLevelContainer.class, lightyController.getServices().getBindingDataBroker());
        assertEquals(topLevelContainer.getSampleContainer().getValue().intValue(), 3);

        // Import fourth file, overrides only inner leaf, expecting value = 4
        importFile(lightyController.getServices(), OVERRIDE_VALUE_XML_PATH,
                INNER_VALUE_YII, FileToDatastoreUtils.ImportFileFormat.XML);
        //Retrieve data from datastore
        topLevelContainer = readDataFromDatastore(
                TopLevelContainer.class, lightyController.getServices().getBindingDataBroker());
        assertEquals(topLevelContainer.getSampleContainer().getValue().intValue(), 4);

        assertTrue(lightyController.shutdown().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
    }


    private static <T extends DataObject> T readDataFromDatastore(final Class<T> clazz, final DataBroker dataBroker)
            throws InterruptedException, ExecutionException, TimeoutException {
        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction()) {
            return readTransaction.read(LogicalDatastoreType.CONFIGURATION,
                    InstanceIdentifier.create(clazz))
                    .get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).orElseThrow();
        }
    }

    private static void importFile(final LightyServices services, final String path, final YangInstanceIdentifier yii,
            final FileToDatastoreUtils.ImportFileFormat format)
            throws InterruptedException, ExecutionException, DeserializationException, TimeoutException, IOException {
        FileToDatastoreUtils.importConfigDataFile(
                FileToDatastoreUtils.class.getResourceAsStream(path),
                yii,
                format,
                services.getEffectiveModelContextProvider().getEffectiveModelContext(),
                services.getClusteredDOMDataBroker(),
                true);
    }


}
