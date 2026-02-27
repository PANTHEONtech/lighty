/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.test.gnmi.rcgnmi;

import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.ERR_MSG_RELEVANT_MODEL_NOT_EXIST;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_NODE_ID;
import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.RESTCONF_DATA_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.utils.EffectiveModelContextBuilder.EffectiveModelContextBuilderException;
import io.lighty.modules.gnmi.test.utils.TestUtils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GnmiUploadModelsITTest extends GnmiITBase {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiUploadModelsITTest.class);

    private static final String GNMI_UPLOAD_MODEL_RPC = "/gnmi-yang-storage:upload-yang-model";
    private static final String GNMI_YANG_STORAGE_MODELS = "/gnmi-yang-storage:gnmi-yang-models";
    private static final String TEST_YANG_NAME = "test-yang";
    private static final String TEST_YANG_SEMVER = "1.2.3";
    private static final String REUPLOAD_YANG_NAME = "gnmi-test-model";
    private static final String REUPLOAD_YANG_SEMVER = "1.0.0";
    private static final String RESOURCES_PATH = "src/test/resources/";
    private static final String RESTCONF_OPERATIONS_PATH = "http://localhost:8888/restconf/operations";
    private static final String YANG_MODEL_RPC = RESTCONF_DATA_PATH + GNMI_YANG_STORAGE_MODELS + "/gnmi-yang-model=";
    private static final String YANG_MODEL_PATH = YANG_MODEL_RPC + TEST_YANG_NAME + "," + TEST_YANG_SEMVER;
    private static final String REUPLOAD_YANG_MODEL_PATH
            = YANG_MODEL_RPC + REUPLOAD_YANG_NAME + "," + REUPLOAD_YANG_SEMVER;
    private static SimulatedGnmiDevice device;

    @BeforeAll
    public static void setupDevice() throws ConfigurationException {
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

    @AfterEach
    void performSpecificCleanupAfterEach() {
        try {
            final HttpResponse<String> getYangStorageTestModelResponse = sendGetRequestJSON(YANG_MODEL_PATH);
            if (getYangStorageTestModelResponse.statusCode() == HttpURLConnection.HTTP_OK) {
                sendDeleteRequestJSON(YANG_MODEL_PATH);
            }
        } catch (InterruptedException | IOException e) {
            LOG.info("Problem when cleaning up uploaded model {}:", TEST_YANG_NAME + "@" + TEST_YANG_SEMVER, e);
        }
    }

    @Test
    void uploadModelTest() throws InterruptedException, IOException, JSONException {
        //assert that uploaded yang currently does not exist in gnmi-yang-storage
        final HttpResponse<String> getYangStorageTestModelNonUploadedResponse = sendGetRequestJSON(YANG_MODEL_PATH);
        assertEquals(HttpURLConnection.HTTP_CONFLICT, getYangStorageTestModelNonUploadedResponse.statusCode());
        JSONAssert.assertEquals(ERR_MSG_RELEVANT_MODEL_NOT_EXIST,
                getYangStorageTestModelNonUploadedResponse.body(), false);

        //read and upload model and assert successful RPC response
        final String testModel = TEST_YANG_NAME + ".yang";
        final String modelBody = TestUtils.readFile(RESOURCES_PATH + "models/" + testModel);
        LOG.info("Content of YANG {} file: {}", testModel, modelBody);
        final String modelsToUploadRpcInput =
            createUploadRpcInput(TEST_YANG_NAME, TEST_YANG_SEMVER, modelBody.replace("\"", "\\\""));
        final HttpResponse<String> postRpcUploadModelResponse =
            sendPostRequestJSON(RESTCONF_OPERATIONS_PATH + GNMI_UPLOAD_MODEL_RPC, modelsToUploadRpcInput);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, postRpcUploadModelResponse.statusCode());

        //assert model is uploaded in gnmi-yang-storage
        final HttpResponse<String> getYangStorageTestModelUploadedResponse = sendGetRequestJSON(YANG_MODEL_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getYangStorageTestModelUploadedResponse.statusCode());
        final JSONArray loadedModels = new JSONObject(getYangStorageTestModelUploadedResponse.body())
            .getJSONArray("gnmi-yang-storage:gnmi-yang-model");
        assertEquals(1, loadedModels.length());
        final JSONObject loadedModel = loadedModels.getJSONObject(0);
        assertEquals(TEST_YANG_NAME, loadedModel.get("name"));
        assertEquals(TEST_YANG_SEMVER, loadedModel.get("version"));
        assertEquals(modelBody, loadedModel.get("body"));
    }

    @Test
    void reuploadModelTest() throws InterruptedException, IOException, JSONException {
        final String testModel = REUPLOAD_YANG_NAME + ".yang";
        final String modelBody = TestUtils.readFile(RESOURCES_PATH + "additional/models/" + testModel);
        LOG.info("Content of YANG {} file to re-upload: {}", testModel, modelBody);

        //assert model is already contained in gnmi-yang-storage and assert model name, semver and content
        final HttpResponse<String> getYangStorageTestModelPresentResponse =
                sendGetRequestJSON(REUPLOAD_YANG_MODEL_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getYangStorageTestModelPresentResponse.statusCode());
        final JSONArray testYangJSONArrayPresent = new JSONObject(getYangStorageTestModelPresentResponse.body())
            .getJSONArray("gnmi-yang-storage:gnmi-yang-model");
        assertEquals(1, testYangJSONArrayPresent.length());
        final JSONObject loadedYangJSONObject = testYangJSONArrayPresent.getJSONObject(0);
        assertEquals(REUPLOAD_YANG_NAME, loadedYangJSONObject.get("name"));
        assertEquals(REUPLOAD_YANG_SEMVER, loadedYangJSONObject.get("version"));
        assertEquals(modelBody, loadedYangJSONObject.get("body"));

        //reupload model with upload RPC
        final String rpcInput =
            createUploadRpcInput(REUPLOAD_YANG_NAME, REUPLOAD_YANG_SEMVER, modelBody.replace("\"", "\\\""));
        final HttpResponse<String> postRpcUploadModelResponse =
            sendPostRequestJSON(RESTCONF_OPERATIONS_PATH + GNMI_UPLOAD_MODEL_RPC, rpcInput);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, postRpcUploadModelResponse.statusCode());

        //assert that model is not contained twice and name, semver and content is matching
        final HttpResponse<String> getYangStorageTestModelReuploadedResponse =
            sendGetRequestJSON(REUPLOAD_YANG_MODEL_PATH);
        assertEquals(HttpURLConnection.HTTP_OK, getYangStorageTestModelReuploadedResponse.statusCode());
        final JSONArray reuploadedYangJSONArrayReuploaded =
            new JSONObject(getYangStorageTestModelReuploadedResponse.body())
                .getJSONArray("gnmi-yang-storage:gnmi-yang-model");
        assertEquals(1, reuploadedYangJSONArrayReuploaded.length());
        final JSONObject reuploadedYangJSONObject = reuploadedYangJSONArrayReuploaded.getJSONObject(0);
        assertEquals(REUPLOAD_YANG_NAME, reuploadedYangJSONObject.get("name"));
        assertEquals(REUPLOAD_YANG_SEMVER, reuploadedYangJSONObject.get("version"));
        assertEquals(modelBody, reuploadedYangJSONObject.get("body"));
    }

    private String createUploadRpcInput(final String name, final String semver, final String body) {
        return "{\n"
            + "\t\"input\": {\n"
            + "\t\t\"name\": \"" + name + "\",\n"
            + "\t\t\"version\": \"" + semver + "\",\n"
            + "\t\t\"body\": \"" + body + "\"\n"
            + "\t}\n"
            + "}";
    }

}
