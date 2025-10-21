/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.test.gnmi.rcgnmi;

import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.ERR_MSG_RELEVANT_MODEL_NOT_EXIST;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_DEVICE_MOUNTPOINT;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_NODE_ID;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_TOPOLOGY_PATH;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.OPENCONFIG_INTERFACES;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.OPENCONFIG_OPENFLOW;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.OPENCONFIG_SYSTEM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.utils.EffectiveModelContextBuilder.EffectiveModelContextBuilderException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnmiGetITTest extends GnmiITBase {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiGetITTest.class);
    private static final String GET_CAPABILITIES_PATH
            = GNMI_TOPOLOGY_PATH + "/node=" + GNMI_NODE_ID + "/gnmi-topology:node-state/available-capabilities";
    private static final String INTERFACES_PATH = GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES;
    private static final String OPENFLOW_PATH = GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_SYSTEM +  OPENCONFIG_OPENFLOW;
    private static final List<String> EXPECTED_CAPABILITIES = List.of(
        "iana-if-type revision: 2023-01-26", "openconfig-alarm-types semver: 0.2.1", "openconfig-alarms semver: 0.3.2",
        "openconfig-extensions revision: 2020-06-16", "openconfig-if-aggregate semver: 2.4.3",
        "openconfig-if-ethernet semver: 2.11.0", "openconfig-if-types semver: 0.2.1",
        "openconfig-inet-types semver: 0.4.0", "openconfig-interfaces semver: 2.5.0",
        "openconfig-platform semver: 0.13.0", "openconfig-platform-types semver: 1.1.0",
        "openconfig-vlan-types semver: 3.1.1", "openconfig-types semver: 0.6.0", "openconfig-vlan semver: 3.2.0",
        "openconfig-yang-types semver: 0.3.0", "gnmi-test-model semver: 1.0.0", "ietf-interfaces revision: 2018-02-20",
        "ietf-yang-types revision: 2013-07-15", "openconfig-aaa semver: 0.5.0", "openconfig-aaa-types semver: 0.4.1",
        "openconfig-license semver: 0.2.0", "openconfig-messages semver: 0.0.1", "openconfig-procmon semver: 0.4.0",
        "openconfig-system semver: 0.10.0","openconfig-system-logging semver: 0.3.1",
        "openconfig-system-terminal semver: 0.3.1", "openconfig-openflow semver: 0.1.2",
        "openconfig-openflow-types semver: 0.1.3", "gnmi-test-aug semver: 1.0.0");

    private static final String OC_INTERFACES_CONTAINER_EXPECTED_JSON_OBJ =
        "{\"openconfig-interfaces:interfaces\":"
            + "{\"interface\":[{\"name\":\"eth3\",\"config\":{\"enabled\":false,\"name\":\"admin\","
            + "\"type\":\"openconfig-if-types:IF_ETHERNET\",\"loopback-mode\":false,\"mtu\":1500}},"
            + "{\"name\":\"br0\",\"config\":{\"enabled\":false,\"name\":\"admin\",\"type\":"
            + "\"openconfig-if-types:IF_ETHERNET\",\"loopback-mode\":false,\"mtu\":100},"
            + "\"openconfig-if-ethernet:ethernet\":{\"config\":{\"enable-flow-control\":true,"
            + "\"openconfig-if-aggregate:aggregate-id\":\"admin\",\"auto-negotiate\":true,"
            + "\"port-speed\":\"openconfig-if-ethernet:SPEED_10MB\"},\"openconfig-vlan:switched-vlan"
            + "\":{\"config\":{\"native-vlan\":37,\"access-vlan\":45,\"interface-mode\":\"ACCESS\"},"
            + "\"state\":{\"native-vlan\":37,\"access-vlan\":45,\"interface-mode\":\"ACCESS\"}},"
            + "\"state\":{\"enable-flow-control\":true,\"port-speed\":"
            + "\"openconfig-if-ethernet:SPEED_10MB\",\"negotiated-duplex-mode\":\"FULL\","
            + "\"negotiated-port-speed\":\"openconfig-if-ethernet:SPEED_10MB\","
            + "\"openconfig-if-aggregate:aggregate-id\":\"admin\",\"auto-negotiate\":true,"
            + "\"hw-mac-address\":\"00:00:0A:BB:28:FC\"}},\"openconfig-if-aggregate:aggregation\":"
            + "{\"state\":{\"min-links\":5,\"lag-speed\":20,\"member\":[\"br0\"],\"lag-type\":"
            + "\"LACP\"},\"openconfig-vlan:switched-vlan\":{\"config\":{\"native-vlan\":34,"
            + "\"access-vlan\":54,\"interface-mode\":\"ACCESS\"},\"state\":{\"native-vlan\":34,"
            + "\"access-vlan\":54,\"interface-mode\":\"ACCESS\"}},\"config\":{\"lag-type\":\"LACP\","
            + "\"min-links\":5}},\"state\":{\"oper-status\":\"DOWN\",\"name\":\"br0\","
            + "\"loopback-mode\":false,\"mtu\":100,\"ifindex\":1,\"counters\":{\"in-octets\":\"100\","
            + "\"out-octets\":\"105\",\"in-fcs-errors\":\"104\",\"out-errors\":\"108\",\"out-pkts\":"
            + "\"106\",\"out-discards\":\"107\",\"in-pkts\":\"101\",\"in-discards\":\"102\","
            + "\"in-errors\":\"103\"},\"enabled\":false,\"logical\":true,\"type\":"
            + "\"openconfig-if-types:IF_ETHERNET\",\"admin-status\":\"UP\"}}]}}";
    private static final String OC_INTERFACE_ETH3_CONFIG_NAME_EXPECTED = "admin";
    private static final String OC_INTERFACE_ETH3_CONFIG_TYPE_EXPECTED = "openconfig-if-types:IF_ETHERNET";
    private static final String OC_INTERFACE_ETH3_EXPECTED = "{\"name\":\"eth3\",\"config\":{\"name\":\"admin\","
        + "\"type\":\"openconfig-if-types:IF_ETHERNET\",\"loopback-mode\":false,\"enabled\":false,\"mtu\":1500}}";
    private static SimulatedGnmiDevice device;

    @BeforeAll
    public static void setupDevice() {
        device = getUnsecureGnmiDevice(DEVICE_IP, DEVICE_PORT);
        try {
            device.start();
        } catch (IOException | EffectiveModelContextBuilderException e) {
            LOG.info("Exception during device startup: ", e);
        }
    }

    @AfterAll
    public static void teardownDevice() {
        device.stop();
    }

    @BeforeEach
    void performSpecificSetupBeforeEach() {
        try {
            if (!connectDevice(GNMI_NODE_ID, DEVICE_IP, DEVICE_PORT)) {
                LOG.info("Problem occurred while connecting device!");
            }
        } catch (InterruptedException | IOException e) {
            LOG.info("Exception occurred while connecting device: ", e);
        }
    }

    @Test
    public void getCapabilitiesTest() throws InterruptedException, IOException, JSONException {
        //assert all expected capabilities are contained in device response
        final HttpResponse<String> capabilitiesResponse = sendGetRequestJSON(GET_CAPABILITIES_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, capabilitiesResponse.statusCode());
        final JSONArray gnmiDeviceCapabilities = new JSONObject(capabilitiesResponse.body())
            .getJSONObject("gnmi-topology:available-capabilities").getJSONArray("available-capability");
        LOG.info("Response: {}", gnmiDeviceCapabilities);
        assertEquals(EXPECTED_CAPABILITIES.size(), gnmiDeviceCapabilities.length());
        final List<String> gnmiDeviceCapabilitiesList = convertCapabilitiesJSONArrayToList(gnmiDeviceCapabilities);
        assertTrue(gnmiDeviceCapabilitiesList.containsAll(EXPECTED_CAPABILITIES));
    }

    @Test
    public void getContainerTest() throws InterruptedException, IOException, JSONException {
        //assert openconfig-interfaces container returns expected value
        final HttpResponse<String> getOcInterfacesContainerResponse = sendGetRequestJSON(INTERFACES_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getOcInterfacesContainerResponse.statusCode());
        final JSONObject ocInterfacesContainer = new JSONObject(getOcInterfacesContainerResponse.body());
        LOG.info("Response: {}", ocInterfacesContainer);
        JSONAssert.assertEquals(OC_INTERFACES_CONTAINER_EXPECTED_JSON_OBJ, ocInterfacesContainer.toString(), false);
    }

    @Test
    public void getLeafTest() throws InterruptedException, IOException, JSONException {
        //assert name which is of leaf type in gnmi openconfig-interfaces - interface - eth3
        final HttpResponse<String> getOcInterfaceEth3ConfigNameResponse =
            sendGetRequestJSON(INTERFACES_PATH + "/interface=eth3/config/name");
        assertEquals(HttpURLConnection.HTTP_OK, getOcInterfaceEth3ConfigNameResponse.statusCode());
        final String ocInterfaceEth3ConfigName =
            new JSONObject(getOcInterfaceEth3ConfigNameResponse.body()).getString("openconfig-interfaces:name");
        LOG.info("Response: {}", ocInterfaceEth3ConfigName);
        assertEquals(OC_INTERFACE_ETH3_CONFIG_NAME_EXPECTED, ocInterfaceEth3ConfigName);
    }

    @Test
    public void getLeafIdentityRefTest() throws InterruptedException, IOException, JSONException {
        //assert type which is of leaf type(identityref) in gnmi openconfig-interfaces - interface - eth3
        final HttpResponse<String> getOcInterfaceEth3ConfigTypeResponse =
            sendGetRequestJSON(INTERFACES_PATH + "/interface=eth3/config/type");
        assertEquals(HttpURLConnection.HTTP_OK, getOcInterfaceEth3ConfigTypeResponse.statusCode());
        final String ocInterfaceEth3ConfigType =
            new JSONObject(getOcInterfaceEth3ConfigTypeResponse.body()).getString("openconfig-interfaces:type");
        LOG.info("Response: {}", ocInterfaceEth3ConfigType);
        assertEquals(OC_INTERFACE_ETH3_CONFIG_TYPE_EXPECTED, ocInterfaceEth3ConfigType);
    }

    @Test
    public void getListEntryTest() throws InterruptedException, IOException, JSONException {
        //assert list entry in openconfig-interfaces - interface - eth3, and also if it is only one with that key
        final HttpResponse<String> getOcInterfaceEth3Response = sendGetRequestJSON(INTERFACES_PATH + "/interface=eth3");
        assertEquals(HttpURLConnection.HTTP_OK, getOcInterfaceEth3Response.statusCode());
        final JSONArray ocInterfaces =
            new JSONObject(getOcInterfaceEth3Response.body()).getJSONArray("openconfig-interfaces:interface");
        assertEquals(1, ocInterfaces.length());
        final String ocInterfaceEth3 = ocInterfaces.getJSONObject(0).toString();
        LOG.info("Response: {}", ocInterfaceEth3);
        JSONAssert.assertEquals(OC_INTERFACE_ETH3_EXPECTED, ocInterfaceEth3, false);
    }

    @Test
    public void getIncorrectListEntryTest() throws InterruptedException, IOException, JSONException {
        //assert that request to list entry which does not exist - interface - ethNonExisting, will fail
        final HttpResponse<String> getOcInterfaceNonExistingResponse =
            sendGetRequestJSON(INTERFACES_PATH + "/interface=ethNonExisting");
        assertEquals(HttpURLConnection.HTTP_CONFLICT, getOcInterfaceNonExistingResponse.statusCode());
        JSONAssert.assertEquals(ERR_MSG_RELEVANT_MODEL_NOT_EXIST, getOcInterfaceNonExistingResponse.body(), false);
    }

    @Test
    public void getNonExistingDataTest() throws InterruptedException, IOException {
        //assert error for request to non existing interfacesincorrect container
        final HttpResponse<String> getOcInterfacesContainerWrongResponse =
            sendGetRequestJSON(INTERFACES_PATH + "incorrect");
        assertEquals(HttpURLConnection.HTTP_CONFLICT, getOcInterfacesContainerWrongResponse.statusCode());
    }

    @Test
    public void getLeafWithRegexPosix() throws IOException, InterruptedException, JSONException {
        final HttpResponse<String> openflowResponse = sendGetRequestJSON(OPENFLOW_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, openflowResponse.statusCode());
        JSONAssert.assertEquals(getExpectedOcOpenflowJsonResult(), openflowResponse.body(), false);
    }

    private List<String> convertCapabilitiesJSONArrayToList(final JSONArray capabilitiesArray) throws JSONException {
        final List<String> capabilitiesList = new ArrayList<>();
        for (int i = 0; i < capabilitiesArray.length(); i++) {
            capabilitiesList.add(capabilitiesArray.getJSONObject(i).getString("capability"));
        }
        return capabilitiesList;
    }

    private static String getExpectedOcOpenflowJsonResult() {
        return "{\"openconfig-openflow:openflow\":{"
                + "\"agent\":{"
                +   "\"config\":{"
                +       "\"backoff-interval\":5,"
                +       "\"datapath-id\":\"00:16:3e:00:00:00:00:00\","
                +       "\"inactivity-probe\":10,"
                +       "\"failure-mode\":\"SECURE\","
                +       "\"max-backoff\":10"
                + "}}}}";
    }
}
