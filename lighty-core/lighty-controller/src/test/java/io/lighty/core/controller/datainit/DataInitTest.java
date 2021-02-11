package io.lighty.core.controller.datainit;

import com.google.common.collect.ImmutableSet;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import java.io.File;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class DataInitTest {
    private static final Set<YangModuleInfo> TOASTER_MODEL = ImmutableSet.of(
            org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.$YangModuleInfoImpl
                    .getInstance());
    private static final Logger LOG = LoggerFactory.getLogger(DataInitTest.class);

    // value from .xml/.json file
    private static int EXPECTED_DARKNESS_FACTOR = 200;
    private static final String PATH_TO_JSON_INIT_FILE = "/initial_config_data.json";
    private static final String PATH_TO_XML_INIT_FILE = "/initial_config_data.xml";
    private static final String PATH_TO_INVALID_JSON_INIT_FILE = "/invalid_initial_config_data.json";
    private static final String PATH_TO_INVALID_XML_INIT_FILE = "/invalid_initial_config_data.xml";
    private static final InstanceIdentifier<Toaster> NODE_YIID = InstanceIdentifier
            .builder(Toaster.class).build();
    private static final long TIMEOUT_MILLIS = 20_000;

    private LightyController lightyController;

    /*
    1. Registers listener on Toaster node
    2. Executes initial config data load from .xml file once everything is loaded
     (services.getLightySystemReadyService().onSystemBootReady())
    3. Give some time to load init data
    4. Check if listener was triggered
    */
    @Test
    public void testInitConfigDataLoadXML() throws Exception {
        URL fileUrl = this.getClass().getResource(PATH_TO_XML_INIT_FILE);
        File jsonFile = new File(fileUrl.getPath());
        lightyController = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(TOASTER_MODEL))
                .withInitialConfigDataFile(jsonFile).build();

        lightyController.start().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        CountDownLatch listenerLatch = new CountDownLatch(1);
        LightyServices services = lightyController.getServices();
        // Should receive notification even when listener is registered after data was changed
        registerToasterListener(services.getBindingDataBroker(), NODE_YIID, listenerLatch);
        listenerLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(listenerLatch.getCount(), 0);
    }

    /*
    1. Registers listener on Toaster node
    2. Executes initial config data load from .xml file once everything is loaded
     (services.getLightySystemReadyService().onSystemBootReady())
    3. Give some time to load init data
    4. Check if listener was triggered
    */
    @Test
    public void testInitConfigDataLoadJSON() throws Exception {
        URL fileUrl = this.getClass().getResource(PATH_TO_JSON_INIT_FILE);
        File jsonFile = new File(fileUrl.getPath());
        lightyController = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(TOASTER_MODEL))
                .withInitialConfigDataFile(jsonFile).build();
        lightyController.start().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        LightyServices services = lightyController.getServices();
        CountDownLatch listenerLatch = new CountDownLatch(1);
        registerToasterListener(services.getBindingDataBroker(), NODE_YIID, listenerLatch);
        // Should receive notification even when listener is registered after data was changed
        listenerLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(listenerLatch.getCount(), 0);

    }

    @Test()
    public void testInvalidInitConfigFileXML() throws Exception {
        URL fileUrl = this.getClass().getResource(PATH_TO_INVALID_XML_INIT_FILE);
        File jsonFile = new File(fileUrl.getPath());
        lightyController = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(TOASTER_MODEL))
                .withInitialConfigDataFile(jsonFile).build();
        boolean result = lightyController.start().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(result,false);
    }

    @Test()
    public void testInvalidInitConfigFileJSON() throws Exception {
        URL fileUrl = this.getClass().getResource(PATH_TO_INVALID_JSON_INIT_FILE);
        File jsonFile = new File(fileUrl.getPath());
        lightyController = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(TOASTER_MODEL))
                .withInitialConfigDataFile(jsonFile).build();
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
