package io.lighty.examples.controllers.gnmi;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.IOUtils;
import org.awaitility.Awaitility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GnmiRestconfExampleTest {

    private static final Duration REQUEST_TIMEOUT_DURATION = Duration.ofMillis(10_000L);
    private static final Duration POLL_INTERVAL_DURATION = Duration.ofMillis(1_000L);
    private static final Duration WAIT_TIME_DURATION = Duration.ofMillis(10_000L);
    private static final String CA_CERTIFICATE_PATH = "src/main/assembly/resources/certificates/ca.crt";
    private static final String CLIENT_CERTIFICATE_PATH = "src/main/assembly/resources/certificates/client.crt";
    private static final String CLIENT_KEY_PATH = "src/main/assembly/resources/certificates/client.key";

    private static final String DEVICE_ID = "gnmi-simulator";
    private static final int DEVICE_PORT = 3333;
    private static final String DEVICE_IP = "127.0.0.1";
    private static final String KEYSTORE_ID = "keystore-id-1";
    private static final String USERNAME = "Admin";
    private static final String PASSWORD = "Admin";
    private static final String SYSTEM_AUTHENTICATION = "openconfig-system:authentication";
    private static final String STATE = "state";
    private static final String CONFIG = "config";
    private static final String AUTHENTICATION_METHOD = "authentication-method";

    private static final String RESTCONF_PATH = "http://localhost:8888/restconf";
    private static final String ADD_CERTIFICATE_PATH
            = RESTCONF_PATH + "/operations/gnmi-certificate-storage:add-keystore-certificate";
    private static final String MOUNTPOINT_PATH
            = RESTCONF_PATH + "/data/network-topology:network-topology/topology=gnmi-topology/node=" + DEVICE_ID;
    private static final String MOUNTPOINT_STATUS_PATH = MOUNTPOINT_PATH + "/gnmi-topology:node-state/node-status";
    private static final String NODE_STATUS_RESPONSE_READY = "{\"gnmi-topology:node-status\":\"READY\"}";
    private static final String GET_INTERFACES = MOUNTPOINT_PATH + "/yang-ext:mount/openconfig-interfaces:interfaces";
    private static final String GET_SYSTEM = MOUNTPOINT_PATH + "/yang-ext:mount/openconfig-system:system";
    private static final String GET_AUTHENTICATION
            = MOUNTPOINT_PATH + "/yang-ext:mount/openconfig-system:system/aaa/authentication";
    private static final String GET_AUTHENTICATION_CONFIG = GET_AUTHENTICATION + "/config" ;
    private static final String GET_CONFIG_AUTHENTICATION_CONFIG = GET_AUTHENTICATION_CONFIG + "?content=config" ;
    private static final String GET_CONFIG_AUTHENTICATION = GET_AUTHENTICATION + "?content=config";

    private static ExecutorService httpClientExecutor;
    private static HttpClient httpClient;

    @BeforeAll
    public static void startUp() throws IOException {
        httpClientExecutor = Executors.newSingleThreadExecutor();
        httpClient = HttpClient.newBuilder().executor(httpClientExecutor).build();

        Main.main(new String[]{});
    }

    @AfterAll
    public static void teardown() {
        StringBuilder exceptionInfo = new StringBuilder();
        boolean successfullyClosedResources = true;
        try {
            sendDeleteRequest(MOUNTPOINT_PATH);
        } catch (InterruptedException | IOException e) {
            exceptionInfo.append(e.getMessage());
            successfullyClosedResources = false;
        } finally {
            httpClientExecutor.shutdownNow();
        }
        assertTrue(successfullyClosedResources, exceptionInfo.toString());
    }

    @Test
    public void readmeExampleTest() throws IOException, InterruptedException {
        // Register keystore.
        final String certificatesRequestBody = getCertificatesRequestBody(getResources(CA_CERTIFICATE_PATH),
                getResources(CLIENT_KEY_PATH), getResources(CLIENT_CERTIFICATE_PATH));
        final HttpResponse<String> addCertificatesResponse
                = sendPostRequest(ADD_CERTIFICATE_PATH, certificatesRequestBody);
        assertEquals(addCertificatesResponse.statusCode(), HttpURLConnection.HTTP_NO_CONTENT);

        // Check if device was not created before.
        HttpResponse<String> notCreatedDeviceResponse = sendGetRequest(MOUNTPOINT_STATUS_PATH);
        if (notCreatedDeviceResponse.statusCode() == HttpURLConnection.HTTP_OK) {
            // Delete device if was created before test.
            sendDeleteRequest(MOUNTPOINT_PATH);
        }

        // Register device.
        final String mountpointRegistrationBody = getMountpointRegistrationBody();
        final HttpResponse<String> mountpointRegistrationResponse
                = sendPutRequest(MOUNTPOINT_PATH, mountpointRegistrationBody);
        assertEquals(mountpointRegistrationResponse.statusCode(), HttpURLConnection.HTTP_CREATED);

        //Verify that mountpoint is created
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    HttpResponse<String> statusResponse = sendGetRequest(MOUNTPOINT_STATUS_PATH);
                    assertEquals(HttpURLConnection.HTTP_OK, statusResponse.statusCode());
                    assertEquals(NODE_STATUS_RESPONSE_READY, statusResponse.body());
                });

        // Verify that can get data from mountpoint.
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    HttpResponse<String> tdGetResponse = sendGetRequest(GET_INTERFACES);
                    assertEquals(HttpURLConnection.HTTP_OK, tdGetResponse.statusCode());
                });
        // Get data from gNMI device and verify that request was successful.
        HttpResponse<String> systemResponse = sendGetRequest(GET_SYSTEM);
        assertEquals(HttpURLConnection.HTTP_OK, systemResponse.statusCode());
        HttpResponse<String> authenticationResponse = sendGetRequest(GET_AUTHENTICATION);
        assertEquals(HttpURLConnection.HTTP_OK, authenticationResponse.statusCode());

        // PUT Authentication data.
        HttpResponse<String> putAuthResponse = sendPutRequest(GET_AUTHENTICATION, getNewAuthenticationData());
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, putAuthResponse.statusCode());

        HttpResponse<String> replacedAuthenticationResponse = sendGetRequest(GET_CONFIG_AUTHENTICATION);
        assertEquals(HttpURLConnection.HTTP_OK, authenticationResponse.statusCode());
        assertEquals(new JSONObject(replacedAuthenticationResponse.body()).toString(),
                new JSONObject(getNewAuthenticationData()).toString());

        // Update config authentication.
        HttpResponse<String> updateAuthResponse
                = sendPatchRequest(GET_AUTHENTICATION_CONFIG, getConfigAuthenticationUpdate());
        assertEquals(HttpURLConnection.HTTP_OK, updateAuthResponse.statusCode());

        // Verify that authentication data are updated.
        HttpResponse<String> updatedAuthenticationResponse = sendGetRequest(GET_CONFIG_AUTHENTICATION);
        JSONObject actualAuthenticationJson
                = new JSONObject(updatedAuthenticationResponse.body()).getJSONObject(SYSTEM_AUTHENTICATION);
        JSONObject expectedAuthenticationJson
                = new JSONObject(updatedAuthentication()).getJSONObject(SYSTEM_AUTHENTICATION);
        assertEquals(expectedAuthenticationJson.getJSONObject(STATE).toString(),
                actualAuthenticationJson.getJSONObject(STATE).toString());

        JSONArray actualAuthArray = actualAuthenticationJson.getJSONObject(CONFIG).getJSONArray(AUTHENTICATION_METHOD);
        JSONArray expectedAuthArray
                = expectedAuthenticationJson.getJSONObject(CONFIG).getJSONArray(AUTHENTICATION_METHOD);
        assertEquals(getSortedJsonArray(expectedAuthArray), getSortedJsonArray(actualAuthArray));

        // Verify that config authentication data are present.
        HttpResponse<String> existingAuthConfigResponse = sendGetRequest(GET_CONFIG_AUTHENTICATION_CONFIG);
        assertEquals(HttpURLConnection.HTTP_OK, existingAuthConfigResponse.statusCode());

        // Delete config authentication.
        HttpResponse<String> deleteAuthenticationResponse = sendDeleteRequest(GET_AUTHENTICATION_CONFIG);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, deleteAuthenticationResponse.statusCode());

        // Verify that config authentication was deleted.
        HttpResponse<String> getDeletedAuthenticationResponse = sendGetRequest(GET_CONFIG_AUTHENTICATION_CONFIG);
        assertEquals(HttpURLConnection.HTTP_CONFLICT, getDeletedAuthenticationResponse.statusCode());
    }

    private static String getResources(final String path) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(path);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        return IOUtils.toString(bufferedInputStream, Charset.defaultCharset());
    }

    private static List<String> getSortedJsonArray(final JSONArray jsonArray) {
        final List<String> list = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.get(i).toString());
        }
        Collections.sort(list);
        return list;
    }

    private String getCertificatesRequestBody(final String ca, final String key, final String clientCert) {
        return "{\n"
                + "    \"input\": {\n"
                + "        \"keystore-id\": \"" + KEYSTORE_ID + "\",\n"
                + "        \"ca-certificate\": \"" + ca + "\",\n"
                + "        \"client-key\": \"" + key + "\",\n"
                + "        \"client-cert\": \"" + clientCert + "\"\n"
                + "    }\n"
                + "}";
    }

    private String getNewAuthenticationData() {
        return "{\n"
                + "    \"openconfig-system:authentication\": {\n"
                + "        \"config\": {\n"
                + "            \"authentication-method\": [\n"
                + "                \"openconfig-aaa-types:TACACS_ALL\"\n"
                + "            ]\n"
                + "        },\n"
                + "        \"state\": {\n"
                + "            \"authentication-method\": [\n"
                + "                \"openconfig-aaa-types:RADIUS_ALL\"\n"
                + "            ]\n"
                + "        }\n"
                + "    }\n"
                + "}";
    }

    private String updatedAuthentication() {
        return "{\n"
                + "    \"openconfig-system:authentication\": {\n"
                + "        \"config\": {\n"
                + "            \"authentication-method\": [\n"
                + "                \"openconfig-aaa-types:TACACS_ALL\",\n"
                + "                \"openconfig-aaa-types:RADIUS_ALL\"\n"
                + "            ]\n"
                + "        },\n"
                + "        \"state\": {\n"
                + "            \"authentication-method\": [\n"
                + "                \"openconfig-aaa-types:RADIUS_ALL\"\n"
                + "            ]\n"
                + "        }\n"
                + "    }\n"
                + "}";
    }

    private String getConfigAuthenticationUpdate() {
        return  "{\n"
                + "    \"openconfig-system:config\": {\n"
                + "        \"authentication-method\": [\n"
                + "            \"openconfig-aaa-types:RADIUS_ALL\"\n"
                + "        ]\n"
                + "    }\n"
                + "}";
    }

    private String getMountpointRegistrationBody() {
        return "{\n"
                + "    \"node\": [\n"
                + "        {\n"
                + "            \"node-id\": \"" + DEVICE_ID + "\",\n"
                + "            \"connection-parameters\": {\n"
                + "                \"host\": \"" + DEVICE_IP + "\",\n"
                + "                \"port\": " + DEVICE_PORT + ",\n"
                + "                \"keystore-id\" : \"" + KEYSTORE_ID + "\",\n"
                + "                \"credentials\": {\n"
                + "                    \"username\": \"" + USERNAME + "\",\n"
                + "                    \"password\": \"" + PASSWORD + "\"\n"
                + "                }\n"
                + "            },\n"
                + "            \"extensions-parameters\": {\n"
                + "                \"gnmi-parameters\": {\n"
                + "                    \"use-model-name-prefix\": true\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "    ]\n"
                + "})";
    }

    private static HttpResponse<String> sendDeleteRequest(final String path) throws InterruptedException, IOException {
        final HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(path))
                .header("Content-Type", "application/json")
                .DELETE()
                .timeout(REQUEST_TIMEOUT_DURATION)
                .build();
        return httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> sendPutRequest(final String path, final String payload)
            throws InterruptedException, IOException {
        final HttpRequest putRequest = HttpRequest.newBuilder()
                .uri(URI.create(path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(payload))
                .timeout(REQUEST_TIMEOUT_DURATION)
                .build();
        return httpClient.send(putRequest, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> sendPostRequest(final String path, final String payload)
            throws InterruptedException, IOException {
        final HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .timeout(REQUEST_TIMEOUT_DURATION)
                .build();
        return httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> sendGetRequest(final String path) throws InterruptedException, IOException {
        final HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(path))
                .header("Content-Type", "application/json")
                .GET()
                .timeout(REQUEST_TIMEOUT_DURATION)
                .build();
        return httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> sendPatchRequest(final String path, final String payload)
            throws InterruptedException, IOException {
        final HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(path))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(payload))
                .timeout(REQUEST_TIMEOUT_DURATION)
                .build();
        return httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
    }
}
