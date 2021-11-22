/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.test.gnmi.rcgnmi;

import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_DEVICE_MOUNTPOINT;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_NODE_ID;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_NODE_STATUS;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_NODE_STATUS_READY;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_TOPOLOGY_PATH;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.INTERFACE_ETH3_CONFIG_NAME_PAYLOAD;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.OPENCONFIG_INTERFACES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.utils.EffectiveModelContextBuilder.EffectiveModelContextBuilderException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnmiConnectionITTest extends GnmiITBase {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiConnectionITTest.class);

    private static final String GET_CAPABILITIES_PATH
        = GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_ID + "/gnmi-topology:node-state/available-capabilities";
    private static final String MODEL_OPENCONFIG_AAA_NAME = "openconfig-aaa";
    private static final String MODEL_OPENCONFIG_AAA_VERSION = "0.5.0";
    private static final String EXPECTED_CAPABILITY
        = "[{\"capability\":\"" + MODEL_OPENCONFIG_AAA_NAME + " semver: " + MODEL_OPENCONFIG_AAA_VERSION + "\"}]";
    private static final Duration CONNECT_ATTEMPT_WAIT_DURATION = Duration.ofMillis(5_000L);
    private static final String GNMI_NODE_STATUS_TRANSIENT_FAIL = "TRANSIENT_FAILURE";
    private static final String GNMI_NODE_STATUS_CONNECTING = "CONNECTING";
    private static final String GNMI_NODE_PATH = GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_ID;
    private static final int MAX_DEVICE_CONNECTION_ATTEMPTS = 4;

    private static final String ANOTHER_GNMI_NODE_ID = "gnmi-another-test-node";
    private static final int ANOTHER_DEVICE_PORT = DEVICE_PORT + 1;
    private static final String ANOTHER_GNMI_DEVICE_MOUNTPOINT =
        GNMI_TOPOLOGY_PATH + "/node=" + ANOTHER_GNMI_NODE_ID + "/yang-ext:mount";

    private static final String GNMI_NODE_WITH_WRONG_PASSWD_ID = "gnmi-credentials-test-node";
    private static final int DEVICE_WITH_CREDENTIALS_PORT = ANOTHER_DEVICE_PORT + 1;
    private static final String GNMI_NODE_MISSING_ENCODING_ID = "gnmi-missing-encoding-node";
    private static final int DEVICE_WITH_MISSING_ENCODING_PORT = DEVICE_WITH_CREDENTIALS_PORT + 1;
    private static final String DEVICE_USERNAME = "admin";
    private static final String DEVICE_PASSWORD = "admin";

    private static final String MULTIPLE_DEVICES_PAYLOAD = "{\n"
        + "    \"network-topology:topology\": [\n"
        + "        {\n"
        + "            \"topology-id\": \"gnmi-topology\",\n"
        + "            \"node\": [\n"
        + "                {\n"
        + "                    \"node-id\": \"" + GNMI_NODE_ID + "\",\n"
        + "                    \"gnmi-topology:connection-parameters\": {\n"
        + "                        \"host\": \"" + DEVICE_IP + "\",\n"
        + "                        \"port\": " + DEVICE_PORT + ",\n"
        + "                        \"connection-type\": \"INSECURE\"\n"
        + "                    },\n"
        + "                    \"extensions-parameters\": {\n"
        + "                        \"gnmi-parameters\": {\n"
        + "                            \"use-model-name-prefix\": true\n"
        + "                        }\n"
        + "                    }"
        + "                },\n"
        + "                {\n"
        + "                    \"node-id\": \"" + ANOTHER_GNMI_NODE_ID + "\",\n"
        + "                    \"gnmi-topology:connection-parameters\": {\n"
        + "                        \"host\": \"" + DEVICE_IP + "\",\n"
        + "                        \"port\": " + ANOTHER_DEVICE_PORT + ",\n"
        + "                        \"connection-type\": \"INSECURE\"\n"
        + "                    },\n"
        + "                    \"extensions-parameters\": {\n"
        + "                        \"gnmi-parameters\": {\n"
        + "                            \"use-model-name-prefix\": true\n"
        + "                        }\n"
        + "                    }"
        + "                }]\n"
        + "        }]\n"
        + "}";

    private static final String DEVICE_WITH_CREDENTIALS_PAYLOAD = "{\n"
        + "\"network-topology:node\" : [{\n"
        + "    \"node-id\": \"" + GNMI_NODE_WITH_WRONG_PASSWD_ID + "\",\n"
        + "    \"gnmi-topology:connection-parameters\": {\n"
        + "        \"host\": \"" + DEVICE_IP + "\",\n"
        + "        \"port\": " + DEVICE_WITH_CREDENTIALS_PORT + ",\n"
        + "        \"connection-type\": \"INSECURE\",\n"
        + "        \"credentials\": {\n"
        + "            \"username\": \"" + DEVICE_USERNAME + "\",\n"
        + "            \"password\": \"" + DEVICE_PASSWORD + "wrong" + "\"\n"
        + "         }"
        + "     }\n"
        + " }]\n"
        + "}";

    private static SimulatedGnmiDevice anotherDevice;
    private static SimulatedGnmiDevice device;
    private static SimulatedGnmiDevice deviceWithCredentials;
    private static SimulatedGnmiDevice deviceWithMissingEncoding;

    @BeforeAll
    public static void setupDevice() {
        device = getUnsecureGnmiDevice(DEVICE_IP, DEVICE_PORT);
        anotherDevice = getUnsecureGnmiDevice(DEVICE_IP, ANOTHER_DEVICE_PORT);
        deviceWithCredentials = getUnsecureGnmiDevice(DEVICE_IP, DEVICE_WITH_CREDENTIALS_PORT,
                                                      DEVICE_USERNAME, DEVICE_PASSWORD);
        deviceWithMissingEncoding = getNonCompliableEncodingDevice(DEVICE_IP, DEVICE_WITH_MISSING_ENCODING_PORT);
        try {
            device.start();
            anotherDevice.start();
            deviceWithCredentials.start();
            deviceWithMissingEncoding.start();
        } catch (IOException | EffectiveModelContextBuilderException e) {
            LOG.info("Exception during device startup: ", e);
        }
    }

    @AfterAll
    public static void teardownDevice() {
        device.stop();
        anotherDevice.stop();
        deviceWithCredentials.stop();
        deviceWithMissingEncoding.stop();
    }

    @AfterEach
    public void performSpecificCleanupAfterEach() {
        /*
        disconnect devices ANOTHER_GNMI_NODE_ID and GNMI_NODE_WITH_WRONG_PASSWD_ID - this cleanup is there
        as a failsafe to ensure that devices will be disconnected when some test fails and assert with disconnection
        wont be reached in the test in that case
        */
        try {
            final HttpResponse<String> getGnmiTopologyResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
            if (getGnmiTopologyResponse.body().contains(ANOTHER_GNMI_NODE_ID)) {
                if (!disconnectDevice(ANOTHER_GNMI_NODE_ID)) {
                    LOG.info("Problem when disconnecting device {}", ANOTHER_GNMI_NODE_ID);
                }
            }
            if (getGnmiTopologyResponse.body().contains(GNMI_NODE_WITH_WRONG_PASSWD_ID)) {
                if (!disconnectDevice(GNMI_NODE_WITH_WRONG_PASSWD_ID)) {
                    LOG.info("Problem when disconnecting device {}", GNMI_NODE_WITH_WRONG_PASSWD_ID);
                }
            }
            if (getGnmiTopologyResponse.body().contains(GNMI_NODE_MISSING_ENCODING_ID)) {
                if (!disconnectDevice(GNMI_NODE_MISSING_ENCODING_ID)) {
                    LOG.info("Problem when disconnecting device {}", GNMI_NODE_MISSING_ENCODING_ID);
                }
            }
        } catch (ExecutionException | InterruptedException | TimeoutException | IOException e) {
            LOG.info("Problem when disconnecting devices {}, {}, {}: ",
                    ANOTHER_GNMI_NODE_ID, GNMI_NODE_WITH_WRONG_PASSWD_ID, GNMI_NODE_MISSING_ENCODING_ID, e);
        }
    }

    @Test
    public void connectDeviceCorrectlyTest()
            throws InterruptedException, IOException, ExecutionException, TimeoutException, JSONException {
        //assert existing and empty gnmi topology
        final HttpResponse<String> getGnmiTopologyResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getGnmiTopologyResponse.statusCode());
        final JSONArray topologies =
            new JSONObject(getGnmiTopologyResponse.body()).getJSONArray("network-topology:topology");
        assertEquals(1, topologies.length());
        final JSONObject gnmiTopologyJSON = topologies.getJSONObject(0);
        LOG.info("Empty gnmi-topology check response: {}", gnmiTopologyJSON);
        assertEquals("gnmi-topology", gnmiTopologyJSON.getString("topology-id"));
        assertThrows(JSONException.class, () -> gnmiTopologyJSON.getJSONArray("node"));

        // add gNMI node to topology
        final String newDevicePayload = createDevicePayload(GNMI_NODE_ID, DEVICE_IP, DEVICE_PORT);
        LOG.info("Adding gnmi device with ID {}", GNMI_NODE_ID);
        final HttpResponse<String> addGnmiDeviceResponse = sendPutRequestJSON(GNMI_NODE_PATH, newDevicePayload);
        assertEquals(HttpURLConnection.HTTP_CREATED, addGnmiDeviceResponse.statusCode());

        // assert gNMI node is connected
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
            .pollInterval(POLL_INTERVAL_DURATION)
            .untilAsserted(() -> {
                final HttpResponse<String> getConnectionStatusResponse =
                    sendGetRequestJSON(GNMI_NODE_PATH  + GNMI_NODE_STATUS);
                assertEquals(HttpURLConnection.HTTP_OK, getConnectionStatusResponse.statusCode());
                final String gnmiDeviceConnectStatus =
                    new JSONObject(getConnectionStatusResponse.body()).getString("gnmi-topology:node-status");
                LOG.info("Response: {}", gnmiDeviceConnectStatus);
                assertEquals(GNMI_NODE_STATUS_READY, gnmiDeviceConnectStatus);
            });

        //assert mountpoint is created
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
            .pollInterval(POLL_INTERVAL_DURATION)
            .untilAsserted(() -> {
                final HttpResponse<String> getDataFromDevice =
                    sendGetRequestJSON(GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES);
                assertEquals(HttpURLConnection.HTTP_OK, getDataFromDevice.statusCode());
            });

        //assert gnmi test node is in topology
        final HttpResponse<String> getGnmiTopologyUpdatedResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getGnmiTopologyUpdatedResponse.statusCode());
        final String nodeIdFromTopology = new JSONObject(getGnmiTopologyUpdatedResponse.body())
            .getJSONArray("network-topology:topology").getJSONObject(0)
            .getJSONArray("node").getJSONObject(0).getString("node-id");
        assertEquals(GNMI_NODE_ID, nodeIdFromTopology);

        //assert disconnected device
        assertTrue(disconnectDevice(GNMI_NODE_ID));
    }

    @Test
    public void connectDeviceWithForceCapabilityAndModelTest()
            throws InterruptedException, IOException, ExecutionException, TimeoutException, JSONException {
        final HttpResponse<String> getGnmiTopologyResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getGnmiTopologyResponse.statusCode());
        final JSONArray topologies =
            new JSONObject(getGnmiTopologyResponse.body()).getJSONArray("network-topology:topology");
        assertEquals(1, topologies.length());
        final JSONObject gnmiTopologyJSON = topologies.getJSONObject(0);
        assertEquals("gnmi-topology", gnmiTopologyJSON.getString("topology-id"));
        assertThrows(JSONException.class, () -> gnmiTopologyJSON.getJSONArray("node"),
            String.format("Gnmi-topology is not empty: {}", gnmiTopologyJSON));

        // add gNMI node to topology
        final String newDevicePayload =
            createDevicePayloadWithAdditionalCapabilities(GNMI_NODE_ID, DEVICE_IP, DEVICE_PORT,
                MODEL_OPENCONFIG_AAA_NAME, MODEL_OPENCONFIG_AAA_VERSION);
        LOG.info("Adding gnmi device with ID {}", GNMI_NODE_ID);
        final HttpResponse<String> addGnmiDeviceResponse = sendPutRequestJSON(GNMI_NODE_PATH, newDevicePayload);
        assertEquals(HttpURLConnection.HTTP_CREATED, addGnmiDeviceResponse.statusCode());

        // assert gNMI node is connected
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
            .pollInterval(POLL_INTERVAL_DURATION)
            .untilAsserted(() -> {
                final HttpResponse<String> capabilitiesResponse =
                    sendGetRequestJSON(GET_CAPABILITIES_PATH);
                assertEquals(HttpURLConnection.HTTP_OK, capabilitiesResponse.statusCode());
                final JSONArray gnmiDeviceCapabilities = new JSONObject(capabilitiesResponse.body())
                    .getJSONObject("gnmi-topology:available-capabilities").getJSONArray("available-capability");
                assertEquals(EXPECTED_CAPABILITY, gnmiDeviceCapabilities.toString());
            });

        //assert disconnected device
        assertTrue(disconnectDevice(GNMI_NODE_ID));
    }

    @Test
    public void connectDeviceWithForceCapabilityWithNotImportedYangModelTest()
            throws InterruptedException, IOException, ExecutionException, TimeoutException, JSONException {
        final HttpResponse<String> getGnmiTopologyResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getGnmiTopologyResponse.statusCode());
        final JSONArray topologies =
            new JSONObject(getGnmiTopologyResponse.body()).getJSONArray("network-topology:topology");
        assertEquals(1, topologies.length());
        final JSONObject gnmiTopologyJSON = topologies.getJSONObject(0);
        assertEquals("gnmi-topology", gnmiTopologyJSON.getString("topology-id"));
        assertThrows(JSONException.class, () -> gnmiTopologyJSON.getJSONArray("node"),
            String.format("Gnmi-topology is not empty: {}", gnmiTopologyJSON));

        final String modelName = "not-imported-model-name";
        final String nodeState = "gnmi-topology:node-state";
        // add gNMI node to topology
        final String newDevicePayload =
            createDevicePayloadWithAdditionalCapabilities(GNMI_NODE_ID, DEVICE_IP, DEVICE_PORT,
                modelName, MODEL_OPENCONFIG_AAA_VERSION);
        LOG.info("Adding gnmi device with ID {}", GNMI_NODE_ID);
        final HttpResponse<String> addGnmiDeviceResponse = sendPutRequestJSON(GNMI_NODE_PATH, newDevicePayload);
        assertEquals(HttpURLConnection.HTTP_CREATED, addGnmiDeviceResponse.statusCode());

        // assert gNMI node is connected
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
            .pollInterval(POLL_INTERVAL_DURATION)
            .untilAsserted(() -> {
                final HttpResponse<String> capabilitiesResponse =
                    sendGetRequestJSON(GNMI_NODE_PATH  + "/" + nodeState);
                assertEquals(HttpURLConnection.HTTP_OK, capabilitiesResponse.statusCode());
                final String gnmiDeviceConnectStatus =
                    new JSONObject(capabilitiesResponse.body()).getJSONObject(nodeState).getString(
                        "node-status");
                final String gnmiDeviceFailureDetails =
                    new JSONObject(capabilitiesResponse.body()).getJSONObject(nodeState).getString(
                        "failure-details");
                assertEquals("FAILURE", gnmiDeviceConnectStatus);
                assertTrue(gnmiDeviceFailureDetails.contains(modelName));
            });

        //assert disconnected device
        assertTrue(disconnectDevice(GNMI_NODE_ID));
    }

    @Test
    public void connectDeviceIncorrectlyTest()
            throws InterruptedException, IOException, ExecutionException, TimeoutException, JSONException {
        //assert existing and empty gnmi topology
        final HttpResponse<String> getGnmiTopologyResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getGnmiTopologyResponse.statusCode());
        final JSONArray topologies =
            new JSONObject(getGnmiTopologyResponse.body()).getJSONArray("network-topology:topology");
        assertEquals(1, topologies.length());
        final JSONObject gnmiTopologyJSON = topologies.getJSONObject(0);
        LOG.info("Response: {}", gnmiTopologyJSON);
        assertEquals("gnmi-topology", gnmiTopologyJSON.getString("topology-id"));
        assertThrows(JSONException.class, () -> gnmiTopologyJSON.getJSONArray("node"));

        //add gnmi node to gnmi topology
        final String newDeviceIncorrectPayload = createDevicePayload(GNMI_NODE_ID, DEVICE_IP, DEVICE_PORT - 1);
        final HttpResponse<String> addGnmiDeviceResponse =
            sendPutRequestJSON(GNMI_NODE_PATH, newDeviceIncorrectPayload);
        assertEquals(HttpURLConnection.HTTP_CREATED, addGnmiDeviceResponse.statusCode());

        //assert gnmi node is created in gnmi topology
        final HttpResponse<String> getGnmiTopologyUpdatedResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getGnmiTopologyUpdatedResponse.statusCode());
        final JSONArray gnmiTopologyNodesJSONArray = new JSONObject(getGnmiTopologyUpdatedResponse.body())
            .getJSONArray("network-topology:topology").getJSONObject(0).getJSONArray("node");
        assertEquals(1, gnmiTopologyNodesJSONArray.length());
        final String gnmiNodeIdFromJSONArray = gnmiTopologyNodesJSONArray.getJSONObject(0).getString("node-id");
        assertEquals(GNMI_NODE_ID, gnmiNodeIdFromJSONArray);

        //assert connecting or transient failure status while trying to connect maxAttempts times
        //and assert number of attempts also
        final AtomicInteger attempt = new AtomicInteger();
        Awaitility.waitAtMost(CONNECT_ATTEMPT_WAIT_DURATION)
            .pollInterval(POLL_INTERVAL_DURATION)
            .untilAsserted(() -> {
                attempt.getAndIncrement();
                final HttpResponse<String> getDeviceConnectStatusResponse =
                    sendGetRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_ID + GNMI_NODE_STATUS);
                final String deviceConnectStatus =
                    new JSONObject(getDeviceConnectStatusResponse.body()).getString("gnmi-topology:node-status");
                LOG.info("Attempt {}, response: {}", attempt, deviceConnectStatus);
                assertTrue(deviceConnectStatus.equals(GNMI_NODE_STATUS_CONNECTING)
                           || deviceConnectStatus.equals(GNMI_NODE_STATUS_TRANSIENT_FAIL));
                assertTrue(attempt.get() <= MAX_DEVICE_CONNECTION_ATTEMPTS);
            });

        //assert disconnected device
        assertTrue(disconnectDevice(GNMI_NODE_ID));
    }

    @Test
    public void disconnectDeviceTest() throws InterruptedException, IOException {
        assertTrue(connectDevice(GNMI_NODE_ID, DEVICE_IP, DEVICE_PORT));

        final HttpResponse<String> deleteGnmiDeviceResponse = sendDeleteRequestJSON(GNMI_NODE_PATH);
        LOG.info("Response: {}", deleteGnmiDeviceResponse.body());
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, deleteGnmiDeviceResponse.statusCode());

        // assert gNMI node is disconnected - gnmi-topology is empty
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
            .pollInterval(POLL_INTERVAL_DURATION)
            .untilAsserted(() -> {
                final HttpResponse<String> getGnmiTopologyResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
                assertEquals(HttpURLConnection.HTTP_OK, getGnmiTopologyResponse.statusCode());
                final JSONObject gnmiTopology = new JSONObject(getGnmiTopologyResponse.body())
                    .getJSONArray("network-topology:topology").getJSONObject(0);
                assertThrows(JSONException.class, () -> gnmiTopology.getJSONArray("node"));
            });
    }

    @Test
    public void connectMultipleDevicesTest()
            throws IOException, InterruptedException, ExecutionException, TimeoutException, JSONException {
        //assert existing and empty gnmi topology
        final HttpResponse<String> getGnmiTopologyResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
        final JSONArray topologies =
            new JSONObject(getGnmiTopologyResponse.body()).getJSONArray("network-topology:topology");
        assertEquals(1, topologies.length());
        final JSONObject gnmiTopologyJSON = topologies.getJSONObject(0);
        LOG.info("Empty gnmi-topology check response: {}", gnmiTopologyJSON);
        assertEquals(HttpURLConnection.HTTP_OK, getGnmiTopologyResponse.statusCode());
        assertEquals("gnmi-topology", gnmiTopologyJSON.getString("topology-id"));
        assertThrows(JSONException.class, () -> gnmiTopologyJSON.getJSONArray("node"));

        final HttpResponse<String> addGnmiDeviceResponse =
                sendPutRequestJSON(GNMI_TOPOLOGY_PATH, MULTIPLE_DEVICES_PAYLOAD);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, addGnmiDeviceResponse.statusCode());

        // assert gNMI nodes are connected
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
            .pollInterval(POLL_INTERVAL_DURATION)
            .untilAsserted(() -> {
                final HttpResponse<String> getNodeConnectionStatusResponse =
                    sendGetRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_ID + GNMI_NODE_STATUS);
                final String gnmiDeviceConnectStatus =
                    new JSONObject(getNodeConnectionStatusResponse.body()).getString("gnmi-topology:node-status");
                LOG.info("Response: {}", gnmiDeviceConnectStatus);
                assertEquals(HttpURLConnection.HTTP_OK, getNodeConnectionStatusResponse.statusCode());
                assertEquals(GNMI_NODE_STATUS_READY, gnmiDeviceConnectStatus);
                //
                final HttpResponse<String> getOtherNodeConnectionStatusResponse =
                    sendGetRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + ANOTHER_GNMI_NODE_ID + GNMI_NODE_STATUS);
                final String otherGnmiDeviceConnectStatus =
                    new JSONObject(getOtherNodeConnectionStatusResponse.body()).getString("gnmi-topology:node-status");
                LOG.info("Response: {}", otherGnmiDeviceConnectStatus);
                assertEquals(HttpURLConnection.HTTP_OK, getOtherNodeConnectionStatusResponse.statusCode());
                assertEquals(GNMI_NODE_STATUS_READY, otherGnmiDeviceConnectStatus);
            });

        //assert mountpoints are created
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
            .pollInterval(POLL_INTERVAL_DURATION)
            .untilAsserted(() -> {
                final HttpResponse<String> getDataFromDevice =
                    sendGetRequestJSON(GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES);
                assertEquals(HttpURLConnection.HTTP_OK, getDataFromDevice.statusCode());
                final HttpResponse<String> getDataFromOtherDevice =
                    sendGetRequestJSON(ANOTHER_GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES);
                assertEquals(HttpURLConnection.HTTP_OK, getDataFromOtherDevice.statusCode());
            });

        //assert disconnected devices
        assertTrue(disconnectDevice(GNMI_NODE_ID));
        assertTrue(disconnectDevice(ANOTHER_GNMI_NODE_ID));
    }

    @Test
    public void disconnectMultipleDeviceTest() throws InterruptedException, IOException {
        assertTrue(connectDevice(GNMI_NODE_ID, DEVICE_IP, DEVICE_PORT));
        assertTrue(connectDevice(ANOTHER_GNMI_NODE_ID, DEVICE_IP, ANOTHER_DEVICE_PORT));

        final HttpResponse<String> deleteGnmiDevicesResponse = sendDeleteRequestJSON(GNMI_TOPOLOGY_PATH);
        LOG.info("Response: {}", deleteGnmiDevicesResponse.body());
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, deleteGnmiDevicesResponse.statusCode());

        // assert gNMI nodes are disconnected - gnmi-topology is empty
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
            .pollInterval(POLL_INTERVAL_DURATION)
            .untilAsserted(() -> {
                final HttpResponse<String> getGnmiTopologyResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
                final JSONObject gnmiTopology = new JSONObject(getGnmiTopologyResponse.body())
                    .getJSONArray("network-topology:topology").getJSONObject(0);
                assertEquals(HttpURLConnection.HTTP_OK, getGnmiTopologyResponse.statusCode());
                assertThrows(JSONException.class, () -> gnmiTopology.getJSONArray("node"));
            });
    }

    @Test
    public void reconnectDeviceWithRequestsMultipleTimesTest()
        throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final int maxReconnections = 5;
        for (int i = 0; i < maxReconnections; i++) {
            //connect device
            assertTrue(connectDevice(GNMI_NODE_ID, DEVICE_IP, DEVICE_PORT));
            //make correct and incorrect get and set requests
            final HttpResponse<String> getDataFromDevice =
                sendGetRequestJSON(GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES);
            assertEquals(HttpURLConnection.HTTP_OK, getDataFromDevice.statusCode());
            final HttpResponse<String> getIncorrectDataFromDevice =
                sendGetRequestJSON(GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES + "incorrect");
            assertEquals(HttpURLConnection.HTTP_CONFLICT, getIncorrectDataFromDevice.statusCode());

            final HttpResponse<String> setDataOnDevice =
                sendPutRequestJSON(GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES + "/interface=eth3/config/name",
                    INTERFACE_ETH3_CONFIG_NAME_PAYLOAD);
            assertEquals(HttpURLConnection.HTTP_NO_CONTENT, setDataOnDevice.statusCode());
            final HttpResponse<String> setDataIncorrectlyOnDevice =
                sendPutRequestJSON(GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES + "/interface=eth3/config/badName",
                    INTERFACE_ETH3_CONFIG_NAME_PAYLOAD);
            assertEquals(HttpURLConnection.HTTP_CONFLICT, setDataIncorrectlyOnDevice.statusCode());

            //disconnect device
            assertTrue(disconnectDevice(GNMI_NODE_ID));
        }
    }

    @Test
    public void reconnectIncorrectlyConnectedDeviceTest()
            throws IOException, InterruptedException, ExecutionException, TimeoutException, JSONException {
        //assert existing and empty gnmi topology
        final HttpResponse<String> getGnmiTopologyResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
        final JSONArray topologies =
            new JSONObject(getGnmiTopologyResponse.body()).getJSONArray("network-topology:topology");
        assertEquals(1, topologies.length());
        final JSONObject gnmiTopologyJSON = topologies.getJSONObject(0);
        LOG.info("Response: {}", gnmiTopologyJSON);
        assertEquals(HttpURLConnection.HTTP_OK, getGnmiTopologyResponse.statusCode());
        assertEquals("gnmi-topology", gnmiTopologyJSON.getString("topology-id"));
        assertThrows(JSONException.class, () -> gnmiTopologyJSON.getJSONArray("node"));

        //add gnmi node to gnmi topology
        final String newDeviceIncorrectPayload = createDevicePayload(GNMI_NODE_ID, DEVICE_IP, DEVICE_PORT - 1);
        final HttpResponse<String> addGnmiDeviceResponse =
            sendPutRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_ID, newDeviceIncorrectPayload);
        assertEquals(HttpURLConnection.HTTP_CREATED, addGnmiDeviceResponse.statusCode());

        //assert gnmi node is created in gnmi topology
        final HttpResponse<String> getGnmiTopologyUpdatedResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getGnmiTopologyUpdatedResponse.statusCode());
        final JSONArray gnmiTopologyNodesJSONArray = new JSONObject(getGnmiTopologyUpdatedResponse.body())
            .getJSONArray("network-topology:topology").getJSONObject(0).getJSONArray("node");
        assertEquals(1, gnmiTopologyNodesJSONArray.length());
        final String gnmiNodeIdFromJSONArray = gnmiTopologyNodesJSONArray.getJSONObject(0).getString("node-id");
        assertEquals(GNMI_NODE_ID, gnmiNodeIdFromJSONArray);

        //assert connecting or transient failure status while trying to connect maxAttempts times
        //and assert number of attempts also
        final AtomicInteger attempt = new AtomicInteger();
        Awaitility.waitAtMost(CONNECT_ATTEMPT_WAIT_DURATION)
            .pollInterval(POLL_INTERVAL_DURATION)
            .untilAsserted(() -> {
                attempt.getAndIncrement();
                final HttpResponse<String> getDeviceConnectStatusResponse =
                    sendGetRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_ID + GNMI_NODE_STATUS);
                final String deviceConnectStatus =
                    new JSONObject(getDeviceConnectStatusResponse.body()).getString("gnmi-topology:node-status");
                LOG.info("Attempt {}, response: {}", attempt, deviceConnectStatus);
                assertTrue(deviceConnectStatus.equals(GNMI_NODE_STATUS_CONNECTING)
                    || deviceConnectStatus.equals(GNMI_NODE_STATUS_TRANSIENT_FAIL));
                assertTrue(attempt.get() <= MAX_DEVICE_CONNECTION_ATTEMPTS);
            });

        //update incorrect gnmi node in gnmi topology
        final String updatedDevicePayload = createDevicePayload(GNMI_NODE_ID, DEVICE_IP, DEVICE_PORT);
        final HttpResponse<String> updateGnmiDeviceResponse =
            sendPutRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_ID, updatedDevicePayload);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, updateGnmiDeviceResponse.statusCode());

        // assert gNMI node is connected
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
            .pollInterval(POLL_INTERVAL_DURATION)
            .untilAsserted(() -> {
                final HttpResponse<String> getConnectionStatusResponse =
                    sendGetRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_ID + GNMI_NODE_STATUS);
                final String gnmiDeviceConnectStatus =
                    new JSONObject(getConnectionStatusResponse.body()).getString("gnmi-topology:node-status");
                LOG.info("Response: {}", gnmiDeviceConnectStatus);
                assertEquals(HttpURLConnection.HTTP_OK, getConnectionStatusResponse.statusCode());
                assertEquals(GNMI_NODE_STATUS_READY, gnmiDeviceConnectStatus);
            });

        //assert mountpoint is created
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
            .pollInterval(POLL_INTERVAL_DURATION)
            .untilAsserted(() -> {
                final HttpResponse<String> getDataFromDevice =
                    sendGetRequestJSON(GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES);
                assertEquals(HttpURLConnection.HTTP_OK, getDataFromDevice.statusCode());
            });

        //assert disconnected device
        assertTrue(disconnectDevice(GNMI_NODE_ID));
    }

    @Test
    public void connectDeviceWithIncorrectCredentialsTest()
            throws IOException, InterruptedException, ExecutionException, TimeoutException, JSONException {
        //assert existing and empty gnmi topology
        final HttpResponse<String> getGnmiTopologyResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getGnmiTopologyResponse.statusCode());
        final JSONArray topologies =
            new JSONObject(getGnmiTopologyResponse.body()).getJSONArray("network-topology:topology");
        assertEquals(1, topologies.length());
        final JSONObject gnmiTopologyJSON = topologies.getJSONObject(0);
        LOG.info("Empty gnmi-topology check response: {}", gnmiTopologyJSON);
        assertEquals("gnmi-topology", gnmiTopologyJSON.getString("topology-id"));
        assertThrows(JSONException.class, () -> gnmiTopologyJSON.getJSONArray("node"));

        // add gNMI node with wrong password to topology
        LOG.info("Adding gnmi device with ID {}", GNMI_NODE_WITH_WRONG_PASSWD_ID);
        final HttpResponse<String> addGnmiDeviceResponse =
            sendPutRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_WITH_WRONG_PASSWD_ID,
                               DEVICE_WITH_CREDENTIALS_PAYLOAD);
        LOG.info("adding device wrong credentials: {}", addGnmiDeviceResponse.body());
        assertEquals(HttpURLConnection.HTTP_CREATED, addGnmiDeviceResponse.statusCode());

        // assert gNMI node is not connected correctly due to wrong password
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
            .pollInterval(POLL_INTERVAL_DURATION)
            .untilAsserted(() -> {
                final HttpResponse<String> getConnectionStatusResponse =
                    sendGetRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_WITH_WRONG_PASSWD_ID
                                       + GNMI_NODE_STATUS);
                assertEquals(HttpURLConnection.HTTP_OK, getConnectionStatusResponse.statusCode());
                final String gnmiDeviceConnectStatus =
                    new JSONObject(getConnectionStatusResponse.body()).getString("gnmi-topology:node-status");
                LOG.info("Response: {}", gnmiDeviceConnectStatus);
                assertNotEquals(GNMI_NODE_STATUS_READY, gnmiDeviceConnectStatus);
                final HttpResponse<String> getDeviceFailureDetailsResponse =
                    sendGetRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_WITH_WRONG_PASSWD_ID
                        + "/gnmi-topology:node-state/failure-details");
                final String gnmiDeviceFailureDetails =
                    new JSONObject(getDeviceFailureDetailsResponse.body()).getString("gnmi-topology:failure-details");
                LOG.info("Response: {}", gnmiDeviceFailureDetails);
                assertTrue(gnmiDeviceFailureDetails.contains("UNAUTHENTICATED"));
            });

        assertTrue(disconnectDevice(GNMI_NODE_WITH_WRONG_PASSWD_ID));

        // assert gNMI node's node-state is also deleted
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
            .pollInterval(POLL_INTERVAL_DURATION)
            .untilAsserted(() -> {
                final HttpResponse<String> getConnectionStatusResponse =
                    sendGetRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_WITH_WRONG_PASSWD_ID
                        + "/gnmi-topology:node-state");
                assertEquals(HttpURLConnection.HTTP_CONFLICT, getConnectionStatusResponse.statusCode());
            });
    }

    @Test
    public void connectDeviceWithMissingEncodingTest()
            throws IOException, InterruptedException, ExecutionException, TimeoutException, JSONException {
        //assert existing and empty gnmi topology
        final HttpResponse<String> getGnmiTopologyResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getGnmiTopologyResponse.statusCode());
        final JSONArray topologies =
                new JSONObject(getGnmiTopologyResponse.body()).getJSONArray("network-topology:topology");
        assertEquals(1, topologies.length());
        final JSONObject gnmiTopologyJSON = topologies.getJSONObject(0);
        LOG.info("Empty gnmi-topology check response: {}", gnmiTopologyJSON);
        assertEquals("gnmi-topology", gnmiTopologyJSON.getString("topology-id"));
        assertThrows(JSONException.class, () -> gnmiTopologyJSON.getJSONArray("node"));

        // add gNMI node with missing encoding to topology
        LOG.info("Adding gnmi device with ID {}", GNMI_NODE_MISSING_ENCODING_ID);
        final HttpResponse<String> addGnmiDeviceResponse =
                sendPutRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_MISSING_ENCODING_ID,
                        createDevicePayload(
                                GNMI_NODE_MISSING_ENCODING_ID, DEVICE_IP, DEVICE_WITH_MISSING_ENCODING_PORT));
        LOG.info("adding device with missing JSON_IETF encoding: {}", addGnmiDeviceResponse.body());
        assertEquals(HttpURLConnection.HTTP_CREATED, addGnmiDeviceResponse.statusCode());

        // assert gNMI node is not connected correctly due to missing JSON_IETF_ENCODING
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    final HttpResponse<String> getConnectionStatusResponse =
                            sendGetRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_MISSING_ENCODING_ID
                                    + GNMI_NODE_STATUS);
                    assertEquals(HttpURLConnection.HTTP_OK, getConnectionStatusResponse.statusCode());
                    final String gnmiDeviceConnectStatus =
                            new JSONObject(getConnectionStatusResponse.body())
                                    .getString("gnmi-topology:node-status");
                    LOG.info("Response: {}", gnmiDeviceConnectStatus);
                    assertNotEquals(GNMI_NODE_STATUS_READY, gnmiDeviceConnectStatus);
                    final HttpResponse<String> getDeviceFailureDetailsResponse =
                            sendGetRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_MISSING_ENCODING_ID
                                    + "/gnmi-topology:node-state/failure-details");
                    final String gnmiDeviceFailureDetails =
                            new JSONObject(getDeviceFailureDetailsResponse.body())
                                    .getString("gnmi-topology:failure-details");
                    LOG.info("Response: {}", gnmiDeviceFailureDetails);
                    assertTrue(gnmiDeviceFailureDetails.contains("JSON_IETF encoding"));
                });

        assertTrue(disconnectDevice(GNMI_NODE_MISSING_ENCODING_ID));

        // assert gNMI node's node-state is also deleted
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    final HttpResponse<String> getConnectionStatusResponse =
                            sendGetRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_MISSING_ENCODING_ID
                                    + "/gnmi-topology:node-state");
                    assertEquals(HttpURLConnection.HTTP_CONFLICT, getConnectionStatusResponse.statusCode());
                });
    }

}
