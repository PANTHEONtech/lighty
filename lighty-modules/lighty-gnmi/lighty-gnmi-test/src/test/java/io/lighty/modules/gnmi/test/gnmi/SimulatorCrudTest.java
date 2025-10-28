/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.gnmi.test.gnmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.utils.GnmiSimulatorConfUtils;
import io.lighty.modules.gnmi.test.utils.TestUtils;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulatorCrudTest {
    private static final Logger LOG = LoggerFactory.getLogger(SimulatorCrudTest.class);

    private static final int TARGET_PORT = 10161;
    private static final String TARGET_HOST = "127.0.0.1";
    private static final String INITIAL_DATA_PATH = "src/test/resources/json/initData";
    private static final String SIMULATOR_CONFIG = "/json/simulator_config.json";
    private static final String SERVER_KEY = "src/test/resources/certs/server-pkcs8.key";
    private static final String SERVER_CERT = "src/test/resources/certs/server.crt";
    private static final String INTERFACES_PREFIX = "openconfig-interfaces";
    private static final String OPENCONFIG_INTERFACES = INTERFACES_PREFIX + ":" + "interfaces";
    private static final String ETHRERNET_PREFIX = "openconfig-if-ethernet";
    private static final String OPENCONFIG_INTERFACE = "interface";
    private static final String OPENCONFIG_CONFIG = "config";
    private static final int UPDATE_MTU_VAL = 500;

    private static SessionProvider sessionProvider;
    private static SimulatedGnmiDevice target;


    @BeforeEach
    public void setUp() throws Exception {
        GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(this.getClass().getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfiguration.setTargetAddress(TARGET_HOST);
        simulatorConfiguration.setTargetPort(TARGET_PORT);
        simulatorConfiguration.setInitialConfigDataPath(INITIAL_DATA_PATH + "/config.json");
        simulatorConfiguration.setInitialStateDataPath(INITIAL_DATA_PATH + "/state.json");
        simulatorConfiguration.setCertKeyPath(SERVER_KEY);
        simulatorConfiguration.setCertPath(SERVER_CERT);

        target = new SimulatedGnmiDevice(simulatorConfiguration);
        target.start();
        final SessionManager sessionManager = TestUtils.createSessionManagerWithCerts();
        final InetSocketAddress targetAddress = new InetSocketAddress(TARGET_HOST, TARGET_PORT);
        sessionProvider = sessionManager.createSession(
                new SessionConfiguration(targetAddress, false));
    }

    @AfterEach
    public void after() throws Exception {
        sessionProvider.close();
        target.stop();
    }

    @Test
    public void getDataWithAugmentationTest() throws ExecutionException, InterruptedException, JSONException {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName(OPENCONFIG_INTERFACES)
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
        assertEquals(1, getResponse.getNotificationCount());
        assertEquals(0, getResponse.getNotification(0).getDeleteCount());
        assertEquals(1, getResponse.getNotification(0).getUpdateCount());
        JSONAssert.assertEquals(getEthernetExpectedResponse(),
                getResponse.getNotification(0).getUpdate(0).getVal().getJsonIetfVal().toStringUtf8(), false);
    }

    @Test
    public void crudSimpleValueTest() throws ExecutionException, InterruptedException, IOException, JSONException {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName(OPENCONFIG_INTERFACES)
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

        // construct simple json
        assertEquals(1, getResponse.getNotificationCount());
        assertEquals(0, getResponse.getNotification(0).getDeleteCount());
        assertEquals(1, getResponse.getNotification(0).getUpdateCount());

        // Get mtu from config file
        final String originalInterfacesJson = TestUtils
                .readFile(INITIAL_DATA_PATH + "/config.json");
        final JsonElement jsonElement = JsonParser.parseString(originalInterfacesJson).getAsJsonObject()
                .get(OPENCONFIG_INTERFACES);
        final int expectedOriginalMtu = jsonElement.getAsJsonObject().getAsJsonArray(OPENCONFIG_INTERFACE)
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("config")
                .getAsJsonPrimitive("mtu").getAsInt();
        final String expectedOriginalMtuJson = "{\"mtu\": " + expectedOriginalMtu + "}";
        JSONAssert.assertEquals(expectedOriginalMtuJson,
                getResponse.getNotification(0).getUpdate(0).getVal().getJsonIetfVal().toStringUtf8(), false);

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
        assertEquals(1, setResponse.getResponseCount());
        assertEquals(Gnmi.UpdateResult.Operation.UPDATE, setResponse.getResponse(0).getOp());

        // Get mtu, should be UPDATE_MTU_VAL
        LOG.info("Sending get request:\n{}", getRequest);
        getResponse = sessionProvider.getGnmiSession().get(getRequest).get();
        LOG.info("Received get response:\n{}", getResponse);

        assertEquals(1, getResponse.getNotificationCount());
        assertEquals(0, getResponse.getNotification(0).getDeleteCount());
        assertEquals(1, getResponse.getNotification(0).getUpdateCount());
        final String expectedChangedMtuJson = "{\"mtu\": " + UPDATE_MTU_VAL + "}";
        JSONAssert.assertEquals(expectedChangedMtuJson,
                getResponse.getNotification(0).getUpdate(0).getVal().getJsonIetfVal().toStringUtf8(), false);

        // Delete mtu
        setRequest = Gnmi.SetRequest.newBuilder()
                .addDelete(path)
                .build();

        LOG.info("Sending delete request:\n{}", setRequest);
        setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        LOG.info("Received delete response:\n{}", setResponse);
        assertEquals(1, setResponse.getResponseCount());
        assertEquals(Gnmi.UpdateResult.Operation.DELETE, setResponse.getResponse(0).getOp());

        // Get mtu, should throw exception
        LOG.info("Sending get request:\n{}", getRequest);

        assertThrows(ExecutionException.class, () -> sessionProvider.getGnmiSession().get(getRequest).get());
    }

    @Test
    public void setContainerInsideList() throws Exception {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName(OPENCONFIG_INTERFACES)
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "eth5")
                        .build())
                .build();
        final Gnmi.Update update = Gnmi.Update.newBuilder()
                .setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                        .setJsonIetfVal(ByteString.copyFromUtf8(getConfigData()))
                        .build())
                .build();
        final Gnmi.SetRequest setRequest = Gnmi.SetRequest.newBuilder()
                .addUpdate(update)
                .build();
        LOG.info("Sending set request:\n{}", setRequest);

        final Gnmi.SetResponse setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        assertEquals("UPDATE", setResponse.getResponse(0).getOp().toString());
    }

    @Test
    public void setAugmentedTestInterfaceConfigTest() throws Exception {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("gnmi-test-model:test-data")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("nested-container")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "eth5")
                        .build())
                .build();
        final Gnmi.Update update = Gnmi.Update.newBuilder()
                .setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                        .setJsonIetfVal(ByteString.copyFromUtf8(getConfigData()))
                        .build())
                .build();
        final Gnmi.SetRequest setRequest = Gnmi.SetRequest.newBuilder()
                .addUpdate(update)
                .build();
        LOG.info("Sending set request:\n{}", setRequest);

        final Gnmi.SetResponse setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        assertEquals("UPDATE", setResponse.getResponse(0).getOp().toString());
    }

    @Test
    public void setContainerWithMultipleListKeyInPathTest() throws Exception {
        final var testDataPath = Gnmi.Path.newBuilder()
            .addElem(Gnmi.PathElem.newBuilder()
                .setName("gnmi-test-model:test-data")
                .build())
            .build();
        final var multipleListKeyUpdate = Gnmi.Update.newBuilder()
            .setPath(testDataPath)
            .setVal(Gnmi.TypedValue.newBuilder()
                .setJsonIetfVal(ByteString.copyFromUtf8("""
                    {
                      "multiple-key-list" : [
                        {
                          "number": 10,
                          "leafref-key": 15,
                          "identityref-key": "openconfig-aaa-types:SYSTEM_ROLE_ADMIN",
                          "union-key": "unbounded"
                        }
                      ]
                    }
                    """))
                .build())
            .build();

        final var innerDataPath = Gnmi.Path.newBuilder()
            .addElem(Gnmi.PathElem.newBuilder()
                .setName("gnmi-test-model:test-data")
                .build())
            .addElem(Gnmi.PathElem.newBuilder()
                .setName("multiple-key-list")
                .putKey("number", "10")
                .putKey("leafref-key", "15")
                .putKey("identityref-key", "openconfig-aaa-types:SYSTEM_ROLE_ADMIN")
                .putKey("union-key", "unbounded")
                .build())
            .addElem(Gnmi.PathElem.newBuilder()
                .setName("inner-container")
                .build())
            .build();
        final var innerDataUpdate = Gnmi.Update.newBuilder()
            .setPath(innerDataPath)
            .setVal(Gnmi.TypedValue.newBuilder()
                .setJsonIetfVal(ByteString.copyFromUtf8("""
                    {
                      "inner-data": "data"
                    }
                    """))
                .build())
            .build();
        final var setRequest = Gnmi.SetRequest.newBuilder()
            .addUpdate(multipleListKeyUpdate)
            .addUpdate(innerDataUpdate)
            .build();
        LOG.info("Sending set request:\n{}", setRequest);

        final var setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        assertEquals("UPDATE", setResponse.getResponse(0).getOp().toString());
        assertEquals("UPDATE", setResponse.getResponse(1).getOp().toString());
    }

    @Test
    public void setMultipleKeyListAsLastElementInPathTest() throws Exception {
        final var multipleKeyPath = Gnmi.Path.newBuilder()
            .addElem(Gnmi.PathElem.newBuilder()
                .setName("gnmi-test-model:test-data")
                .build())
            .addElem(Gnmi.PathElem.newBuilder()
                .setName("multiple-key-list")
                .putKey("number", "10")
                .putKey("leafref-key", "15")
                .putKey("identityref-key", "openconfig-aaa-types:SYSTEM_DEFINED_ROLES")
                .putKey("union-key", "5")
                .build())
            .build();
        final var innerContainerUpdate = Gnmi.Update.newBuilder()
            .setPath(multipleKeyPath)
            .setVal(Gnmi.TypedValue.newBuilder()
                .setJsonIetfVal(ByteString.copyFromUtf8("""
                    {
                      "inner-container": {
                        "inner-data": "data"
                      }
                    }
                    """))
                .build())
            .build();
        final var setRequest = Gnmi.SetRequest.newBuilder().addUpdate(innerContainerUpdate).build();
        LOG.info("Sending set request:\n{}", setRequest);

        final var setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        assertEquals("UPDATE", setResponse.getResponse(0).getOp().toString());
    }

    @Test
    public void crudComplexValueTest() throws ExecutionException, InterruptedException, IOException, JSONException {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName(OPENCONFIG_INTERFACES)
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
        final JsonObject jsonElement = JsonParser.parseString(originalInterfacesJson).getAsJsonObject();
        JSONAssert.assertEquals(responseJson, jsonElement.toString(), false);

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

        JSONAssert.assertEquals(responseJson, jsonElement.toString(), false);
        // Delete interfaces
        setRequest = Gnmi.SetRequest.newBuilder()
                .addDelete(path)
                .build();

        LOG.info("Sending delete request:\n{}", setRequest);
        setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        LOG.info("Received delete response:\n{}", setResponse);
        assertEquals(1, setResponse.getResponseCount());
        assertEquals(Gnmi.UpdateResult.Operation.DELETE, setResponse.getResponse(0).getOp());

        //Get interfaces, should throw exception
        LOG.info("Sending get request:\n{}", getRequest);

        assertThrows(ExecutionException.class, () -> sessionProvider.getGnmiSession().get(getRequest).get());
    }

    @Test
    public void crudSimpleAugmentedValue() throws ExecutionException, InterruptedException, IOException, JSONException {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName(OPENCONFIG_INTERFACES)
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
        final JsonPrimitive originalJsonValue = JsonParser.parseString(originalInterfacesJson).getAsJsonObject()
                .getAsJsonObject(OPENCONFIG_INTERFACES)
                .getAsJsonArray(OPENCONFIG_INTERFACE)
                .get(1)
                .getAsJsonObject()
                .getAsJsonObject(ETHRERNET_PREFIX + ":ethernet")
                .getAsJsonObject("config")
                .getAsJsonPrimitive("enable-flow-control");

        final String expectedOriginalFlowControl = "{\"enable-flow-control\": "
                + originalJsonValue.getAsBoolean() + "}";
        JSONAssert.assertEquals(expectedOriginalFlowControl, responseJson, false);

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
        JSONAssert.assertEquals(expectedUpdatedFlowControl, responseJson, false);
        // Delete interfaces
        setRequest = Gnmi.SetRequest.newBuilder()
                .addDelete(path)
                .build();

        LOG.info("Sending delete request:\n{}", setRequest);
        setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        LOG.info("Received delete response:\n{}", setResponse);
        assertEquals(1, setResponse.getResponseCount());
        assertEquals(Gnmi.UpdateResult.Operation.DELETE, setResponse.getResponse(0).getOp());

        //Get interfaces, should throw exception
        LOG.info("Sending get request:\n{}", getRequest);

        assertThrows(ExecutionException.class, () -> sessionProvider.getGnmiSession().get(getRequest).get());
    }

    @Test
    public void crudComplexAugmentedValue() throws ExecutionException, InterruptedException, IOException,
            JSONException {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName(OPENCONFIG_INTERFACES)
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
        JsonObject expectedJson = JsonParser.parseString(originalInterfacesJson).getAsJsonObject()
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
        JSONAssert.assertEquals(expectedJson.toString(), responseJson, false);

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
        JSONAssert.assertEquals(expectedJson.toString(), responseJson, false);

        // Delete interfaces
        setRequest = Gnmi.SetRequest.newBuilder()
                .addDelete(path)
                .build();

        LOG.info("Sending delete request:\n{}", setRequest);
        setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        LOG.info("Received delete response:\n{}", setResponse);
        assertEquals(1, setResponse.getResponseCount());
        assertEquals(Gnmi.UpdateResult.Operation.DELETE, setResponse.getResponse(0).getOp());

        //Get interfaces, should throw exception
        LOG.info("Sending get request:\n{}", getRequest);

        assertThrows(ExecutionException.class, () -> sessionProvider.getGnmiSession().get(getRequest).get());
    }


    @Test
    public void getListEntryTest() throws ExecutionException, InterruptedException, IOException, JSONException {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName(OPENCONFIG_INTERFACES)
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
        final JsonElement jsonElement = JsonParser.parseString(originalInterfacesJson).getAsJsonObject()
                .get(OPENCONFIG_INTERFACES).getAsJsonObject().getAsJsonArray(OPENCONFIG_INTERFACE)
                .get(0)
                .getAsJsonObject();

        // Add prefix to config element
        final JsonElement configElement = jsonElement
                .getAsJsonObject()
                .getAsJsonObject(OPENCONFIG_CONFIG);

        jsonElement.getAsJsonObject().remove(OPENCONFIG_CONFIG);
        jsonElement.getAsJsonObject().add(INTERFACES_PREFIX + ":" + OPENCONFIG_CONFIG, configElement);
        JSONAssert.assertEquals(jsonElement.toString(), responseJson, false);
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

        assertFalse(capabilityResponse.getSupportedEncodingsList().isEmpty());
        assertFalse(capabilityResponse.getSupportedModelsList().isEmpty());
        assertFalse(capabilityResponse.getGNMIVersion().isEmpty());
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

    private static String getConfigData() {
        return "{\n"
                + "    \"config\": {\n"
                + "      \"name\": \"Vlan10\",\n"
                + "      \"type\": \"iana-if-type:l2vlan\",\n"
                + "      \"mtu\": 1550\n"
                + "    }\n"
                + "}";
    }
}
