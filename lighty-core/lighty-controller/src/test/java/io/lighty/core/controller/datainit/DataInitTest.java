/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.datainit;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class DataInitTest {

    private static final Logger LOG = LoggerFactory.getLogger(DataInitTest.class);

    // value from .xml/.json file
    private static int EXPECTED_DARKNESS_FACTOR = 200;
    private static final String PATH_TO_JSON_INIT_CONFIG = "/DataInitJsonConfig.json";
    private static final String PATH_TO_XML_INIT_CONFIG = "/DataInitXmlConfig.json";
    private static final String PATH_TO_INVALID_PATH_TO_INIT_CONFIG = "/DataInitInvalidInitPathConfig.json";
    private static final String PATH_TO_INVALID_JSON_NODES_INIT_CONFIG = "/DataInitInvalidInitConfigJson.json";
    private static final String PATH_TO_INVALID_XML_NODES_INIT_CONFIG = "/DataInitInvalidInitConfigXml.json";
    private static final InstanceIdentifier<Toaster> NODE_YIID = InstanceIdentifier
            .builder(Toaster.class).build();
    private static final long TIMEOUT_MILLIS = 20_000;

    private LightyController lightyController;

    /*
    1. Give some time to load init data
    2. Registers listener on Toaster node
    3. Check if listener was triggered
    */
    @Test
    public void testInitConfigDataLoadXML() throws Exception {
        InputStream configStream =  this.getClass().getResourceAsStream(PATH_TO_XML_INIT_CONFIG);
        lightyController = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getConfiguration(configStream))
                .build();
        lightyController.start().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        CountDownLatch listenerLatch = new CountDownLatch(1);
        LightyServices services = lightyController.getServices();
        // Should receive notification even when listener is registered after data was changed
        registerToasterListener(services.getBindingDataBroker(), NODE_YIID, listenerLatch);
        listenerLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(listenerLatch.getCount(), 0);
    }

    /*
    1. Give some time to load init data
    2. Registers listener on Toaster node
    3. Check if listener was triggered
    */
    @Test
    public void testInitConfigDataLoadJSON() throws Exception {
        InputStream configStream =  this.getClass().getResourceAsStream(PATH_TO_JSON_INIT_CONFIG);
        lightyController = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getConfiguration(configStream))
                .build();
        lightyController.start().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        CountDownLatch listenerLatch = new CountDownLatch(1);
        LightyServices services = lightyController.getServices();
        // Should receive notification even when listener is registered after data was changed
        registerToasterListener(services.getBindingDataBroker(), NODE_YIID, listenerLatch);
        listenerLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(listenerLatch.getCount(), 0);
    }

    @Test
    public void testInvalidInitFilePath() throws Exception {
        InputStream configStream =  this.getClass().getResourceAsStream(PATH_TO_INVALID_PATH_TO_INIT_CONFIG);
        lightyController = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getConfiguration(configStream))
                .build();
        boolean result = lightyController.start().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(result,false);

    }

    @Test
    public void testInvalidInitConfigFileJSON() throws Exception {
        InputStream configStream =  this.getClass().getResourceAsStream(PATH_TO_INVALID_JSON_NODES_INIT_CONFIG);
        lightyController = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getConfiguration(configStream))
                .build();
        boolean result = lightyController.start().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(result,false);
    }

    @Test
    public void testInvalidInitConfigFileXML() throws Exception {
        InputStream configStream =  this.getClass().getResourceAsStream(PATH_TO_INVALID_XML_NODES_INIT_CONFIG);
        lightyController = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getConfiguration(configStream))
                .build();
        boolean result = lightyController.start().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(result,false);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    @AfterMethod
    public void shutdownLighty() {
        try {
            lightyController.shutdown().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOG.error("Shutdown of lightyController failed", e);
        }
    }

    private ToasterListener registerToasterListener(DataBroker dataBroker,
                                                    InstanceIdentifier<Toaster> instanceIdentifier,
                                                    CountDownLatch listenerLatch) {
        ToasterListener listener = new ToasterListener(listenerLatch, EXPECTED_DARKNESS_FACTOR);
        dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, instanceIdentifier),
                listener);
        return listener;
    }


}
