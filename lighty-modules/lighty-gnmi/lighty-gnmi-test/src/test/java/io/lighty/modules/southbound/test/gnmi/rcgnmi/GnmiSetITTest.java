/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.southbound.test.gnmi.rcgnmi;

import static io.lighty.modules.southbound.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.ERR_MSG_RELEVANT_MODEL_NOT_EXIST;
import static io.lighty.modules.southbound.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_DEVICE_MOUNTPOINT;
import static io.lighty.modules.southbound.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_NODE_ID;
import static io.lighty.modules.southbound.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.INTERFACE_ETH3_CONFIG_NAME_PAYLOAD;
import static io.lighty.modules.southbound.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.OPENCONFIG_INTERFACES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.modules.southbound.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.southbound.test.utils.TestUtils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnmiSetITTest extends GnmiITBase {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiSetITTest.class);
    private static final String CONFIG_INTERFACE_INIT_DATA = getResource("/json/initData/config.json");
    private static final String GNMI_TEST_DATA = "gnmi-test-model:test-data";
    private static final String CONFIG_DATASTORE = "?content=config";
    private static final String GNMI_TEST_CONTAINER_PATH = GNMI_DEVICE_MOUNTPOINT + "/" + GNMI_TEST_DATA;
    private static final String GNMI_TEST_BASE_LIST_PATH = GNMI_DEVICE_MOUNTPOINT + "/gnmi-test-model:base-list=datO";
    private static final String WRONG_BASE_LIST_PATH = GNMI_DEVICE_MOUNTPOINT + "/gnmi-test-model:base-list=WRONG";
    private static final String NESTED_LIST_PATH = GNMI_TEST_BASE_LIST_PATH + "/nested-list=dat21";
    private static final String WRONG_NESTED_LIST_PATH = WRONG_BASE_LIST_PATH + "/nested-list=dat21";
    private static final String TEST_LEAF_LIST = "test-leaf-list";
    private static final String TEST_LIST = "test-list";
    private static final String LIST_ID_10 = "INTERFACE_10";
    private static final String LIST_ID_20 = "INTERFACE_20";
    private static final String BASE_LIST = "gnmi-test-model:base-list";
    private static final String LIST_KEY = "list-key";
    private static final String NESTED_LIST = "nested-list";
    private static final String INTERFACES_CONTAINER = "openconfig-interfaces:interfaces";
    private static final String INTERFACE = "interface";
    private static final String LEAFLIST_REPLACE_VAL
            = "{\"gnmi-test-model:test-data\":{\"test-leaf-list\":[\"data1\",\"data2\"]}}";
    private static final String LEAFLIST_UPDATE_VAL
            = "{\"gnmi-test-model:test-data\":{\"test-leaf-list\":[\"data2\",\"data3\",\"data4\"]}}";
    private static final String LEAFLIST_UPDATE_RESPONSE_VAL
            = "{\"gnmi-test-model:test-data\":{\"test-leaf-list\":[\"data1\",\"data2\",\"data3\",\"data4\"]}}";

    private static final String INTERFACES_PATH = GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES;
    private static final String CONTAINER_PATH = INTERFACES_PATH + "/interface=eth3/config";
    private static final String LEAF_PATH = INTERFACES_PATH + "/interface=eth3/config/name";
    private static final String IDENTITY_REF_PATH = INTERFACES_PATH + "/interface=eth3/config/type";
    private static final String INTERFACE_ETH3_CONFIG_PAYLOAD = "{\n"
        + "  \"config\": {\n"
        + "    \"enabled\": false,\n"
        + "    \"name\": \"updated-config\",\n"
        + "    \"type\": \"openconfig-if-types:IF_LOOPBACK\",\n"
        + "    \"loopback-mode\": true,\n"
        + "    \"mtu\": 1400\n"
        + "  }\n"
        + "}";

    private static final String IDENTITY_PAYLOAD
            = "{\"openconfig-interfaces:type\":\"openconfig-if-types:IF_LOOPBACK\"}";
    private static final String INTERFACE_BR0_STATE_AGGREGATE_MIN_LINKS_TYPE_PAYLOAD = "{\n"
        + "    \"openconfig-if-aggregate:min-links\": 10\n"
        + "}";
    private static final JSONObject CONFIG_FALSE_ERROR = new JSONObject("{\n"
        + "    \"errors\": {\n"
        + "        \"error\": [\n"
        + "            {\n"
        + "                \"error-type\": \"application\",\n"
        + "                \"error-tag\": \"operation-failed\",\n"
        + "                \"error-info\": \"io.grpc.StatusRuntimeException: NOT_FOUND: "
        + "Update for non existing simple value is not permitted\",\n"
        + "                \"error-message\": \"Transaction failed\"\n"
        + "            }\n"
        + "        ]\n"
        + "    }\n"
        + "}");
    private static SimulatedGnmiDevice device;

    @BeforeAll
    public static void setupDevice() {
        device = getUnsecureGnmiDevice(DEVICE_IP, DEVICE_PORT);
        try {
            device.start();
        } catch (IOException e) {
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
    public void setContainerTest() throws InterruptedException, IOException {
        //assert value before set, does not equals value that should be set
        final JSONObject configContainerPayloadJSONObject = new JSONObject(INTERFACE_ETH3_CONFIG_PAYLOAD);
        final HttpResponse<String> getOcInterfaceEth3ConfigContainerBeforeResponse = sendGetRequestJSON(CONTAINER_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getOcInterfaceEth3ConfigContainerBeforeResponse.statusCode());
        final JSONObject eth3ConfigContainerBeforeJSONObject =
            new JSONObject(getOcInterfaceEth3ConfigContainerBeforeResponse.body());
        assertNotEquals(configContainerPayloadJSONObject.getJSONObject("config").toString(),
            eth3ConfigContainerBeforeJSONObject.getJSONObject("openconfig-interfaces:config").toString());

        //assert that response for set value is 204
        final HttpResponse<String> setOcInterfaceEth3ConfigContainerResponse = sendPutRequestJSON(CONTAINER_PATH,
                INTERFACE_ETH3_CONFIG_PAYLOAD);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, setOcInterfaceEth3ConfigContainerResponse.statusCode());

        //assert that value has been set and equals to expected
        final HttpResponse<String> getOcInterfaceEth3ConfigContainerResponse = sendGetRequestJSON(CONTAINER_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getOcInterfaceEth3ConfigContainerResponse.statusCode());
        final JSONObject eth3ConfigContainerJSONObject =
            new JSONObject(getOcInterfaceEth3ConfigContainerResponse.body());
        assertEquals(configContainerPayloadJSONObject.getJSONObject("config").toString(),
                     eth3ConfigContainerJSONObject.getJSONObject("openconfig-interfaces:config").toString());

        restoreDeviceToOriginalState();
    }

    @Test
    public void setLeafTest() throws InterruptedException, IOException {
        final JSONObject configNameLeafPayloadJSONObject = new JSONObject(INTERFACE_ETH3_CONFIG_NAME_PAYLOAD);
        final HttpResponse<String> getOcInterfaceEth3ConfigNameLeafBeforeResponse = sendGetRequestJSON(LEAF_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getOcInterfaceEth3ConfigNameLeafBeforeResponse.statusCode());
        final JSONObject eth3ConfigNameLeafBeforeJSONObject =
            new JSONObject(getOcInterfaceEth3ConfigNameLeafBeforeResponse.body());
        assertNotEquals(configNameLeafPayloadJSONObject.toString(), eth3ConfigNameLeafBeforeJSONObject.toString());

        final HttpResponse<String> setOcInterfaceEth3ConfigNameLeafResponse = sendPutRequestJSON(LEAF_PATH,
                INTERFACE_ETH3_CONFIG_NAME_PAYLOAD);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, setOcInterfaceEth3ConfigNameLeafResponse.statusCode());

        final HttpResponse<String> getOcInterfaceEth3ConfigNameLeafResponse = sendGetRequestJSON(LEAF_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getOcInterfaceEth3ConfigNameLeafResponse.statusCode());
        final JSONObject eth3ConfigNameLeafJSONObject = new JSONObject(getOcInterfaceEth3ConfigNameLeafResponse.body());
        assertEquals(configNameLeafPayloadJSONObject.toString(), eth3ConfigNameLeafJSONObject.toString());

        restoreDeviceToOriginalState();
    }

    @Test
    public void setLeafIdentityRefTest() throws InterruptedException, IOException {
        //Check if current data is not same as updating data
        final HttpResponse<String> getIdentityData = sendGetRequestJSON(IDENTITY_REF_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getIdentityData.statusCode());
        JSONObject actualIdentityData = new JSONObject(getIdentityData.body());
        assertNotEquals(IDENTITY_PAYLOAD, actualIdentityData.toString());

        //Update current data with put request
        final HttpResponse<String> putIdentityReq = sendPutRequestJSON(IDENTITY_REF_PATH, IDENTITY_PAYLOAD);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, putIdentityReq.statusCode());

        //Verify updated data
        final HttpResponse<String> getUpdatedIdentity = sendGetRequestJSON(IDENTITY_REF_PATH + CONFIG_DATASTORE);
        assertEquals(HttpURLConnection.HTTP_OK, getUpdatedIdentity.statusCode());
        final JSONObject identityJsonResult = new JSONObject(getUpdatedIdentity.body());
        assertEquals(IDENTITY_PAYLOAD, identityJsonResult.toString());

        restoreDeviceToOriginalState();
    }

    @Test
    public void setConfigFalseDataTest() throws InterruptedException, IOException {
        //check if min-links on the given path is default(not updated)
        final JSONObject minLinksPayloadJSONObject =
            new JSONObject(INTERFACE_BR0_STATE_AGGREGATE_MIN_LINKS_TYPE_PAYLOAD);
        final HttpResponse<String> getMinLinksBeforeResponse =
            sendGetRequestJSON(GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES
                + "/interface=br0/openconfig-if-aggregate:aggregation/state/min-links");
        assertEquals(HttpURLConnection.HTTP_OK, getMinLinksBeforeResponse.statusCode());
        final JSONObject minLinksBeforeJSONObject = new JSONObject(getMinLinksBeforeResponse.body());
        assertNotEquals(minLinksPayloadJSONObject.toString(), minLinksBeforeJSONObject.toString());

        //set min-links on tha path and assert expected error (min-links is in config false container)
        final HttpResponse<String> setMinLinksResponse =
            sendPutRequestJSON(GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES
                    + "/interface=br0/openconfig-if-aggregate:aggregation/state/min-links",
                INTERFACE_BR0_STATE_AGGREGATE_MIN_LINKS_TYPE_PAYLOAD);
        assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, setMinLinksResponse.statusCode());
        final JSONArray errorsResponse =
            new JSONObject(setMinLinksResponse.body()).getJSONObject("errors").getJSONArray("error");
        assertEquals(1, errorsResponse.length());
        final JSONArray errorsExpected = CONFIG_FALSE_ERROR.getJSONObject("errors").getJSONArray("error");
        assertEquals(errorsExpected.toString(), errorsResponse.toString());

        //assert that it was not updated
        final HttpResponse<String> getMinLinksResponse =
            sendGetRequestJSON(GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES
                + "/interface=br0/openconfig-if-aggregate:aggregation/state/min-links");
        assertEquals(HttpURLConnection.HTTP_OK, getMinLinksResponse.statusCode());
        final JSONObject minLinksJSONObject = new JSONObject(getMinLinksResponse.body());
        assertEquals(minLinksBeforeJSONObject.toString(), minLinksJSONObject.toString());

        restoreDeviceToOriginalState();
    }

    @Test
    public void deleteLeafTest() throws InterruptedException, IOException {
        //assert default initialized state of name of interface eth3
        final HttpResponse<String> getOcInterfaceEth3ConfigNameLeafBeforeResponse =
            sendGetRequestJSON(GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES + "/interface=eth3/config/name");
        assertEquals(HttpURLConnection.HTTP_OK, getOcInterfaceEth3ConfigNameLeafBeforeResponse.statusCode());
        final JSONObject eth3ConfigNameLeafBeforeJSONObject =
            new JSONObject(getOcInterfaceEth3ConfigNameLeafBeforeResponse.body());
        final JSONObject configNameLeafPayloadJSONObject = new JSONObject(INTERFACE_ETH3_CONFIG_NAME_PAYLOAD);
        assertNotEquals(configNameLeafPayloadJSONObject.toString(), eth3ConfigNameLeafBeforeJSONObject.toString());

        //delete leaf name from interface eth3
        final HttpResponse<String> deleteOcInterfaceEth3ConfigNameLeafResponse =
            sendDeleteRequestJSON(GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES + "/interface=eth3/config/name");
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, deleteOcInterfaceEth3ConfigNameLeafResponse.statusCode());

        //assert errors getting deleted leaf name from eth3 interface
        final HttpResponse<String> getOcInterfaceEth3ConfigNameLeafResponse =
            sendGetRequestJSON(GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES + "/interface=eth3/config/name");
        assertEquals(HttpURLConnection.HTTP_CONFLICT, getOcInterfaceEth3ConfigNameLeafResponse.statusCode());
        final JSONArray responseErrors = new JSONObject(getOcInterfaceEth3ConfigNameLeafResponse.body())
            .getJSONObject("errors").getJSONArray("error");
        assertEquals(1, responseErrors.length());
        final String ocDeletedLeafError = responseErrors.getJSONObject(0).toString();
        assertEquals(ERR_MSG_RELEVANT_MODEL_NOT_EXIST, ocDeletedLeafError);

        restoreDeviceToOriginalState();
    }

    @Test
    public void setSimpleListEntryTest() throws IOException, InterruptedException {
        // Set simple list with
        final String listPath = GNMI_TEST_CONTAINER_PATH + "/test-list=" + LIST_ID_10;
        final String listBody = getSimpleListData(LIST_ID_10);
        final HttpResponse<String> setListResponse = sendPutRequestJSON(listPath, listBody);
        assertEquals(HttpURLConnection.HTTP_CREATED, setListResponse.statusCode());

        //Verify that simple list is setup correctly
        final HttpResponse<String> getListdata = sendGetRequestJSON(listPath);
        assertEquals(HttpURLConnection.HTTP_OK, getListdata.statusCode());
        assertTrue(TestUtils.jsonMatch(getListdata.body(), listBody));

        //Update simple list
        final String updateListPath = GNMI_TEST_CONTAINER_PATH + "/test-list=" + LIST_ID_20;
        final String updateListBody = getSimpleListData(LIST_ID_20);
        final HttpResponse<String> updateListResponse = sendPatchRequestJSON(updateListPath, updateListBody);
        assertEquals(HttpURLConnection.HTTP_OK, updateListResponse.statusCode());

        //Verify that simple list is updated
        final HttpResponse<String> getAllListData = sendGetRequestJSON(GNMI_TEST_CONTAINER_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getAllListData.statusCode());
        final JSONArray updatedJsonArray = new JSONObject(getAllListData.body())
                .getJSONObject(GNMI_TEST_DATA).getJSONArray(TEST_LIST);
        final List<String> sortedJsonArray = getSortedJsonArray(updatedJsonArray);
        assertEquals(sortedJsonArray.size(), 2);
        sortedJsonArray.remove(String.format("{\"key\":\"%s\"}", LIST_ID_10));
        sortedJsonArray.remove(String.format("{\"key\":\"%s\"}", LIST_ID_20));
        assertEquals(sortedJsonArray.size(), 0);

        removeGnmiTestDataContainer();
    }

    @Test
    public void setSimpleListEntryInsideContainerTest() throws IOException, InterruptedException {
        //Set list from container path
        final String setListBody = getNewListInContainerData();
        final HttpResponse<String> setListResponse = sendPutRequestJSON(GNMI_TEST_CONTAINER_PATH, setListBody);
        assertEquals(HttpURLConnection.HTTP_CREATED, setListResponse.statusCode());

        //Verify that list is created
        final HttpResponse<String> getAllListData = sendGetRequestJSON(GNMI_TEST_CONTAINER_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getAllListData.statusCode());

        final JSONArray expected = new JSONObject(getAllListData.body())
                .getJSONObject(GNMI_TEST_DATA).getJSONArray(TEST_LIST);
        final JSONArray actual = new JSONObject(setListBody)
                .getJSONObject(GNMI_TEST_DATA).getJSONArray(TEST_LIST);
        final List<String> expectedArray = getSortedJsonArray(expected);
        final List<String> actualArray = getSortedJsonArray(actual);
        assertEquals(expectedArray, actualArray);

        // Update list data
        final HttpResponse<String> updateListResponse
                = sendPatchRequestJSON(GNMI_TEST_CONTAINER_PATH, getUpdateListInContainerData());
        assertEquals(HttpURLConnection.HTTP_OK, updateListResponse.statusCode());

        // Verify that list is updated
        final HttpResponse<String> getUpdatedData = sendGetRequestJSON(GNMI_TEST_CONTAINER_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getAllListData.statusCode());
        final JSONArray expectedUpdate = new JSONObject(getUpdatedData.body())
                .getJSONObject(GNMI_TEST_DATA).getJSONArray(TEST_LIST);
        final JSONArray actualUpdate  = new JSONObject(getUpdatedListInContainerResultData())
                .getJSONObject(GNMI_TEST_DATA).getJSONArray(TEST_LIST);
        List<String> expectedUpdateArray = getSortedJsonArray(expectedUpdate);
        List<String> actualUpdateArray = getSortedJsonArray(actualUpdate);
        assertEquals(expectedUpdateArray, actualUpdateArray);

        removeGnmiTestDataContainer();
    }

    @Test
    public void setInterfaceListEntryTest() throws IOException, InterruptedException {
        // Replace data in interface config
        String setListPath = GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES;
        String setListBody = getInterfaceContainerListBody(LIST_ID_10);
        final HttpResponse<String> setListResponse = sendPutRequestJSON(setListPath, setListBody);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, setListResponse.statusCode());

        // Verify that data was replaced in configuration data-store
        final HttpResponse<String> getListResponse = sendGetRequestJSON(setListPath + CONFIG_DATASTORE);
        assertEquals(HttpURLConnection.HTTP_OK, getListResponse.statusCode());
        assertTrue(TestUtils.jsonMatch(setListBody, getListResponse.body()));

        // Update interface data
        String updateListPath = GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES;
        String updateListBody = getInterfaceContainerListBody(LIST_ID_20);
        final HttpResponse<String> updateListResponse = sendPatchRequestJSON(updateListPath, updateListBody);
        assertEquals(HttpURLConnection.HTTP_OK, updateListResponse.statusCode());

        //Verify that interface data was updated
        final HttpResponse<String> getAllData = sendGetRequestJSON(setListPath + CONFIG_DATASTORE);
        assertEquals(HttpURLConnection.HTTP_OK, getAllData.statusCode());

        JSONArray actualUpdatedData = new JSONObject(getAllData.body())
                .getJSONObject(INTERFACES_CONTAINER).getJSONArray(INTERFACE);
        JSONArray expectedUpdatedData = new JSONObject(getInterfaceListUpdateResponse(LIST_ID_10, LIST_ID_20))
                .getJSONObject(INTERFACES_CONTAINER).getJSONArray(INTERFACE);
        List<String> actualUpdateArray = getSortedJsonArray(actualUpdatedData);
        List<String> expectedUpdateArray = getSortedJsonArray(expectedUpdatedData);
        assertEquals(expectedUpdateArray, actualUpdateArray);

        restoreDeviceToOriginalState();
    }

    @Test
    public void setNestedListInWrongWayTest() throws IOException, InterruptedException {
        String nestedListBody = getNestedListBody();
        final HttpResponse<String> nestedListResponse = sendPutRequestJSON(WRONG_NESTED_LIST_PATH, nestedListBody);
        assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, nestedListResponse.statusCode());
    }

    @Test
    public void setNestedListTest() throws IOException, InterruptedException {
        // Create data in base list
        String baseListBody = getBaseListBody();
        final HttpResponse<String> nestedResponse = sendPutRequestJSON(GNMI_TEST_BASE_LIST_PATH, baseListBody);
        assertEquals(HttpURLConnection.HTTP_CREATED, nestedResponse.statusCode());

        // Verify created base list data
        final HttpResponse<String> getBaseList = sendGetRequestJSON(GNMI_TEST_BASE_LIST_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getBaseList.statusCode());
        assertTrue(TestUtils.jsonMatch(getBaseList.body(), getBaseListBody()));

        // Replace data in nested list
        String nestedListBody = getNestedListBody();
        final HttpResponse<String> nestedListResponse = sendPutRequestJSON(NESTED_LIST_PATH, nestedListBody);
        assertEquals(HttpURLConnection.HTTP_CREATED, nestedListResponse.statusCode());

        // Verify replaced data in nested list
        final HttpResponse<String> getRequestJSON = sendGetRequestJSON(GNMI_TEST_BASE_LIST_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getRequestJSON.statusCode());
        assertTrue(TestUtils.jsonMatch(getRequestJSON.body(), getNestedListBodyResponse()));

        // Update nested list
        final HttpResponse<String> nestedListUpdateResponse
                = sendPatchRequestJSON(GNMI_TEST_BASE_LIST_PATH, getBaseListBody());
        assertEquals(HttpURLConnection.HTTP_OK, nestedListUpdateResponse.statusCode());

        // Verify that nested list was updated
        final HttpResponse<String> getUpdatedNestedList = sendGetRequestJSON(GNMI_TEST_BASE_LIST_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getUpdatedNestedList.statusCode());

        JSONObject actualBaseList = new JSONObject(getUpdatedNestedList.body())
                .getJSONArray(BASE_LIST).getJSONObject(0);
        JSONObject expectedBaseList = new JSONObject(getNestedListUpdateBodyResponse())
                .getJSONArray(BASE_LIST).getJSONObject(0);
        assertEquals(expectedBaseList.get(LIST_KEY), actualBaseList.get(LIST_KEY));
        List<String> actualNestedList = getSortedJsonArray(actualBaseList.getJSONArray(NESTED_LIST));
        List<String> expectedNestedList = getSortedJsonArray(expectedBaseList.getJSONArray(NESTED_LIST));
        assertEquals(expectedNestedList, actualNestedList);

        removeGnmiTestBaseList();
    }

    @Test
    public void setLeafListEntryTest() throws InterruptedException, IOException {
        //Verify that leaf-list data are empty
        final HttpResponse<String> getInvalidResponse = sendGetRequestJSON(GNMI_TEST_CONTAINER_PATH);
        assertEquals(HttpURLConnection.HTTP_CONFLICT, getInvalidResponse.statusCode());

        //Put data to leaf-list
        final HttpResponse<String> setLeafListResponse
                = sendPutRequestJSON(GNMI_TEST_CONTAINER_PATH, LEAFLIST_REPLACE_VAL);
        assertEquals(HttpURLConnection.HTTP_CREATED, setLeafListResponse.statusCode());

        //Verify that leaf-list data are created
        final HttpResponse<String> getLeafListData = sendGetRequestJSON(GNMI_TEST_CONTAINER_PATH);
        final String successResponse = getLeafListData.body();
        assertEquals(HttpURLConnection.HTTP_OK, getLeafListData.statusCode());
        compareLeafListResponse(LEAFLIST_REPLACE_VAL, successResponse);

        //Update data in leaf-list
        final HttpResponse<String> updateLeafListResponse
                = sendPatchRequestJSON(GNMI_TEST_CONTAINER_PATH, LEAFLIST_UPDATE_VAL);
        assertEquals(HttpURLConnection.HTTP_OK, updateLeafListResponse.statusCode());

        //Verify that leaf-list data are updated
        final HttpResponse<String> getLeafListUpdateData = sendGetRequestJSON(GNMI_TEST_CONTAINER_PATH);
        final String updateResponse = getLeafListUpdateData.body();
        assertEquals(HttpURLConnection.HTTP_OK, getLeafListUpdateData.statusCode());
        compareLeafListResponse(LEAFLIST_UPDATE_RESPONSE_VAL, updateResponse);

        removeGnmiTestDataContainer();
    }

    private static void compareLeafListResponse(final String expected, final String actual) {
        final JSONArray expectedArray = new JSONObject(expected)
                .getJSONObject(GNMI_TEST_DATA).getJSONArray(TEST_LEAF_LIST);
        final JSONArray actualArray = new JSONObject(actual)
                .getJSONObject(GNMI_TEST_DATA).getJSONArray(TEST_LEAF_LIST);
        final List<String> expectedList = getSortedJsonArray(expectedArray);
        final List<String> actualList = getSortedJsonArray(actualArray);
        assertEquals(expectedList, actualList);
    }

    private static List<String> getSortedJsonArray(final JSONArray jsonArray) {
        final List<String> list = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.get(i).toString());
        }
        Collections.sort(list);
        return list;
    }

    private void restoreDeviceToOriginalState() throws IOException, InterruptedException {
        String path = GNMI_DEVICE_MOUNTPOINT + OPENCONFIG_INTERFACES;
        HttpResponse<String> stringHttpResponse = sendPutRequestJSON(path, CONFIG_INTERFACE_INIT_DATA);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, stringHttpResponse.statusCode());
    }

    private void removeGnmiTestDataContainer() throws IOException, InterruptedException {
        HttpResponse<String> deleteResponse = sendDeleteRequestJSON(GNMI_TEST_CONTAINER_PATH);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, deleteResponse.statusCode());
    }

    private void removeGnmiTestBaseList() throws IOException, InterruptedException {
        HttpResponse<String> deleteResponse = sendDeleteRequestJSON(GNMI_TEST_BASE_LIST_PATH);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, deleteResponse.statusCode());
    }

    private static String getResource(final String path) {
        try {
            final byte[] bytes = Files.readAllBytes(Paths.get(GnmiSetITTest.class.getResource(path).toURI()));
            return new String(bytes);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(String.format("Failed to read resources at path [%s]", path), e);
        }
    }

    private String getBaseListBody() {
        return "{\n"
              + "    \"gnmi-test-model:base-list\": [\n"
              + "        {\n"
              + "            \"list-key\": \"datO\",\n"
              + "            \"nested-list\": [\n"
              + "                {\n"
              + "                    \"nested-list-key\": \"dat1\"\n"
              + "                },\n"
              + "                {\n"
              + "                    \"nested-list-key\": \"dat2\"\n"
              + "                }\n"
              + "            ]\n"
              + "        }\n"
              + "    ]\n"
              + "}";
    }

    private String getNestedListBody() {
        return "{\n"
              + "      \"gnmi-test-model:nested-list\": [\n"
              + "          {\n"
              + "              \"nested-list-key\": \"dat21\"\n"
              + "          }\n"
              + "      ]\n"
              + "}";
    }

    private String getNestedListBodyResponse() {
        return "{\n"
              + "    \"gnmi-test-model:base-list\": [\n"
              + "        {\n"
              + "            \"list-key\": \"datO\",\n"
              + "            \"nested-list\": [\n"
              + "                {\n"
              + "                     \"nested-list-key\": \"dat21\"\n"
              + "                }\n"
              + "            ]\n"
              + "        }\n"
              + "    ]\n"
              + "}";
    }

    private String getNestedListUpdateBodyResponse() {
        return "{\n"
              + "    \"gnmi-test-model:base-list\": [\n"
              + "        {\n"
              + "            \"list-key\": \"datO\",\n"
              + "            \"nested-list\": [\n"
              + "                {\n"
              + "                    \"nested-list-key\": \"dat21\"\n"
              + "                },\n"
              + "                {\n"
              + "                    \"nested-list-key\": \"dat1\"\n"
              + "                },\n"
              + "                {\n"
              + "                    \"nested-list-key\": \"dat2\"\n"
              + "                }\n"
              + "            ]\n"
              + "        }\n"
              + "    ]\n"
              + "}";
    }

    private String getInterfaceContainerListBody(final String name) {
        return String.format("{\n"
              + "  \"openconfig-interfaces:interfaces\": {\n"
              + "    \"interface\": [\n"
              + "      {\n"
              + "         \"name\": \"%s\",\n"
              + "        \"config\": {\n"
              + "          \"enabled\": false,\n"
              + "          \"name\": \"admin\",\n"
              + "          \"type\": \"openconfig-if-types:IF_ETHERNET\",\n"
              + "          \"loopback-mode\": false,\n"
              + "          \"mtu\": 1500\n"
              + "        }\n"
              + "      }\n"
              + "    ]\n"
              + "  }\n"
              + "}", name);
    }

    private String getSimpleListData(final String keyId) {
        return String.format("{\n"
              + "  \"gnmi-test-model:test-list\": [\n"
              + "    {\n"
              + "     \"key\":\"%s\""
              + "    }\n"
              + "  ]\n"
              + "}", keyId);
    }

    private String getNewListInContainerData() {
        return "{\n"
              + "    \"gnmi-test-model:test-data\" : {\n"
              + "        \"test-list\" : [\n"
              + "            {\n"
              + "                    \"key\":\"dat1\"\n"
              + "            },\n"
              + "            {\n"
              + "                    \"key\":\"dat2\"\n"
              + "            }\n"
              + "        ]\n"
              + "    }\n"
              + "}";
    }

    private String getUpdateListInContainerData() {
        return "{\n"
              + "    \"gnmi-test-model:test-data\" : {\n"
              + "        \"test-list\" : [\n"
              + "            {\n"
              + "                    \"key\":\"dat2\"\n"
              + "            },\n"
              + "            {\n"
              + "                    \"key\":\"dat3\"\n"
              + "            }\n"
              + "        ]\n"
              + "    }\n"
              + "}";
    }

    private String getUpdatedListInContainerResultData() {
        return "{\n"
              + "    \"gnmi-test-model:test-data\" : {\n"
              + "        \"test-list\" : [\n"
              + "            {\n"
              + "                    \"key\":\"dat1\"\n"
              + "            },\n"
              + "            {\n"
              + "                    \"key\":\"dat2\"\n"
              + "            },\n"
              + "            {\n"
              + "                    \"key\":\"dat3\"\n"
              + "            }\n"
              + "        ]\n"
              + "    }\n"
              + "}";
    }

    private String getInterfaceListUpdateResponse(final String firstNameId, final String secondNameId) {
        return String.format("{\n"
              + "    \"openconfig-interfaces:interfaces\": {\n"
              + "        \"interface\": [\n"
              + "            {\n"
              + "                \"name\": \"%s\",\n"
              + "                \"config\": {\n"
              + "                    \"enabled\": false,\n"
              + "                    \"name\": \"admin\",\n"
              + "                    \"type\": \"openconfig-if-types:IF_ETHERNET\",\n"
              + "                    \"loopback-mode\": false,\n"
              + "                    \"mtu\": 1500\n"
              + "                }\n"
              + "            },\n"
              + "            {\n"
              + "                \"name\": \"%s\",\n"
              + "                \"config\": {\n"
              + "                    \"enabled\": false,\n"
              + "                    \"name\": \"admin\",\n"
              + "                    \"type\": \"openconfig-if-types:IF_ETHERNET\",\n"
              + "                    \"loopback-mode\": false,\n"
              + "                    \"mtu\": 1500\n"
              + "                }\n"
              + "            }\n"
              + "        ]\n"
              + "    }\n"
              + "}", firstNameId, secondNameId);
    }
}
