/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.test.gnmi;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.ByteString;
import gnmi.Gnmi;
import io.lighty.modules.gnmi.connector.configuration.SessionConfiguration;
import io.lighty.modules.gnmi.connector.session.api.SessionManager;
import io.lighty.modules.gnmi.connector.session.api.SessionProvider;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDeviceBuilder;
import io.lighty.modules.gnmi.test.utils.TestUtils;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulatorCrudTest {

    private static final Logger LOG = LoggerFactory.getLogger(SimulatorCrudTest.class);

    private static final int TARGET_PORT = 10161;
    private static final String TARGET_HOST = "127.0.0.1";
    private static final String INITIAL_DATA_PATH = "src/test/resources/json/initData";
    private static final String TEST_SCHEMA_PATH = "src/test/resources/simulator_models";
    private static final String INTERFACES_PREFIX = "openconfig-interfaces";
    private static final String OPENCONFIG_INTERFACES = INTERFACES_PREFIX + ":" + "interfaces";
    private static final String ETHRERNET_PREFIX = "openconfig-if-ethernet";
    private static final String OPENCONFIG_INTERFACE = "interface";
    private static final String OPENCONFIG_CONFIG = "config";
    private static final int UPDATE_MTU_VAL = 500;

    private static SessionProvider sessionProvider;
    private static SimulatedGnmiDevice target;


    @Before
    public void setUp() throws NoSuchAlgorithmException, CertificateException, InvalidKeySpecException, IOException,
            URISyntaxException {

        target = new SimulatedGnmiDeviceBuilder().setHost(TARGET_HOST).setPort(TARGET_PORT)
                .setInitialConfigDataPath(INITIAL_DATA_PATH + "/config.json")
                .setInitialStateDataPath(INITIAL_DATA_PATH + "/state.json")
                .setYangsPath(TEST_SCHEMA_PATH)
                .build();
        target.start();
        final SessionManager sessionManager = TestUtils.createSessionManagerWithCerts();
        final InetSocketAddress targetAddress = new InetSocketAddress(TARGET_HOST, TARGET_PORT);
        sessionProvider = sessionManager.createSession(
                new SessionConfiguration(targetAddress, false));
    }

    @After
    public void after() throws Exception {
        sessionProvider.close();
        target.stop();
    }

    @Test
    public void getDataWithAugmentationTest() throws ExecutionException, InterruptedException {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "br0")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("ethernet")
                        .build())
                .build();
        final Gnmi.GetRequest getRequest = Gnmi.GetRequest.newBuilder()
                .addPath(path)
                .setEncoding(Gnmi.Encoding.JSON_IETF)
                .build();

        LOG.info("Sending get request:\n{}", getRequest);
        final Gnmi.GetResponse getResponse = sessionProvider.getGnmiSession().get(getRequest).get();
        LOG.info("Received get response:\n{}", getResponse);
        Assert.assertEquals(1, getResponse.getNotificationCount());
        Assert.assertEquals(0, getResponse.getNotification(0).getDeleteCount());
        Assert.assertEquals(1, getResponse.getNotification(0).getUpdateCount());
        Assert.assertTrue(TestUtils.jsonMatch(getEthernetExpectedResponse(),
                getResponse.getNotification(0).getUpdate(0).getVal().getJsonIetfVal().toStringUtf8()));
    }

    @Test
    public void crudSimpleValueTest() throws ExecutionException, InterruptedException, IOException {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("openconfig-interfaces:interfaces")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "eth3")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("mtu")
                        .build())
                .build();

        final Gnmi.GetRequest getRequest = Gnmi.GetRequest.newBuilder()
                .addPath(path)
                .setEncoding(Gnmi.Encoding.JSON_IETF)
                .build();


        // Get mtu, should be 1500 from initial simulator data, load it from that initial config file
        LOG.info("Sending get request:\n{}", getRequest);
        Gnmi.GetResponse getResponse = sessionProvider.getGnmiSession().get(getRequest).get();
        LOG.info("Received get response:\n{}", getResponse);

        final String originalInterfacesJson = TestUtils
                .readFile(INITIAL_DATA_PATH + "/config.json");
        JsonElement jsonElement = new JsonParser().parse(originalInterfacesJson).getAsJsonObject()
                .get(OPENCONFIG_INTERFACES);
        // Get mtu from config file
        int expectedOriginalMtu = jsonElement.getAsJsonObject().getAsJsonArray(OPENCONFIG_INTERFACE)
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("config")
                .getAsJsonPrimitive("mtu").getAsInt();
        // construct simple json
        String expectedOriginalMtuJson = "{\"mtu\": " + expectedOriginalMtu + "}";

        Assert.assertEquals(1, getResponse.getNotificationCount());
        Assert.assertEquals(0, getResponse.getNotification(0).getDeleteCount());
        Assert.assertEquals(1, getResponse.getNotification(0).getUpdateCount());
        Assert.assertTrue(TestUtils.jsonMatch(expectedOriginalMtuJson,
                getResponse.getNotification(0).getUpdate(0).getVal().getJsonIetfVal().toStringUtf8()));
        // Set mtu to UPDATE_MTU_VAL
        Gnmi.Update update = Gnmi.Update.newBuilder()
                .setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                        .setIntVal(UPDATE_MTU_VAL)
                        .build())
                .build();
        Gnmi.SetRequest setRequest = Gnmi.SetRequest.newBuilder()
                .addUpdate(update)
                .build();

        LOG.info("Sending set request:\n{}", setRequest);
        Gnmi.SetResponse setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        LOG.info("Received set response:\n{}", setResponse);
        Assert.assertEquals(1, setResponse.getResponseCount());
        Assert.assertEquals(Gnmi.UpdateResult.Operation.UPDATE, setResponse.getResponse(0).getOp());

        // Get mtu, should be UPDATE_MTU_VAL
        LOG.info("Sending get request:\n{}", getRequest);
        getResponse = sessionProvider.getGnmiSession().get(getRequest).get();
        LOG.info("Received get response:\n{}", getResponse);

        String expectedChangedMtuJson = "{\"mtu\": " + UPDATE_MTU_VAL + "}";
        Assert.assertEquals(1, getResponse.getNotificationCount());
        Assert.assertEquals(0, getResponse.getNotification(0).getDeleteCount());
        Assert.assertEquals(1, getResponse.getNotification(0).getUpdateCount());
        Assert.assertTrue(TestUtils.jsonMatch(expectedChangedMtuJson,
                getResponse.getNotification(0).getUpdate(0).getVal().getJsonIetfVal().toStringUtf8()));

        // Delete mtu
        setRequest = Gnmi.SetRequest.newBuilder()
                .addDelete(path)
                .build();

        LOG.info("Sending delete request:\n{}", setRequest);
        setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        LOG.info("Received delete response:\n{}", setResponse);
        Assert.assertEquals(1, setResponse.getResponseCount());
        Assert.assertEquals(Gnmi.UpdateResult.Operation.DELETE, setResponse.getResponse(0).getOp());

        // Get mtu, should throw exception
        LOG.info("Sending get request:\n{}", getRequest);

        Assert.assertThrows(ExecutionException.class, () -> sessionProvider.getGnmiSession().get(getRequest).get());

    }

    @Test
    public void crudComplexValueTest() throws ExecutionException, InterruptedException, IOException {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces")
                        .build())
                .build();
        final Gnmi.GetRequest getRequest = Gnmi.GetRequest.newBuilder()
                .addPath(path)
                .setEncoding(Gnmi.Encoding.JSON_IETF)
                .setType(Gnmi.GetRequest.DataType.CONFIG)
                .build();

        // Get interfaces
        LOG.info("Sending get request:\n{}", getRequest);
        Gnmi.GetResponse getResponse = sessionProvider.getGnmiSession().get(getRequest).get();
        LOG.info("Received get response:\n{}", getResponse);
        String responseJson = getResponse.getNotification(0)
                .getUpdate(0)
                .getVal()
                .getJsonIetfVal()
                .toStringUtf8();

        // Should get initial interfaces data, NOT wrapped in top level element: openconfig-interfaces:interfaces
        final String originalInterfacesJson = TestUtils
                .readFile(INITIAL_DATA_PATH + "/config.json");
        final JsonObject jsonElement = new JsonParser().parse(originalInterfacesJson).getAsJsonObject();
        Assert.assertTrue(TestUtils.jsonMatch(responseJson, jsonElement.toString()));

        // Set MTU of 0 index interface to 1499
        jsonElement.getAsJsonObject(OPENCONFIG_INTERFACES).getAsJsonArray(OPENCONFIG_INTERFACE)
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("config")
                .addProperty("mtu", UPDATE_MTU_VAL);
        // Set loopback-mode of 1 index interface to true
        jsonElement.getAsJsonObject(OPENCONFIG_INTERFACES).getAsJsonArray(OPENCONFIG_INTERFACE)
                .get(1)
                .getAsJsonObject()
                .getAsJsonObject("config")
                .addProperty("loopback-mode", true);
        // Send Set request
        final Gnmi.Update update = Gnmi.Update.newBuilder()
                .setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                        .setJsonIetfVal(ByteString.copyFromUtf8(jsonElement.getAsJsonObject(OPENCONFIG_INTERFACES)
                                .toString()))
                        .build())
                .build();
        Gnmi.SetRequest setRequest = Gnmi.SetRequest.newBuilder()
                .addUpdate(update)
                .build();
        LOG.info("Sending set request:\n{}", setRequest);

        Gnmi.SetResponse setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        LOG.info("Received set response:\n{}", setResponse);

        // Get interfaces
        LOG.info("Sending get request:\n{}", getRequest);
        getResponse = sessionProvider.getGnmiSession().get(getRequest).get();
        LOG.info("Received get response:\n{}", getResponse);
        responseJson = getResponse.getNotification(0)
                .getUpdate(0)
                .getVal()
                .getJsonIetfVal()
                .toStringUtf8();

        Assert.assertTrue(TestUtils.jsonMatch(responseJson, jsonElement.toString()));
        // Delete interfaces
        setRequest = Gnmi.SetRequest.newBuilder()
                .addDelete(path)
                .build();

        LOG.info("Sending delete request:\n{}", setRequest);
        setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        LOG.info("Received delete response:\n{}", setResponse);
        Assert.assertEquals(1, setResponse.getResponseCount());
        Assert.assertEquals(Gnmi.UpdateResult.Operation.DELETE, setResponse.getResponse(0).getOp());

        //Get interfaces, should throw exception
        LOG.info("Sending get request:\n{}", getRequest);

        Assert.assertThrows(ExecutionException.class, () -> sessionProvider.getGnmiSession().get(getRequest).get());
    }

    @Test
    public void crudSimpleAugmentedValue() throws ExecutionException, InterruptedException, IOException {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "br0")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("ethernet")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("enable-flow-control")
                        .build())
                .build();

        final Gnmi.GetRequest getRequest = Gnmi.GetRequest.newBuilder()
                .addPath(path)
                .setEncoding(Gnmi.Encoding.JSON_IETF)
                .setType(Gnmi.GetRequest.DataType.CONFIG)
                .build();

        // Get enable-flow-control
        LOG.info("Sending get request:\n{}", getRequest);
        Gnmi.GetResponse getResponse = sessionProvider.getGnmiSession().get(getRequest).get();
        LOG.info("Received get response:\n{}", getResponse);
        String responseJson = getResponse.getNotification(0)
                .getUpdate(0)
                .getVal()
                .getJsonIetfVal()
                .toStringUtf8();

        final String originalInterfacesJson = TestUtils
                .readFile(INITIAL_DATA_PATH + "/config.json");
        final JsonPrimitive originalJsonValue = new JsonParser().parse(originalInterfacesJson).getAsJsonObject()
                .getAsJsonObject(OPENCONFIG_INTERFACES)
                .getAsJsonArray(OPENCONFIG_INTERFACE)
                .get(1)
                .getAsJsonObject()
                .getAsJsonObject(ETHRERNET_PREFIX + ":ethernet")
                .getAsJsonObject("config")
                .getAsJsonPrimitive("enable-flow-control");

        final String expectedOriginalFlowControl = "{\"enable-flow-control\": "
                + originalJsonValue.getAsBoolean() + "}";
        Assert.assertTrue(TestUtils.jsonMatch(responseJson, expectedOriginalFlowControl));

        // Send Set request, change enable-flow-control to opposite
        final Gnmi.Update update = Gnmi.Update.newBuilder()
                .setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                        .setBoolVal(!originalJsonValue.getAsBoolean())
                        .build())
                .build();
        Gnmi.SetRequest setRequest = Gnmi.SetRequest.newBuilder()
                .addUpdate(update)
                .build();
        LOG.info("Sending set request:\n{}", setRequest);

        Gnmi.SetResponse setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        LOG.info("Received set response:\n{}", setResponse);

        // Get ethernet config
        LOG.info("Sending get request:\n{}", getRequest);
        getResponse = sessionProvider.getGnmiSession().get(getRequest).get();
        LOG.info("Received get response:\n{}", getResponse);
        responseJson = getResponse.getNotification(0)
                .getUpdate(0)
                .getVal()
                .getJsonIetfVal()
                .toStringUtf8();

        final String expectedUpdatedFlowControl = "{\"enable-flow-control\": "
                + !originalJsonValue.getAsBoolean() + "}";
        Assert.assertTrue(TestUtils.jsonMatch(responseJson, expectedUpdatedFlowControl));
        // Delete interfaces
        setRequest = Gnmi.SetRequest.newBuilder()
                .addDelete(path)
                .build();

        LOG.info("Sending delete request:\n{}", setRequest);
        setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        LOG.info("Received delete response:\n{}", setResponse);
        Assert.assertEquals(1, setResponse.getResponseCount());
        Assert.assertEquals(Gnmi.UpdateResult.Operation.DELETE, setResponse.getResponse(0).getOp());

        //Get interfaces, should throw exception
        LOG.info("Sending get request:\n{}", getRequest);

        Assert.assertThrows(ExecutionException.class, () -> sessionProvider.getGnmiSession().get(getRequest).get());
    }


    @Test
    public void crudComplexAugmentedValue() throws ExecutionException, InterruptedException, IOException {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "br0")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("ethernet")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config")
                        .build())
                .build();

        final Gnmi.GetRequest getRequest = Gnmi.GetRequest.newBuilder()
                .addPath(path)
                .setEncoding(Gnmi.Encoding.JSON_IETF)
                .setType(Gnmi.GetRequest.DataType.CONFIG)
                .build();

        // Get ethernet config
        LOG.info("Sending get request:\n{}", getRequest);
        Gnmi.GetResponse getResponse = sessionProvider.getGnmiSession().get(getRequest).get();
        LOG.info("Received get response:\n{}", getResponse);
        String responseJson = getResponse.getNotification(0)
                .getUpdate(0)
                .getVal()
                .getJsonIetfVal()
                .toStringUtf8();

        final String originalInterfacesJson = TestUtils
                .readFile(INITIAL_DATA_PATH + "/config.json");
        JsonObject expectedJson = new JsonParser().parse(originalInterfacesJson).getAsJsonObject()
                .getAsJsonObject(OPENCONFIG_INTERFACES)
                .getAsJsonArray(OPENCONFIG_INTERFACE)
                .get(1)
                .getAsJsonObject()
                .getAsJsonObject(ETHRERNET_PREFIX + ":ethernet")
                .getAsJsonObject("config");

        //Wrap expected result in openconfig-if-ethernet because the simulator returns it that way (with prefix)
        JsonObject wrappedObject = new JsonObject();
        wrappedObject.add(ETHRERNET_PREFIX + ":config", expectedJson);
        expectedJson = wrappedObject;

        Assert.assertTrue(TestUtils.jsonMatch(responseJson, expectedJson.toString()));
        // Set enable-flow-control of 1 index interface (br0) to false
        expectedJson.getAsJsonObject(ETHRERNET_PREFIX + ":config")
                .addProperty("enable-flow-control", false);

        // Send Set request
        final Gnmi.Update update = Gnmi.Update.newBuilder()
                .setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                        .setJsonIetfVal(ByteString.copyFromUtf8(expectedJson
                                .getAsJsonObject(ETHRERNET_PREFIX + ":config").toString()))
                        .build())
                .build();
        Gnmi.SetRequest setRequest = Gnmi.SetRequest.newBuilder()
                .addUpdate(update)
                .build();
        LOG.info("Sending set request:\n{}", setRequest);

        Gnmi.SetResponse setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        LOG.info("Received set response:\n{}", setResponse);

        // Get ethernet config
        LOG.info("Sending get request:\n{}", getRequest);
        getResponse = sessionProvider.getGnmiSession().get(getRequest).get();
        LOG.info("Received get response:\n{}", getResponse);
        responseJson = getResponse.getNotification(0)
                .getUpdate(0)
                .getVal()
                .getJsonIetfVal()
                .toStringUtf8();

        Assert.assertTrue(TestUtils.jsonMatch(responseJson, expectedJson.toString()));
        // Delete interfaces
        setRequest = Gnmi.SetRequest.newBuilder()
                .addDelete(path)
                .build();

        LOG.info("Sending delete request:\n{}", setRequest);
        setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        LOG.info("Received delete response:\n{}", setResponse);
        Assert.assertEquals(1, setResponse.getResponseCount());
        Assert.assertEquals(Gnmi.UpdateResult.Operation.DELETE, setResponse.getResponse(0).getOp());

        //Get interfaces, should throw exception
        LOG.info("Sending get request:\n{}", getRequest);

        Assert.assertThrows(ExecutionException.class, () -> sessionProvider.getGnmiSession().get(getRequest).get());
    }


    @Test
    public void getListEntryTest() throws ExecutionException, InterruptedException, IOException {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder().setName("interface")
                        .putKey("name", "eth3")
                        .build())
                .build();

        final Gnmi.GetRequest getRequest = Gnmi.GetRequest.newBuilder()
                .addPath(path)
                .setEncoding(Gnmi.Encoding.JSON_IETF)
                .setType(Gnmi.GetRequest.DataType.CONFIG)
                .build();

        LOG.info("Sending get request:\n{}", getRequest);
        final Gnmi.GetResponse getResponse = sessionProvider.getGnmiSession().get(getRequest).get();
        LOG.info("Received get response:\n{}", getResponse);
        final String responseJson = getResponse.getNotification(0)
                .getUpdate(0)
                .getVal()
                .getJsonIetfVal()
                .toStringUtf8();

        final String originalInterfacesJson = TestUtils
                .readFile(INITIAL_DATA_PATH + "/config.json");
        final JsonElement jsonElement = new JsonParser().parse(originalInterfacesJson).getAsJsonObject()
                .get(OPENCONFIG_INTERFACES).getAsJsonObject().getAsJsonArray(OPENCONFIG_INTERFACE)
                .get(0)
                .getAsJsonObject();

        // Add prefix to config element
        final JsonElement configElement = jsonElement
                .getAsJsonObject()
                .getAsJsonObject(OPENCONFIG_CONFIG);

        jsonElement.getAsJsonObject().remove(OPENCONFIG_CONFIG);
        jsonElement.getAsJsonObject().add(INTERFACES_PREFIX + ":" + OPENCONFIG_CONFIG, configElement);
        Assert.assertTrue(TestUtils.jsonMatch(responseJson, jsonElement.toString()));
    }

    @Test
    public void capabilityTest() throws ExecutionException, InterruptedException {
        final Gnmi.CapabilityRequest request = Gnmi.CapabilityRequest.newBuilder().build();

        LOG.info("Sending capabilities request:\n{}", request);
        final ListenableFuture<Gnmi.CapabilityResponse> capabilityFuture = sessionProvider
                .getGnmiSession()
                .capabilities(request);

        final Gnmi.CapabilityResponse capabilityResponse = capabilityFuture.get();
        LOG.info("Received capabilities response:\n{}", capabilityResponse);

        Assert.assertFalse(capabilityResponse.getSupportedEncodingsList().isEmpty());
        Assert.assertFalse(capabilityResponse.getSupportedModelsList().isEmpty());
        Assert.assertFalse(capabilityResponse.getGNMIVersion().isEmpty());
    }

    private static String getEthernetExpectedResponse() {
        return "{\"openconfig-if-ethernet:ethernet\":{"
                    + "\"config\":{"
                       + "\"enable-flow-control\":true,"
                       + "\"openconfig-if-aggregate:aggregate-id\":\"admin\","
                       + "\"auto-negotiate\":true,"
                       + "\"port-speed\":\"openconfig-if-ethernet:SPEED_10MB\""
                   + "}"
                       + ",\"openconfig-vlan:switched-vlan\":{"
                           + "\"config\":{"
                               + "\"native-vlan\":37,"
                               + "\"access-vlan\":45,"
                               + "\"interface-mode\":\"ACCESS\""
                           + "}"
                       + "}"
                   + "}"
               + "}";
    }
}
