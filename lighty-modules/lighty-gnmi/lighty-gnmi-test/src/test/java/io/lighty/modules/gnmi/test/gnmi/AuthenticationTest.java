/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.test.gnmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import gnmi.Gnmi;
import gnmi.Gnmi.GetRequest;
import gnmi.Gnmi.GetResponse;
import gnmi.Gnmi.Path;
import gnmi.Gnmi.PathElem;
import gnmi.Gnmi.SetRequest;
import gnmi.Gnmi.SetResponse;
import gnmi.Gnmi.TypedValue;
import gnmi.Gnmi.Update;
import gnmi.Gnmi.UpdateResult;
import io.lighty.modules.gnmi.connector.configuration.SecurityFactory;
import io.lighty.modules.gnmi.connector.configuration.SessionConfiguration;
import io.lighty.modules.gnmi.connector.gnmi.session.impl.GnmiSessionFactoryImpl;
import io.lighty.modules.gnmi.connector.session.SessionManagerFactoryImpl;
import io.lighty.modules.gnmi.connector.session.api.SessionManager;
import io.lighty.modules.gnmi.connector.session.api.SessionProvider;
import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.utils.EffectiveModelContextBuilder.EffectiveModelContextBuilderException;
import io.lighty.modules.gnmi.simulatordevice.utils.GnmiSimulatorConfUtils;
import io.lighty.modules.gnmi.test.utils.TestUtils;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationTest {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationTest.class);
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final String WRONG_USERNAME = "foo";
    private static final String WRONG_PASSWORD = "bar";
    private static final String UNAUTHENTICATED_WRONG_USERNAME_OR_PASSWORD
            = "io.grpc.StatusRuntimeException: UNAUTHENTICATED: Wrong username or password";
    private static final String TARGET_HOST = "127.0.0.1";
    private static final String INITIAL_DATA_PATH = "src/test/resources/json/initData";
    private static final String TEST_SCHEMA_PATH = "src/test/resources/additional/models";
    private static final String SIMULATOR_CONFIG = "/json/simulator_config.json";
    private static final String INTERFACES_PREFIX = "openconfig-interfaces";
    private static final String OPENCONFIG_INTERFACES = INTERFACES_PREFIX + ":" + "interfaces";
    private static final String OPENCONFIG_INTERFACE = "interface";
    private static final String SERVER_KEY = "src/test/resources/certs/server-pkcs8.key";
    private static final String SERVER_CERT = "src/test/resources/certs/server.crt";
    private static final int UPDATE_MTU_VAL = 500;
    private static final int TARGET_PORT = 10161;

    @Test
    public void crudSimpleValueWithAuthenticationTest() throws Exception {
        final SimulatedGnmiDevice device = startDeviceWithAuthentication(USERNAME, PASSWORD);
        final Path pathToMtu = getPathToMtu();
        final GetRequest getRequest = getGnmiRequest(pathToMtu);
        // Get mtu, should be 1500 from initial simulator data, load it from that initial config file
        LOG.info("Sending get request:\n{}", getRequest);
        final SessionProvider sessionProvider = getSessionWithAuth(USERNAME, PASSWORD);
        GetResponse getResponse = sessionProvider.getGnmiSession().get(getRequest).get();
        LOG.info("Received get response:\n{}", getResponse);

        final String originalInterfacesJson = TestUtils
                .readFile(INITIAL_DATA_PATH + "/config.json");
        final JsonElement jsonElement
                = JsonParser.parseString(originalInterfacesJson).getAsJsonObject().get(OPENCONFIG_INTERFACES);
        // Get mtu from config file
        final int expectedOriginalMtu = jsonElement.getAsJsonObject().getAsJsonArray(OPENCONFIG_INTERFACE)
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("config")
                .getAsJsonPrimitive("mtu").getAsInt();
        // construct simple json
        final String expectedOriginalMtuJson = "{\"mtu\": " + expectedOriginalMtu + "}";

        assertEquals(1, getResponse.getNotificationCount());
        assertEquals(0, getResponse.getNotification(0).getDeleteCount());
        assertEquals(1, getResponse.getNotification(0).getUpdateCount());
        JSONAssert.assertEquals(expectedOriginalMtuJson,
                getResponse.getNotification(0).getUpdate(0).getVal().getJsonIetfVal().toStringUtf8(), false);

        // Set mtu to UPDATE_MTU_VAL
        final SetRequest setRequest = getSetRequest(pathToMtu);
        LOG.info("Sending set request:\n{}", setRequest);
        SetResponse setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        LOG.info("Received set response:\n{}", setResponse);
        assertEquals(1, setResponse.getResponseCount());
        assertEquals(UpdateResult.Operation.UPDATE, setResponse.getResponse(0).getOp());

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
        final SetRequest deleteRequest = getDeleteRequest(pathToMtu);
        LOG.info("Sending delete request:\n{}", deleteRequest);
        setResponse = sessionProvider.getGnmiSession().set(deleteRequest).get();
        LOG.info("Received delete response:\n{}", setResponse);
        assertEquals(1, setResponse.getResponseCount());
        assertEquals(UpdateResult.Operation.DELETE, setResponse.getResponse(0).getOp());

        // Get mtu, should throw exception
        LOG.info("Sending get request:\n{}", getRequest);
        assertThrows(ExecutionException.class, () -> sessionProvider.getGnmiSession().get(getRequest).get());

        device.stop();
        sessionProvider.close();
    }

    @Test
    public void crudSimpleValueWithWrongAuthenticationTest() throws Exception {
        final SimulatedGnmiDevice device = startDeviceWithAuthentication(USERNAME, PASSWORD);
        final Path pathToMtu = getPathToMtu();
        final GetRequest getRequest = getGnmiRequest(pathToMtu);
        // Should throw Unauthenticated exception
        LOG.info("Sending get request:\n{}", getRequest);
        final SessionProvider sessionProvider = getSessionWithAuth(WRONG_USERNAME, WRONG_PASSWORD);
        final ExecutionException getResponseException = assertThrows(
                ExecutionException.class, () -> sessionProvider.getGnmiSession().get(getRequest).get());
        assertEquals(UNAUTHENTICATED_WRONG_USERNAME_OR_PASSWORD, getResponseException.getMessage());

        // Should throw Unauthenticated exception
        final SetRequest setRequest = getSetRequest(pathToMtu);
        LOG.info("Sending set request:\n{}", setRequest);
        final ExecutionException setResponseException = assertThrows(
                ExecutionException.class, () -> sessionProvider.getGnmiSession().set(setRequest).get());
        assertEquals(UNAUTHENTICATED_WRONG_USERNAME_OR_PASSWORD, setResponseException.getMessage());

        // Should throw Unauthenticated exception
        final SetRequest deleteRequest = getDeleteRequest(pathToMtu);
        LOG.info("Sending delete request:\n{}", deleteRequest);
        final ExecutionException deleteResponseException = assertThrows(
                ExecutionException.class, () -> sessionProvider.getGnmiSession().set(deleteRequest).get());
        assertEquals(UNAUTHENTICATED_WRONG_USERNAME_OR_PASSWORD, deleteResponseException.getMessage());

        device.stop();
        sessionProvider.close();
    }

    @Test
    public void crudSimpleValueWithNoTlsTest() throws Exception {
        final SimulatedGnmiDevice device = startDeviceInNotTlsMode();
        final Path pathToMtu = getPathToMtu();
        final GetRequest getRequest = getGnmiRequest(pathToMtu);
        // Get mtu, should be 1500 from initial simulator data, load it from that initial config file
        LOG.info("Sending get request:\n{}", getRequest);
        final SessionProvider sessionProvider = getSessionInNoTLS();
        GetResponse getResponse = sessionProvider.getGnmiSession().get(getRequest).get();
        LOG.info("Received get response:\n{}", getResponse);

        final String originalInterfacesJson = TestUtils
                .readFile(INITIAL_DATA_PATH + "/config.json");
        final JsonElement jsonElement
                = JsonParser.parseString(originalInterfacesJson).getAsJsonObject().get(OPENCONFIG_INTERFACES);
        // Get mtu from config file
        final int expectedOriginalMtu = jsonElement.getAsJsonObject().getAsJsonArray(OPENCONFIG_INTERFACE)
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("config")
                .getAsJsonPrimitive("mtu").getAsInt();
        // construct simple json
        final String expectedOriginalMtuJson = "{\"mtu\": " + expectedOriginalMtu + "}";

        assertEquals(1, getResponse.getNotificationCount());
        assertEquals(0, getResponse.getNotification(0).getDeleteCount());
        assertEquals(1, getResponse.getNotification(0).getUpdateCount());
        JSONAssert.assertEquals(expectedOriginalMtuJson,
                getResponse.getNotification(0).getUpdate(0).getVal().getJsonIetfVal().toStringUtf8(), false);

        // Set mtu to UPDATE_MTU_VAL
        final SetRequest setRequest = getSetRequest(pathToMtu);
        LOG.info("Sending set request:\n{}", setRequest);
        SetResponse setResponse = sessionProvider.getGnmiSession().set(setRequest).get();
        LOG.info("Received set response:\n{}", setResponse);
        assertEquals(1, setResponse.getResponseCount());
        assertEquals(UpdateResult.Operation.UPDATE, setResponse.getResponse(0).getOp());

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
        final SetRequest deleteRequest = getDeleteRequest(pathToMtu);
        LOG.info("Sending delete request:\n{}", deleteRequest);
        setResponse = sessionProvider.getGnmiSession().set(deleteRequest).get();
        LOG.info("Received delete response:\n{}", setResponse);
        assertEquals(1, setResponse.getResponseCount());
        assertEquals(UpdateResult.Operation.DELETE, setResponse.getResponse(0).getOp());

        // Get mtu, should throw exception
        LOG.info("Sending get request:\n{}", getRequest);
        assertThrows(ExecutionException.class, () -> sessionProvider.getGnmiSession().get(getRequest).get());

        device.stop();
        sessionProvider.close();
    }

    @Test
    public void crudSimpleValueWithNoTlsDeviceAndTlsSessionTest() throws Exception {
        final SimulatedGnmiDevice device = startDeviceInNotTlsMode();
        final Path pathToMtu = getPathToMtu();
        final GetRequest getRequest = getGnmiRequest(pathToMtu);
        // Get mtu, should be 1500 from initial simulator data, load it from that initial config file
        LOG.info("Sending get request:\n{}", getRequest);
        final SessionProvider sessionProvider = getSessionWithAuth(USERNAME, PASSWORD);
        assertThrows(ExecutionException.class, () -> sessionProvider.getGnmiSession().get(getRequest).get());

        device.stop();
        sessionProvider.close();
    }

    private SimulatedGnmiDevice startDeviceWithAuthentication(final String username, final String password)
            throws IOException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(this.getClass().getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfiguration.setTargetAddress(TARGET_HOST);
        simulatorConfiguration.setTargetPort(TARGET_PORT);
        simulatorConfiguration.setYangsPath(TEST_SCHEMA_PATH);
        simulatorConfiguration.setInitialConfigDataPath(INITIAL_DATA_PATH + "/config.json");
        simulatorConfiguration.setInitialStateDataPath(INITIAL_DATA_PATH + "/state.json");
        simulatorConfiguration.setCertKeyPath(SERVER_KEY);
        simulatorConfiguration.setCertPath(SERVER_CERT);
        simulatorConfiguration.setUsername(username);
        simulatorConfiguration.setPassword(password);

        final SimulatedGnmiDevice authenticateDevice =
                new SimulatedGnmiDevice(simulatorConfiguration);
        authenticateDevice.start();
        return authenticateDevice;
    }

    private SimulatedGnmiDevice startDeviceInNotTlsMode()
            throws IOException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(this.getClass().getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfiguration.setTargetAddress(TARGET_HOST);
        simulatorConfiguration.setTargetPort(TARGET_PORT);
        simulatorConfiguration.setYangsPath(TEST_SCHEMA_PATH);
        simulatorConfiguration.setInitialConfigDataPath(INITIAL_DATA_PATH + "/config.json");
        simulatorConfiguration.setInitialStateDataPath(INITIAL_DATA_PATH + "/state.json");
        simulatorConfiguration.setUsePlaintext(true);

        final SimulatedGnmiDevice authenticateDevice
                = new SimulatedGnmiDevice(simulatorConfiguration);
        authenticateDevice.start();
        return authenticateDevice;
    }

    private SessionProvider getSessionInNoTLS() {
        final SessionManagerFactoryImpl managerFactory = new SessionManagerFactoryImpl(new GnmiSessionFactoryImpl());
        final SessionManager sessionManager = managerFactory.createSessionManager(
                SecurityFactory.createInsecureGnmiSecurity());
        final InetSocketAddress targetAddress = new InetSocketAddress(TARGET_HOST, TARGET_PORT);
        return sessionManager.createSession(new SessionConfiguration(targetAddress, true));
    }

    private SessionProvider getSessionWithAuth(final String username, final String password) throws Exception {
        final SessionManager sessionManager = TestUtils.createSessionManagerWithCerts();
        final InetSocketAddress targetAddress = new InetSocketAddress(TARGET_HOST, TARGET_PORT);
        return sessionManager.createSession(new SessionConfiguration(targetAddress, false, username, password));
    }

    private SetRequest getDeleteRequest(final Path path) {
        return SetRequest.newBuilder()
                .addDelete(path)
                .build();
    }

    private SetRequest getSetRequest(final Path path) {
        final Update update = Update.newBuilder()
                .setPath(path)
                .setVal(TypedValue.newBuilder()
                        .setIntVal(UPDATE_MTU_VAL)
                        .build())
                .build();
        return SetRequest.newBuilder()
                .addUpdate(update)
                .build();
    }

    private GetRequest getGnmiRequest(final Path path) {
        return GetRequest.newBuilder()
                .addPath(path)
                .setEncoding(Gnmi.Encoding.JSON_IETF)
                .build();
    }

    private Path getPathToMtu() {
        return Path.newBuilder()
                .addElem(PathElem.newBuilder()
                        .setName(OPENCONFIG_INTERFACES)
                        .build())
                .addElem(PathElem.newBuilder()
                        .setName(OPENCONFIG_INTERFACE)
                        .putKey("name", "eth3")
                        .build())
                .addElem(PathElem.newBuilder()
                        .setName("config")
                        .build())
                .addElem(PathElem.newBuilder()
                        .setName("mtu")
                        .build())
                .build();
    }
}
