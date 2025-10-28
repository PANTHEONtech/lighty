/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.kit.examples.community.tests;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;

import io.lighty.kit.examples.community.aaa.restconf.Main;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AAATestIT {

    private static final Logger LOG = LoggerFactory.getLogger(AAATestIT.class);
    public static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000;

    private static final String PATH_PREFIX = "http://localhost:8888/";
    private static final String AUTH = "Authorization";
    private static final String USERS_PATH = "auth/v1/users/";
    private static final String DOMAIN_SDN_USERS_PATH = PATH_PREFIX + "auth/v1/domains/sdn/users/";
    private static final String ALL_USERS_PATH = PATH_PREFIX + USERS_PATH;
    private static final String USERS = "users";
    private static final String ROLES = "roles";
    private static final String BASIC = "Basic ";
    private static final String NAME = "name";
    private static final String DESC = "description";
    private static final String USER_ID = "userid";
    private static final String ROLE_ID = "roleid";
    private static final String GRANT_ID = "grantid";
    private static final String NEW_USER_GRANT_ID = "newUser@sdn@admin@sdn@sdn";
    private static final String ADMIN = "admin";
    private static final String ADMIN_SDN = "admin@sdn";
    private static final String ADMIN_DESCRIPTION = "admin user";
    private static final String NEW_USER = "newUser";
    private static final String NEW_USER_SDN = "newUser@sdn";
    private static String NEW_USER_DATA;
    private static String UPDATE_USER_DATA;
    private static String GRANT_ADMIN_ROLE_DATA;

    private String adminAuth;
    private String wrongAuth;
    private String newUserAuth;
    private String newUserAuthUpdated;
    private Connection adminConnectionCorrect;
    private HttpClient httpClient;
    private HttpClient httpClientOther;
    private HttpClient httpClientWrongCredentials;
    private Main main;

    @BeforeClass
    public void initClass() throws Exception {
        LOG.info("init restconf and controller");
        this.main = new Main();
        this.main.start(new String[]{}, false);

        LOG.info("controller and restconf started successfully");
        adminAuth = BASIC + Base64.getEncoder().encodeToString((ADMIN + ":admin").getBytes(UTF_8));
        wrongAuth = BASIC + Base64.getEncoder().encodeToString(("foo:bar").getBytes(UTF_8));
        newUserAuth = BASIC + Base64.getEncoder().encodeToString((NEW_USER + ":verySecretPassword").getBytes(UTF_8));
        newUserAuthUpdated = BASIC + Base64.getEncoder()
            .encodeToString((NEW_USER + ":evenMoreSecretPass").getBytes(UTF_8));

        NEW_USER_DATA = TestUtils.readResource("/testdata/new-user.json");
        UPDATE_USER_DATA = TestUtils.readResource("/testdata/update-user.json");
        GRANT_ADMIN_ROLE_DATA = TestUtils.readResource("/testdata/grant-admin-role-data.json");
    }

    @BeforeMethod
    public void init() throws Exception {
        LOG.info("start all http clients");
        httpClient = new HttpClient();
        httpClient.setIdleTimeout(60000 * 60);
        httpClient.start();
        adminConnectionCorrect = new Connection(httpClient, adminAuth);

        httpClientWrongCredentials = new HttpClient();
        httpClientWrongCredentials.setIdleTimeout(60000 * 60);
        httpClientWrongCredentials.start();

        httpClientOther = new HttpClient();
        httpClientOther.setIdleTimeout(60000 * 60);
        httpClientOther.start();
    }

    @Test
    public void getAndCheckDefaultAdminUsersTest() throws Exception {
        LOG.info("Get all user tests");

        assertUsersExist(adminConnectionCorrect, UserDetails.of(ADMIN, ADMIN_DESCRIPTION, ADMIN_SDN));

        final Connection userConnectionWrong = new Connection(httpClientWrongCredentials, wrongAuth);
        final Connection userConnectionCorrect = new Connection(httpClientOther, newUserAuth);

        assertEquals(getAllUsers(adminConnectionCorrect).getStatus(), HttpStatus.OK_200);
        assertEquals(getAllUsers(userConnectionWrong).getStatus(), HttpStatus.UNAUTHORIZED_401);
        assertEquals(getAllUsers(userConnectionCorrect).getStatus(), HttpStatus.UNAUTHORIZED_401);
    }

    @Test
    public void addUserTest() throws Exception {
        LOG.info("Add new user test");

        final ContentResponse getAllUsersExpectOne = getAllUsers(adminConnectionCorrect);
        assertEquals(getAllUsersExpectOne.getStatus(), HttpStatus.OK_200);
        assertEquals(getUsersArrayFromResponse(getAllUsersExpectOne).length(), 1);

        final ContentResponse addUserResponse = addUser(adminConnectionCorrect, NEW_USER_DATA);
        assertEquals(addUserResponse.getStatus(), HttpStatus.CREATED_201);
        final UserDetails newUserDetails = getUsersDetailsFromResponse(addUserResponse);
        assertEquals(newUserDetails.userid, NEW_USER_SDN);

        final ContentResponse getAllUsersResponse = getAllUsers(adminConnectionCorrect);
        assertEquals(getAllUsersResponse.getStatus(), HttpStatus.OK_200);
        assertEquals(getUsersArrayFromResponse(getAllUsersResponse).length(), 2);

        final Connection userConnectionUnauthorized = new Connection(httpClientOther, this.newUserAuth);
        assertEquals(getAllUsers(userConnectionUnauthorized).getStatus(), HttpStatus.UNAUTHORIZED_401);

        // grant admin role
        final ContentResponse grantUserResponse =
            grantUser(adminConnectionCorrect, NEW_USER_SDN, GRANT_ADMIN_ROLE_DATA);
        assertEquals(grantUserResponse.getStatus(), HttpStatus.CREATED_201);
        final String newUserGrantIdValue = new JSONObject(grantUserResponse.getContentAsString()).getString(GRANT_ID);
        assertEquals(newUserGrantIdValue, NEW_USER_GRANT_ID);

        final Connection userConnectionGranted = new Connection(httpClientOther, this.newUserAuth);
        final Connection userConnectionWrong = new Connection(httpClientWrongCredentials, this.wrongAuth);
        assertEquals(getAllUsers(userConnectionGranted).getStatus(), HttpStatus.OK_200);
        assertEquals(getAllUsers(userConnectionWrong).getStatus(), HttpStatus.UNAUTHORIZED_401);
    }

    @Test
    public void getSpecificUsersTest() throws Exception {
        LOG.info("get specific user test");
        assertEquals(addUser(adminConnectionCorrect, NEW_USER_DATA).getStatus(), HttpStatus.CREATED_201);

        final ContentResponse getNewUserResponse = getSpecificUser(adminConnectionCorrect, NEW_USER_SDN);
        assertEquals(getNewUserResponse.getStatus(), HttpStatus.OK_200);
        final UserDetails newUserDetails = getUsersDetailsFromResponse(getNewUserResponse);
        assertEquals(newUserDetails.userid, NEW_USER_SDN);
        assertEquals(newUserDetails.name, NEW_USER);

        final ContentResponse getAdminUserResponse = getSpecificUser(adminConnectionCorrect, ADMIN_SDN);
        assertEquals(getAdminUserResponse.getStatus(), HttpStatus.OK_200);
        final UserDetails adminDetails = getUsersDetailsFromResponse(getAdminUserResponse);
        assertEquals(adminDetails.userid, ADMIN_SDN);
        assertEquals(adminDetails.name, ADMIN);
    }

    @Test
    public void updateUserInfoTest() throws Exception {
        LOG.info("Update user data and try to use them");
        assertEquals(addUser(adminConnectionCorrect, NEW_USER_DATA).getStatus(), HttpStatus.CREATED_201);
        assertEquals(updateUser(adminConnectionCorrect, NEW_USER_SDN, UPDATE_USER_DATA).getStatus(), HttpStatus.OK_200);

        final ContentResponse getNewUserResponse = getSpecificUser(adminConnectionCorrect, NEW_USER_SDN);
        assertEquals(getNewUserResponse.getStatus(), HttpStatus.OK_200);
        final UserDetails newUserDetails = getUsersDetailsFromResponse(getNewUserResponse);
        assertEquals(newUserDetails.userid, NEW_USER_SDN);
        assertEquals(newUserDetails.name, NEW_USER);

        //grant admin role
        final ContentResponse grantUserResponse =
                grantUser(adminConnectionCorrect, NEW_USER_SDN, GRANT_ADMIN_ROLE_DATA);
        assertEquals(grantUserResponse.getStatus(), HttpStatus.CREATED_201);
        final String newUserGrantIdValue = new JSONObject(grantUserResponse.getContentAsString()).getString(GRANT_ID);
        assertEquals(newUserGrantIdValue, NEW_USER_GRANT_ID);

        final Connection userConnectionAuthUpdated = new Connection(httpClientOther, this.newUserAuthUpdated);
        assertEquals(getAllUsers(userConnectionAuthUpdated).getStatus(), HttpStatus.OK_200);
    }

    @Test
    public void deleteUserTest() throws Exception {
        LOG.info("delete user");
        assertEquals(addUser(adminConnectionCorrect, NEW_USER_DATA).getStatus(), HttpStatus.CREATED_201);

        assertEquals(deleteUser(adminConnectionCorrect, NEW_USER_SDN).getStatus(), HttpStatus.NO_CONTENT_204);
        assertUsersExist(adminConnectionCorrect, UserDetails.of(ADMIN, ADMIN_DESCRIPTION, ADMIN_SDN));
    }

    @Test
    public void readNotExistingUserExpectError() throws Exception {
        LOG.info("get specific not existing user");
        assertEquals(getSpecificUser(adminConnectionCorrect, NEW_USER).getStatus(), HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void deleteNotExistingUserExpectError() throws Exception {
        LOG.info("delete specific not existing user");
        assertEquals(deleteUser(adminConnectionCorrect, NEW_USER_SDN).getStatus(), HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void readDataCorrectCredentials() throws Exception {
        LOG.info("try to get modules state with correct credentials");
        assertEquals(getSomeData(adminConnectionCorrect).getStatus(), HttpStatus.OK_200);
    }

    @Test
    public void readDataWrongCredentials() throws Exception {
        LOG.info("try to get modules state with incorrect credentials");
        final Connection adminConnectionWrong = new Connection(httpClient, this.wrongAuth);
        assertEquals(getSomeData(adminConnectionWrong).getStatus(), HttpStatus.UNAUTHORIZED_401);
    }

    private ContentResponse addUser(final Connection connection, String userData)
        throws InterruptedException, ExecutionException, TimeoutException {
        return sendPostRequestJSON(connection, ALL_USERS_PATH, userData);
    }

    private ContentResponse grantUser(final Connection connection, String user, String grantData)
        throws InterruptedException, ExecutionException, TimeoutException {
        return sendPostRequestJSON(connection,
            DOMAIN_SDN_USERS_PATH + user + "/roles",
            grantData);
    }

    private ContentResponse updateUser(final Connection connection, String user, String updatedData)
        throws InterruptedException, ExecutionException, TimeoutException {
        return connection.getHttpClient().newRequest(ALL_USERS_PATH + user)
            .method(HttpMethod.PUT)
            .header(AAATestIT.AUTH, connection.getBasicAuthEncoded())
            .content(new StringContentProvider(updatedData), MediaType.APPLICATION_JSON)
            .send();
    }

    private JSONArray getUsersArrayFromResponse(ContentResponse response) {
        return new JSONObject(response.getContentAsString()).getJSONArray(USERS);
    }

    private UserDetails getUsersDetailsFromResponse(ContentResponse response) {
        final JSONObject user = new JSONObject(response.getContentAsString());
        return UserDetails.of(user.getString(NAME), user.getString(DESC), user.getString(USER_ID));
    }

    private ContentResponse getAllUsers(final Connection connection)
            throws Exception {
        return sendGetRequestJSON(connection, ALL_USERS_PATH);
    }

    private ContentResponse getSomeData(final Connection connection)
            throws Exception {
        return sendGetRequestJSON(
            connection,
            PATH_PREFIX + "restconf/data/ietf-yang-library:modules-state");
    }

    private ContentResponse deleteUser(final Connection connection, String user)
            throws Exception {
        return sendDeleteRequestJSON(connection, ALL_USERS_PATH + user);
    }

    private void assertUsersExist(Connection connection, final UserDetails... users)
            throws Exception {
        final ContentResponse getUsersResponse = sendGetRequestJSON(connection, ALL_USERS_PATH);
        // 2. Check if their info is as expected
        assertEquals(getUsersResponse.getStatus(), HttpStatus.OK_200);
        final JSONObject responseObject = new JSONObject(getUsersResponse.getContentAsString());
        final JSONArray usersJson = responseObject.getJSONArray(USERS);
        assertEquals(usersJson.length(), usersJson.length());
        for (int i = 0; i < usersJson.length(); i++) {
            final JSONObject userJson = (JSONObject) responseObject.getJSONArray(USERS).get(i);
            boolean found = false;
            for (final UserDetails user : users) {
                if (userJson.getString(NAME).equals(user.name)
                    && userJson.getString(DESC).equals(user.description)) {
                    found = true;
                }
            }
            Assert.assertTrue(found);
        }
    }

    private void checkAndDeleteNonAdminUsers() throws Exception {
        final ContentResponse response = sendGetRequestJSON(adminConnectionCorrect, ALL_USERS_PATH);
        assertEquals(response.getStatus(), HttpStatus.OK_200);
        final JSONArray users = new JSONObject(response.getContentAsString()).getJSONArray(USERS);
        if (users.length() > 1) {
            for (int i = 0; i < users.length(); i++) {
                final JSONObject user = (JSONObject) users.get(i);
                if (!user.getString(NAME).equals(ADMIN) && !user.getString(DESC).equals("admin user")) {
                    ContentResponse responseRoles = sendGetRequestJSON(
                            adminConnectionCorrect,
                            DOMAIN_SDN_USERS_PATH
                                + user.getString(USER_ID) + "/roles");
                    final JSONArray roles = new JSONObject(responseRoles.getContentAsString()).getJSONArray(ROLES);
                    for (int j = 0; j < roles.length(); j++) {
                        final JSONObject role = (JSONObject) roles.get(j);
                        sendDeleteRequestJSON(adminConnectionCorrect,
                            DOMAIN_SDN_USERS_PATH
                                + user.getString(USER_ID) + "/roles/" + role.getString(ROLE_ID));
                    }
                    sendDeleteRequestJSON(adminConnectionCorrect,
                        ALL_USERS_PATH + user.getString(USER_ID));
                }
            }
        }
    }

    private ContentResponse getSpecificUser(final Connection connection, final String user)
        throws Exception {
        return sendGetRequestJSON(connection, ALL_USERS_PATH + user);
    }

    private ContentResponse sendPostRequestJSON(final Connection connection, String path, String payload)
        throws InterruptedException, ExecutionException, TimeoutException {
        return connection.getHttpClient().newRequest(path)
            .method(HttpMethod.POST)
            .header(AAATestIT.AUTH, connection.getBasicAuthEncoded())
            .content(new StringContentProvider(payload), MediaType.APPLICATION_JSON)
            .send();
    }

    private ContentResponse sendGetRequestJSON(final Connection connection, String path)
        throws InterruptedException, ExecutionException, TimeoutException {
        return connection.getHttpClient().newRequest(path)
            .method(HttpMethod.GET)
            .header(AAATestIT.AUTH, connection.getBasicAuthEncoded())
            .send();
    }

    private ContentResponse sendDeleteRequestJSON(final Connection connection, String path)
        throws InterruptedException, ExecutionException, TimeoutException {
        return connection.getHttpClient().newRequest(path)
            .method(HttpMethod.DELETE)
            .header(AAATestIT.AUTH, connection.getBasicAuthEncoded())
            .send();
    }

    @AfterMethod
    public void cleanUp() throws Exception {
        checkAndDeleteNonAdminUsers();
        LOG.info("stopping all the http clients");
        httpClient.stop();
        httpClientOther.stop();
        httpClientWrongCredentials.stop();
    }

    @AfterClass
    public void shutdown() {
        LOG.info("removing db files");
        File currentDirFile = new File(".");
        String lightyTestsPath = currentDirFile.getAbsolutePath();
        try {
            FileUtils.deleteDirectory(new File(lightyTestsPath + "/configuration"));
            FileUtils.deleteDirectory(new File(lightyTestsPath + "/data"));
        } catch (IOException e) {
            LOG.error("Deletion of directory failed", e);
        }


        LOG.info("shutdown.");
        final ExecutorService executorService = Executors.newFixedThreadPool(5);
        executorService.shutdown();
        try {
            executorService.awaitTermination(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while shutting down", e);
        }
        this.main.shutdown();
    }

    private static class Connection {

        private final HttpClient httpClient;
        private final String basicAuthEncoded;

        Connection(final HttpClient httpClient, final String basicAuthEncoded) {
            this.httpClient = httpClient;
            this.basicAuthEncoded = basicAuthEncoded;
        }

        public HttpClient getHttpClient() {
            return this.httpClient;
        }

        public String getBasicAuthEncoded() {
            return this.basicAuthEncoded;
        }
    }

    private static class UserDetails {
        public String name;
        public String description;
        public String userid;

        UserDetails(final String name, final String description, final String userid) {
            this.name = name;
            this.description = description;
            this.userid = userid;
        }

        public static UserDetails of(final String name, final String description, final String userid) {
            return new UserDetails(name, description, userid);
        }
    }

}
