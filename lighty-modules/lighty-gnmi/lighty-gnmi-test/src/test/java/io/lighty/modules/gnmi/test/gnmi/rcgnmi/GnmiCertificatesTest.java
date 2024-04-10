/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.test.gnmi.rcgnmi;

import static io.lighty.modules.gnmi.test.gnmi.rcgnmi.GnmiITBase.GeneralConstants.GNMI_NODE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.utils.EffectiveModelContextBuilder.EffectiveModelContextBuilderException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.awaitility.Awaitility;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnmiCertificatesTest extends GnmiITBase {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiCertificatesTest.class);
    private static final TestCertificates TEST_CERTIFICATES = new TestCertificates();

    private static final String KEY_PATH = "src/test/resources/certs/server-pkcs8.key";
    private static final String CERTIFICATE_PATH = "src/test/resources/certs/server.crt";

    private static final String ADD_CERTIFICATE_PATH
            = "http://localhost:8888/restconf/operations/gnmi-certificate-storage:add-keystore-certificate";
    private static final String REMOVE_CERTIFICATE_PATH
            = "http://127.0.0.1:8888/restconf/operations/gnmi-certificate-storage:remove-keystore-certificate";
    private static final String GET_CERTIFICATE_PATH
            = "http://localhost:8888/restconf/data/gnmi-certificate-storage:keystore=%s";
    private static final String CREATE_MOUNTPOINT_PATH
            = "http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=%s";
    private static final String TEST_DATA_PATH
            = "http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=%s/"
          + "yang-ext:mount/openconfig-interfaces:interfaces";
    private static final String MOUNTPOINT_STATUS_PATH
            = CREATE_MOUNTPOINT_PATH + "/gnmi-topology:node-state/node-status";
    private static final String NODE_STATUS_RESPONSE_READY = "{\"gnmi-topology:node-status\":\"READY\"}";
    private static final String NODE_STATUS_TRANSIENT_FAIL = "{\"gnmi-topology:node-status\":\"TRANSIENT_FAILURE\"}";
    private static final String NODE_STATUS_FAIL = "{\"gnmi-topology:node-status\":\"FAILURE\"}";
    private static final String USERNAME = "USER";
    private static final String PASSWORD = "PASS";

    private static SimulatedGnmiDevice device;

    @BeforeAll
    public static void setupDevice() throws ConfigurationException {
        device = getSecureGnmiDevice(DEVICE_IP, DEVICE_PORT, KEY_PATH, CERTIFICATE_PATH, USERNAME, PASSWORD);
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

    @Test
    public void testCertificateRegistration() throws IOException, InterruptedException, JSONException {
        // Register keystore
        final String id = "test-registration";
        final String certificatesRequestBody = getCertificatesRequestBody(id, TEST_CERTIFICATES.getCaCert(),
                TEST_CERTIFICATES.getClientEncKey(), TEST_CERTIFICATES.getPassphrase(),
                TEST_CERTIFICATES.getClientEncCert());
        final HttpResponse<String> response = sendPostRequestJSON(ADD_CERTIFICATE_PATH, certificatesRequestBody);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, response.statusCode());

        //Get keystore data and validate output
        final HttpResponse<String> getResponse = sendGetRequestJSON(String.format(GET_CERTIFICATE_PATH, id));
        assertEquals(HttpURLConnection.HTTP_OK, getResponse.statusCode());
        final String body = getResponse.body();
        JSONObject jsonObject = new JSONObject(body)
                .getJSONArray("gnmi-certificate-storage:keystore").getJSONObject(0);
        assertEquals(jsonObject.get("keystore-id"), id);
        assertEquals(jsonObject.get("ca-certificate"), TEST_CERTIFICATES.getCaCert());
        assertEquals(jsonObject.get("client-cert"), TEST_CERTIFICATES.getClientEncCert());

        assertNotEquals(jsonObject.get("client-key"), TEST_CERTIFICATES.getClientEncKey());
        assertNotEquals(jsonObject.get("passphrase"), TEST_CERTIFICATES.getPassphrase());

        // Remove keystore
        final HttpResponse<String> removeResponse
                = sendPostRequestJSON(REMOVE_CERTIFICATE_PATH, getRemoveCertificateBody(id));
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, removeResponse.statusCode());

        final HttpResponse<String> getRemovedCertResponse = sendGetRequestJSON(String.format(GET_CERTIFICATE_PATH, id));
        assertEquals(HttpURLConnection.HTTP_CONFLICT, getRemovedCertResponse.statusCode());
    }

    @Test
    public void testWrongCertificateRegistration() throws IOException, InterruptedException {
        // Register keystore
        final String id = "test-wrong-registration";
        final String certificatesRequestBody = getRemoveCertificateBody(id);
        final HttpResponse<String> response = sendPostRequestJSON(ADD_CERTIFICATE_PATH, certificatesRequestBody);
        assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, response.statusCode());

        //Get keystore data and validate output
        final HttpResponse<String> getResponse = sendGetRequestJSON(String.format(GET_CERTIFICATE_PATH, id));
        assertEquals(HttpURLConnection.HTTP_CONFLICT, getResponse.statusCode());
    }

    @Test
    public void connectDeviceWithCertificates() throws IOException, InterruptedException {
        // Register keystore
        final String keystoreId = "test-certificate";
        final String certificatesRequestBody = getCertificatesRequestBody(keystoreId, TEST_CERTIFICATES.getCaCert(),
                TEST_CERTIFICATES.getClientKey(), TEST_CERTIFICATES.getClientCert());
        final HttpResponse<String> response = sendPostRequestJSON(ADD_CERTIFICATE_PATH, certificatesRequestBody);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, response.statusCode());

        //Register device
        final String mRegBody = getMountpointRegistrationBody(GNMI_NODE_ID, keystoreId);
        final HttpResponse<String> mRegResponse
                = sendPutRequestJSON(String.format(CREATE_MOUNTPOINT_PATH, GNMI_NODE_ID), mRegBody);
        assertEquals(HttpURLConnection.HTTP_CREATED, mRegResponse.statusCode());

        //Verify that mountpoint is created
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    HttpResponse<String> statusResponse
                            = sendGetRequestJSON(String.format(MOUNTPOINT_STATUS_PATH, GNMI_NODE_ID));
                    Assertions.assertEquals(HttpURLConnection.HTTP_OK, statusResponse.statusCode());
                    Assertions.assertEquals(NODE_STATUS_RESPONSE_READY, statusResponse.body());
                });

        //Verify that can get data from mountpoint
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    HttpResponse<String> tdGetResponse
                            = sendGetRequestJSON(String.format(TEST_DATA_PATH, GNMI_NODE_ID));
                    Assertions.assertEquals(HttpURLConnection.HTTP_OK, tdGetResponse.statusCode());
                });
    }

    @Test
    public void connectDeviceWithEncryptedCertificate() throws IOException, InterruptedException {
        // Register keystore
        final String keystoreId = "test-encrypted-certificate";
        final String certificatesRequestBody = getCertificatesRequestBody(keystoreId, TEST_CERTIFICATES.getCaCert(),
                TEST_CERTIFICATES.getClientEncKey(), TEST_CERTIFICATES.getPassphrase(),
                TEST_CERTIFICATES.getClientEncCert());
        final HttpResponse<String> response = sendPostRequestJSON(ADD_CERTIFICATE_PATH, certificatesRequestBody);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, response.statusCode());

        //Register device
        final String mRegBody = getMountpointRegistrationBody(GNMI_NODE_ID, keystoreId);
        final HttpResponse<String> mRegResponse
                = sendPutRequestJSON(String.format(CREATE_MOUNTPOINT_PATH, GNMI_NODE_ID), mRegBody);
        assertEquals(HttpURLConnection.HTTP_CREATED, mRegResponse.statusCode());

        //Verify that mountpoint is created
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    HttpResponse<String> statusResponse
                            = sendGetRequestJSON(String.format(MOUNTPOINT_STATUS_PATH, GNMI_NODE_ID));
                    Assertions.assertEquals(HttpURLConnection.HTTP_OK, statusResponse.statusCode());
                    Assertions.assertEquals(NODE_STATUS_RESPONSE_READY, statusResponse.body());
                });

        //Verify that can get data from mountpoint
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    HttpResponse<String> tdGetResponse
                            = sendGetRequestJSON(String.format(TEST_DATA_PATH, GNMI_NODE_ID));
                    Assertions.assertEquals(HttpURLConnection.HTTP_OK, tdGetResponse.statusCode());
                });
    }

    @Test
    public void connectDeviceWithWrongCaCertificatesAndReconnect() throws IOException, InterruptedException {
        // Register keystore
        final String keystoreId = "test-wrong-ca";
        final String certificatesRequestBody = getCertificatesRequestBody(keystoreId,
                TEST_CERTIFICATES.getWrongCaCert(), TEST_CERTIFICATES.getClientEncKey(),
                TEST_CERTIFICATES.getPassphrase(), TEST_CERTIFICATES.getClientCert());

        final HttpResponse<String> response = sendPostRequestJSON(ADD_CERTIFICATE_PATH, certificatesRequestBody);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, response.statusCode());

        //Register device
        final String mRegBody = getMountpointRegistrationBody(GNMI_NODE_ID, keystoreId);
        final HttpResponse<String> mRegResponse
                = sendPutRequestJSON(String.format(CREATE_MOUNTPOINT_PATH, GNMI_NODE_ID), mRegBody);
        assertEquals(HttpURLConnection.HTTP_CREATED, mRegResponse.statusCode());

        //Verify that mountpoint can't reach device
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    HttpResponse<String> statusResponse
                            = sendGetRequestJSON(String.format(MOUNTPOINT_STATUS_PATH, GNMI_NODE_ID));
                    Assertions.assertEquals(HttpURLConnection.HTTP_OK, statusResponse.statusCode());
                    Assertions.assertEquals(NODE_STATUS_TRANSIENT_FAIL, statusResponse.body());
                });

        //Verify that mountpoint doesn't provide any data
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    HttpResponse<String> tdGetResponse
                            = sendGetRequestJSON(String.format(TEST_DATA_PATH, GNMI_NODE_ID));
                    Assertions.assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, tdGetResponse.statusCode());
                    assertTrue(tdGetResponse.body().contains("Mount point") && tdGetResponse.body()
                            .contains("does not exist"));
                });

        // Register correct keystore
        final String correctKeystoreId = "test-correct-ca";
        final String correctCertReqBody = getCertificatesRequestBody(correctKeystoreId, TEST_CERTIFICATES.getCaCert(),
                TEST_CERTIFICATES.getClientEncKey(), TEST_CERTIFICATES.getPassphrase(),
                TEST_CERTIFICATES.getClientCert());
        final HttpResponse<String> correctCerResponse = sendPostRequestJSON(ADD_CERTIFICATE_PATH, correctCertReqBody);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, correctCerResponse.statusCode());

        //Reload device with correct information
        final String mCorrectRegBody = getMountpointRegistrationBody(GNMI_NODE_ID, correctKeystoreId);
        final HttpResponse<String> mCorrectResponse
                = sendPutRequestJSON(String.format(CREATE_MOUNTPOINT_PATH, GNMI_NODE_ID), mCorrectRegBody);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, mCorrectResponse.statusCode());

        //Verify that mountpoint is created
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    HttpResponse<String> statusResponse
                            = sendGetRequestJSON(String.format(MOUNTPOINT_STATUS_PATH, GNMI_NODE_ID));
                    Assertions.assertEquals(HttpURLConnection.HTTP_OK, statusResponse.statusCode());
                    Assertions.assertEquals(NODE_STATUS_RESPONSE_READY, statusResponse.body());
                });

        //Verify that can get data from mountpoint
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    HttpResponse<String> tdGetResponse
                            = sendGetRequestJSON(String.format(TEST_DATA_PATH, GNMI_NODE_ID));
                    Assertions.assertEquals(HttpURLConnection.HTTP_OK, tdGetResponse.statusCode());
                });
    }

    @Test
    public void connectDeviceWithoutPassphrase() throws IOException, InterruptedException {
        // Register keystore
        final String keystoreId = "test-without-passphrase";
        final String certificatesRequestBody = getCertificatesRequestBody(keystoreId, TEST_CERTIFICATES.getCaCert(),
                TEST_CERTIFICATES.getClientEncKey(), TEST_CERTIFICATES.getClientCert());

        final HttpResponse<String> response = sendPostRequestJSON(ADD_CERTIFICATE_PATH, certificatesRequestBody);
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, response.statusCode());

        //Register device
        final String mRegBody = getMountpointRegistrationBody(GNMI_NODE_ID, keystoreId);
        final HttpResponse<String> mRegResponse
                = sendPutRequestJSON(String.format(CREATE_MOUNTPOINT_PATH, GNMI_NODE_ID), mRegBody);
        assertEquals(HttpURLConnection.HTTP_CREATED, mRegResponse.statusCode());

        //Verify that mountpoint can't be created
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    HttpResponse<String> statusResponse
                            = sendGetRequestJSON(String.format(MOUNTPOINT_STATUS_PATH, GNMI_NODE_ID));
                    assertEquals(HttpURLConnection.HTTP_OK, statusResponse.statusCode());
                    assertEquals(NODE_STATUS_FAIL, statusResponse.body());
                });

        //Verify that mountpoint doesn't provide any data
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    HttpResponse<String> tdGetResponse
                            = sendGetRequestJSON(String.format(TEST_DATA_PATH, GNMI_NODE_ID));
                    assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, tdGetResponse.statusCode());
                    assertTrue(tdGetResponse.body().contains("Mount point") && tdGetResponse.body()
                            .contains("does not exist"));
                });
    }

    private String getMountpointRegistrationBody(final String nodeId, final String keystoreId) {
        return String.format("{\n"
              + "    \"node\": [\n"
              + "        {\n"
              + "            \"node-id\": \"%s\",\n"
              + "            \"connection-parameters\": {\n"
              + "                \"host\": \"%s\",\n"
              + "                \"port\": %s,\n"
              + "                \"keystore-id\" : \"%s\",\n"
              + "                \"credentials\": {\n"
              + "                    \"username\": \"%s\",\n"
              + "                    \"password\": \"%s\"\n"
              + "                }\n"
              + "            },\n"
              + "            \"extensions-parameters\": {\n"
              + "                \"gnmi-parameters\": {\n"
              + "                    \"use-model-name-prefix\": true\n"
              + "                }\n"
              + "            }"
              + "        }\n"
              + "    ]\n"
              + "}", nodeId, DEVICE_IP, DEVICE_PORT, keystoreId, USERNAME, PASSWORD);
    }

    private String getCertificatesRequestBody(final String id, final String ca, final String key,
                                              final String passphrase, final String clientCert) {
        return String.format("{\n"
                + "    \"input\": {\n"
                + "        \"keystore-id\": \"%s\",\n"
                + "        \"ca-certificate\": \"%s\",\n"
                + "        \"client-key\": \"%s\",\n"
                + "        \"passphrase\": \"%s\",\n"
                + "        \"client-cert\": \"%s\"\n"
                + "    }\n"
                + "}", id, ca, key, passphrase, clientCert);
    }

    private String getCertificatesRequestBody(final String id, final String ca, final String key,
                                              final String clientCert) {
        return String.format("{\n"
                + "    \"input\": {\n"
                + "        \"keystore-id\": \"%s\",\n"
                + "        \"ca-certificate\": \"%s\",\n"
                + "        \"client-key\": \"%s\",\n"
                + "        \"client-cert\": \"%s\"\n"
                + "    }\n"
                + "}", id, ca, key, clientCert);
    }

    private String getRemoveCertificateBody(final String keystoreId) {
        return String.format("{\n"
                + "    \"input\": {\n"
                + "        \"keystore-id\": \"%s\"\n"
                + "    }\n"
                + "}", keystoreId);
    }

    private static String getResource(final String path) {
        try {
            final byte[] bytes = Files.readAllBytes(Paths.get(GnmiCertificatesTest.class.getResource(path).toURI()));
            return new String(bytes);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(String.format("Failed to read resources at path [%s]", path), e);
        }
    }

    private static class TestCertificates {
        private static final String CLIENT_CERT = "/certs/client.crt";
        private static final String CA = "/certs/ca.crt";
        private static final String CLIENT_KEY = "/certs/client.key";
        private static final String PASSPHRASE = "/certs/client_key_passphrase.txt";
        private static final String CLIENT_ENC_KEY = "/certs/client.encrypted.key";
        private static final String CLIENT_ENC_CERT = "/certs/client.encrypted.crt";
        private static final String WRONG_CA_CRT = "/certs/wrong_ca.crt";

        private final String clientCert;
        private final String caCert;
        private final String clientKey;
        private final String passphrase;
        private final String clientEncKey;
        private final String clientEncCert;
        private final String wrongCaCert;

        TestCertificates() {
            this.clientCert = getResource(CLIENT_CERT);
            this.caCert = getResource(CA);
            this.clientKey = getResource(CLIENT_KEY);
            this.passphrase = getResource(PASSPHRASE);
            this.clientEncKey = getResource(CLIENT_ENC_KEY);
            this.clientEncCert = getResource(CLIENT_ENC_CERT);
            this.wrongCaCert = getResource(WRONG_CA_CRT);
        }

        public String getClientCert() {
            return clientCert;
        }

        public String getCaCert() {
            return caCert;
        }

        public String getClientKey() {
            return clientKey;
        }

        public String getPassphrase() {
            return passphrase;
        }

        public String getClientEncKey() {
            return clientEncKey;
        }

        public String getClientEncCert() {
            return clientEncCert;
        }

        public String getWrongCaCert() {
            return wrongCaCert;
        }
    }
}
