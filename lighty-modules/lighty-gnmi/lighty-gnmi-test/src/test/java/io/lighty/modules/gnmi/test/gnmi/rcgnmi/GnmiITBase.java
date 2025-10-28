/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.test.gnmi.rcgnmi;

import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_NODE_ID;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_NODE_STATUS;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_NODE_STATUS_READY;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_TOPOLOGY_PATH;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.OPENCONFIG_INTERFACES;

import gnmi.Gnmi;
import io.lighty.applications.rcgnmi.app.RCgNMIApp;
import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.utils.GnmiSimulatorConfUtils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GnmiITBase {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiITBase.class);

    protected static final int DEVICE_PORT = 9090;
    protected static final String DEVICE_IP = "127.0.0.1";
    protected static final Duration REQUEST_TIMEOUT_DURATION = Duration.ofMillis(10_000L);
    protected static final Duration POLL_INTERVAL_DURATION = Duration.ofMillis(1_000L);
    protected static final Duration WAIT_TIME_DURATION = Duration.ofMillis(10_000L);

    protected static final String INITIAL_JSON_DATA_PATH = "src/test/resources/json/initData";
    private static final String TEST_SCHEMA_PATH = "src/test/resources/additional/models";
    private static final String SIMULATOR_CONFIG = "/json/simulator_config.json";

    protected static ExecutorService httpClientExecutor;
    protected static RCgNMIApp application;
    protected static HttpClient httpClient;

    @BeforeAll
    public static void setup() {
        httpClientExecutor = Executors.newSingleThreadExecutor();
        httpClient = HttpClient.newBuilder().executor(httpClientExecutor).build();

        application = new RCgNMIApp();
        application.start(new String[]{"-c", "src/test/resources/json/app_init_config.json"});
    }

    @AfterAll
    public static void teardown() {
        httpClientExecutor.shutdownNow();
        application.stop();
    }

    @AfterEach
    public void cleanup() {
        LOG.info("Performing cleanup!");
        /*
        disconnect device GNMI_NODE_ID after each test in all of inherited classes
        even when in the end of some tests there is assert disconnecting device, it needs to be there
        as a failsafe to ensure when some test fails that device will be disconnected and wont affect other tests
        */
        try {
            final HttpResponse<String> getGnmiTopologyResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH);
            if (getGnmiTopologyResponse.body().contains(GNMI_NODE_ID)) {
                if (!disconnectDevice(GNMI_NODE_ID)) {
                    LOG.info("Problem when disconnecting device {}", GNMI_NODE_ID);
                }
            }
        } catch (ExecutionException | InterruptedException | TimeoutException | IOException e) {
            LOG.info("Problem when disconnecting device {}: {}", GNMI_NODE_ID, e);
        }
        LOG.info("Cleanup done!");
    }

    protected static SimulatedGnmiDevice getUnsecureGnmiDevice(final String host, final int port) {

        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(GnmiITBase.class.getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfiguration.setTargetAddress(host);
        simulatorConfiguration.setTargetPort(port);
        simulatorConfiguration.setYangsPath(TEST_SCHEMA_PATH);
        simulatorConfiguration.setInitialConfigDataPath(INITIAL_JSON_DATA_PATH + "/config.json");
        simulatorConfiguration.setInitialStateDataPath(INITIAL_JSON_DATA_PATH + "/state.json");

        return new SimulatedGnmiDevice(simulatorConfiguration);
    }

    protected static SimulatedGnmiDevice getUnsecureGnmiDevice(final String host, final int port,
                                                              final String username, final String password) {
        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(GnmiITBase.class.getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfiguration.setTargetAddress(host);
        simulatorConfiguration.setTargetPort(port);
        simulatorConfiguration.setYangsPath(TEST_SCHEMA_PATH);
        simulatorConfiguration.setInitialConfigDataPath(INITIAL_JSON_DATA_PATH + "/config.json");
        simulatorConfiguration.setInitialStateDataPath(INITIAL_JSON_DATA_PATH + "/state.json");
        simulatorConfiguration.setUsername(username);
        simulatorConfiguration.setPassword(password);

        return new SimulatedGnmiDevice(simulatorConfiguration);
    }

    protected static SimulatedGnmiDevice getNonCompliableEncodingDevice(final String host, final int port) {
        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(GnmiITBase.class.getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfiguration.setTargetAddress(host);
        simulatorConfiguration.setTargetPort(port);
        simulatorConfiguration.setYangsPath(TEST_SCHEMA_PATH);
        simulatorConfiguration.setInitialConfigDataPath(INITIAL_JSON_DATA_PATH + "/config.json");
        simulatorConfiguration.setInitialStateDataPath(INITIAL_JSON_DATA_PATH + "/state.json");
        simulatorConfiguration.setSupportedEncodings(EnumSet.of(Gnmi.Encoding.JSON));

        return new SimulatedGnmiDevice(simulatorConfiguration);
    }

    protected static SimulatedGnmiDevice getSecureGnmiDevice(final String host, final int port,
                                                             final String keyPath, final String certPath,
                                                             final String username, final String password) {
        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(GnmiITBase.class.getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfiguration.setTargetAddress(host);
        simulatorConfiguration.setTargetPort(port);
        simulatorConfiguration.setYangsPath(TEST_SCHEMA_PATH);
        simulatorConfiguration.setInitialConfigDataPath(INITIAL_JSON_DATA_PATH + "/config.json");
        simulatorConfiguration.setInitialStateDataPath(INITIAL_JSON_DATA_PATH + "/state.json");
        simulatorConfiguration.setCertPath(certPath);
        simulatorConfiguration.setCertKeyPath(keyPath);
        simulatorConfiguration.setUsername(username);
        simulatorConfiguration.setPassword(password);

        return new SimulatedGnmiDevice(simulatorConfiguration);
    }

    protected boolean connectDevice(final String nodeId, final String ipAddr, final int port)
        throws InterruptedException, IOException {
        LOG.info("Connecting device!");
        //check there is not present device with nodeId in gnmi-topology topology
        final HttpResponse<String> getGnmiNodeResponse = sendGetRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + nodeId);
        if (getGnmiNodeResponse.statusCode() == HttpURLConnection.HTTP_OK) {
            LOG.info("Gnmi node {} is already in the topology", nodeId);
            return false;
        }

        // add gNMI node to topology
        final String newDevicePayload = createDevicePayload(nodeId, ipAddr, port);
        LOG.info("Adding gnmi device with ID {} on IP ADDRESS:PORT {}:{}", nodeId, ipAddr, port);
        final HttpResponse<String> addGnmiDeviceResponse =
            sendPutRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + nodeId, newDevicePayload);
        if (addGnmiDeviceResponse.statusCode() != HttpURLConnection.HTTP_CREATED) {
            LOG.info("Problem when adding node {} into gnmi topology: {}y", nodeId, addGnmiDeviceResponse);
            return false;
        }

        // check if gNMI node is connected
        try {
            Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .until(() -> {
                    final HttpResponse<String> getConnectionStatusResponse =
                        sendGetRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + nodeId + GNMI_NODE_STATUS);
                    if (getConnectionStatusResponse.statusCode() != HttpURLConnection.HTTP_OK) {
                        return false;
                    }
                    final String gnmiDeviceConnectStatus = new JSONObject(
                        getConnectionStatusResponse.body()).getString("gnmi-topology:node-status");
                    LOG.info("Check node {} connection status response: {}", nodeId, gnmiDeviceConnectStatus);
                    return gnmiDeviceConnectStatus.equals(GNMI_NODE_STATUS_READY);
                });
        } catch (ConditionTimeoutException e) {
            LOG.info("Failure during connecting the device - gnmi node status is not READY!");
            return false;
        }

        //check if mountpoint is created
        try {
            Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .until(() -> {
                    final HttpResponse<String> getDataFromDevice = sendGetRequestJSON(GNMI_TOPOLOGY_PATH
                        + "/node=" + nodeId + "/yang-ext:mount" + OPENCONFIG_INTERFACES);
                    LOG.info("Check mountpoint for node {} is created response {}", nodeId, getDataFromDevice);
                    return getDataFromDevice.statusCode() == HttpURLConnection.HTTP_OK;
                });
        } catch (ConditionTimeoutException e) {
            LOG.info("Failure during connecting the device - mountpoint is not created or unreachable!");
            return false;
        }

        LOG.info("Device successfully connected!");
        return true;
    }

    protected boolean disconnectDevice(final String nodeId) throws ExecutionException, InterruptedException,
        TimeoutException, IOException {
        LOG.info("Disconnecting device!");
        final HttpResponse<String> deleteGnmiDeviceResponse =
            sendDeleteRequestJSON(GNMI_TOPOLOGY_PATH + "/node=" + nodeId);
        LOG.info("Delete gnmi node {} response: {}", nodeId, deleteGnmiDeviceResponse);
        if (deleteGnmiDeviceResponse.statusCode() != HttpURLConnection.HTTP_NO_CONTENT) {
            LOG.info("Failure during disconnecting the device - node {} was not deleted: {}!", nodeId,
                deleteGnmiDeviceResponse);
            return false;
        }

        // check if gNMI node is disconnected - gnmi node is not present
        try {
            Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .until(() -> {
                    final HttpResponse<String> getGnmiNodeResponse = sendGetRequestJSON(
                        GNMI_TOPOLOGY_PATH + "/node=" + nodeId);
                    LOG.info("Get node {} from topology when disconnecting: {}", nodeId, getGnmiNodeResponse);
                    return HttpURLConnection.HTTP_CONFLICT == getGnmiNodeResponse.statusCode();
                });
        } catch (ConditionTimeoutException e) {
            LOG.info("Failure during disconnecting the device - node {} was not deleted!", nodeId);
            return false;
        }

        LOG.info("Device disconnected!");
        return true;
    }

    protected String createDevicePayload(final String nodeId, final String ipAddr, final int port) {
        return "{\n"
            + "    \"network-topology:node\" : [{\n"
            + "        \"node-id\": \"" + nodeId + "\",\n"
            + "        \"gnmi-topology:connection-parameters\": {\n"
            + "            \"host\": \"" + ipAddr + "\",\n"
            + "            \"port\": " + port + ",\n"
            + "            \"connection-type\": \"INSECURE\"\n"
            + "        },\n"
            + "        \"extensions-parameters\": {\n"
            + "            \"gnmi-parameters\": {\n"
            + "                \"use-model-name-prefix\": true\n"
            + "            }\n"
            + "        }"
            + "    }]\n"
            + "}";
    }

    protected String createDevicePayloadWithAdditionalCapabilities(final String nodeId, final String ipAddr,
                                                                   final int port, final String modelName,
                                                                   final String modelVersion) {
        return "{\n"
            + "    \"node\": [\n"
            + "        {\n"
            + "            \"node-id\": \"" + nodeId + "\",\n"
            + "            \"connection-parameters\": {\n"
            + "                \"host\": \"" + ipAddr + "\",\n"
            + "                \"port\": " + port + ",\n"
            + "                \"connection-type\": \"INSECURE\"\n"
            + "            },\n"
            + "            \"extensions-parameters\": {\n"
            + "                \"gnmi-parameters\": {\n"
            + "                    \"overwrite-data-type\": \"NONE\",\n"
            + "                    \"use-model-name-prefix\": true,\n"
            + "                    \"path-target\": \"OC_YANG\"\n"
            + "                },\n"
            + "                \"force-capability\": [\n"
            + "                    {\n"
            + "                        \"name\": \"" + modelName + "\",\n"
            + "                        \"version\": \"" + modelVersion + "\"\n"
            + "                    }\n"
            + "                ]\n"
            + "            }\n"
            + "        }\n"
            + "    ]\n"
            + "}";
    }

    protected HttpResponse<String> sendDeleteRequestJSON(final String path) throws InterruptedException, IOException {
        LOG.info("Sending DELETE request to path: {}", path);
        final HttpRequest deleteRequest = HttpRequest.newBuilder()
            .uri(URI.create(path))
            .header("Content-Type", "application/json")
            .DELETE()
            .timeout(REQUEST_TIMEOUT_DURATION)
            .build();
        return httpClient.send(deleteRequest, BodyHandlers.ofString());
    }

    protected HttpResponse<String> sendPutRequestJSON(final String path, final String payload)
        throws InterruptedException, IOException {
        LOG.info("Sending PUT request with {} payload to path: {}", payload, path);
        final HttpRequest putRequest = HttpRequest.newBuilder()
            .uri(URI.create(path))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .PUT(BodyPublishers.ofString(payload))
            .timeout(REQUEST_TIMEOUT_DURATION)
            .build();
        return httpClient.send(putRequest, BodyHandlers.ofString());
    }

    protected HttpResponse<String> sendPostRequestJSON(final String path, final String payload)
        throws InterruptedException, IOException {
        LOG.info("Sending POST request with {} payload to path: {}", payload, path);
        final HttpRequest postRequest = HttpRequest.newBuilder()
            .uri(URI.create(path))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(payload))
            .timeout(REQUEST_TIMEOUT_DURATION)
            .build();
        return httpClient.send(postRequest, BodyHandlers.ofString());
    }

    protected HttpResponse<String> sendGetRequestJSON(final String path) throws InterruptedException, IOException {
        LOG.info("Sending GET request to path: {}", path);
        final HttpRequest getRequest = HttpRequest.newBuilder()
            .uri(URI.create(path))
            .header("Content-Type", "application/json")
            .GET()
            .timeout(REQUEST_TIMEOUT_DURATION)
            .build();
        return httpClient.send(getRequest, BodyHandlers.ofString());
    }

    protected HttpResponse<String> sendPatchRequestJSON(final String path, final String payload)
            throws InterruptedException, IOException {
        LOG.info("Sending PATCH request to path: {}", path);
        final HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(path))
                .header("Content-Type", "application/json")
                .method("PATCH", BodyPublishers.ofString(payload))
                .timeout(REQUEST_TIMEOUT_DURATION)
                .build();
        return httpClient.send(getRequest, BodyHandlers.ofString());
    }

    protected static final class GeneralConstants {

        public static final String RESTCONF_DATA_PATH = "http://localhost:8888/restconf/data";
        public static final String GNMI_NODE_ID = "gnmi-node-test";
        public static final String GNMI_NODE_STATUS = "/gnmi-topology:node-state/node-status";
        public static final String GNMI_TOPOLOGY_PATH =
            RESTCONF_DATA_PATH + "/network-topology:network-topology/topology=gnmi-topology";
        public static final String GNMI_DEVICE_MOUNTPOINT =
            GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_ID + "/yang-ext:mount";
        public static final String OPENCONFIG_INTERFACES = "/openconfig-interfaces:interfaces";
        public static final String OPENCONFIG_OPENFLOW = "/openconfig-openflow:openflow";
        public static final String OPENCONFIG_SYSTEM = "/openconfig-system:system";
        public static final String GNMI_NODE_STATUS_READY = "READY";

        public static final String ERR_MSG_RELEVANT_MODEL_NOT_EXIST =
                "{\"errors\":{\"error\":[{\"error-message\":"
              + "\"Request could not be completed because the relevant data model content does not "
              + "exist\",\"error-tag\":\"data-missing\",\"error-type\":\"protocol\"}]}}";
        protected static final String INTERFACE_ETH3_CONFIG_NAME_PAYLOAD = "{\n"
            + "\"openconfig-interfaces:name\": \"updated-config-name\"\n"
            + "}";

        private GeneralConstants() {
            //Hide constructor
        }

    }

}
