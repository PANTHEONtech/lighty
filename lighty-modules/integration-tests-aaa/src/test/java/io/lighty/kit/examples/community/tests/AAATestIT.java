/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.kit.examples.community.tests;

import io.lighty.kit.examples.community.aaa.restconf.Main;
import java.io.File;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
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
    private static final String PATH_PREFIX = "http://localhost:8888/";
    private static final String AUTH = "Authorization";
    private static final String USERS_PATH = "auth/v1/users";
    private static final String USERS = "users";
    private static final String BASIC = "Basic ";
    private static final String NAME = "name";
    private static final String USER_ID = "userid";
    private static final String ADMIN = "admin";
    private static final String NEW_USER = "newUser";

    private String authorization;
    private String wrongAuthorization;
    private String newAuthorization;
    private String updatedAuthorization;
    private HttpClient httpClient;
    private HttpClient httpClientWrongCredentials;
    private HttpClient httpClientUser;


    @BeforeClass
    public void initClass() throws Exception {
        LOG.info("init restconf and controller");
        Main.start();

        LOG.info("controller and restconf started successfully");
        authorization = BASIC + Base64.getEncoder().encodeToString(("admin:admin").getBytes("UTF-8"));
        wrongAuthorization = BASIC + Base64.getEncoder().encodeToString(("foo:bar").getBytes("UTF-8"));
        updatedAuthorization = BASIC + Base64.getEncoder().encodeToString(("newUser:evenMoreSecretPassword")
                .getBytes("UTF-8"));
        newAuthorization = BASIC + Base64.getEncoder().encodeToString(("newUser:verySecretPassword")
                .getBytes("UTF-8"));
    }

    @BeforeMethod
    public void init() {
        LOG.info("start all http clients");
        httpClient = new HttpClient();
        httpClient.setIdleTimeout(60000 * 60);
        httpClientWrongCredentials = new HttpClient();
        httpClientWrongCredentials.setIdleTimeout(60000 * 60);
        httpClientUser = new HttpClient();
        httpClientUser.setIdleTimeout(60000 * 60);

        try {
            httpClient.start();
            httpClientWrongCredentials.start();
            httpClientUser.start();
        } catch (final Exception e) {
            LOG.error(e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Test(enabled=false)
    public void getUsersTest() throws Exception {
        LOG.info("Get all user tests");
        // 1. Get users
        checkInitialData();
        // 3. Get users with wrong credentials
        getAllAndCheck(httpClientWrongCredentials, this.wrongAuthorization, HttpStatus.UNAUTHORIZED_401);
        getAllAndCheck(httpClientUser, this.newAuthorization, HttpStatus.UNAUTHORIZED_401);
    }

    @Test(enabled=false)
    public void addUserTest() throws Exception {
        LOG.info("Add new user test");
        // 1. Add new user
        String payload = TestUtils.readResource("/testdata/new-user.json");

        Request request = httpClient.newRequest(PATH_PREFIX + USERS_PATH);
        ContentResponse response = request
                .method(HttpMethod.POST)
                .header(AUTH, this.authorization)
                .content(new StringContentProvider(payload), MediaType.APPLICATION_JSON)
                .send();
        Assert.assertEquals(response.getStatus(), HttpStatus.CREATED_201);
        final JSONObject user = new JSONObject(response.getContentAsString());
        Assert.assertEquals(user.getString(USER_ID), "newUser@sdn");

        // 2. Read all users and check if the user is added
        response = getAllAndCheck(httpClient, this.authorization, HttpStatus.OK_200);
        final JSONObject responseObject = new JSONObject(response.getContentAsString());
        final JSONArray users = responseObject.getJSONArray(USERS);
        Assert.assertEquals(users.length(), 2);

        // 3. Try to read data with new user expect HttpStatus.UNAUTHORIZED_401
        getAllAndCheck(httpClientUser, this.newAuthorization, HttpStatus.UNAUTHORIZED_401);

        // 4. Grant rights to new user and read data
        request = httpClient.newRequest(PATH_PREFIX + "auth/v1/domains/sdn/users/newUser@sdn/roles");
        payload = TestUtils.readResource("/testdata/grant-data.json");
        response = request
                .method(HttpMethod.POST)
                .content(new StringContentProvider(payload), MediaType.APPLICATION_JSON)
                .send();
        Assert.assertEquals(response.getStatus(), HttpStatus.CREATED_201);
        Assert.assertEquals(new JSONObject(response.getContentAsString()).get("grantid"), "newUser@sdn@admin@sdn@sdn");

        getAllAndCheck(httpClient, this.newAuthorization, HttpStatus.OK_200);
        // 5. Try to read data with wrong credentials
        getAllAndCheck(httpClientWrongCredentials, this.wrongAuthorization, HttpStatus.UNAUTHORIZED_401);
    }

    @Test(enabled=false)
    public void getSpecificUsersTest() throws Exception {
        LOG.info("get specific user test");
        // 1. Get new specific user and check if its info is as expected
        getAndCheckSpecificUser(NEW_USER, this.httpClient, this.authorization, false);
        getAndCheckSpecificUser(ADMIN, this.httpClientUser, this.newAuthorization, false);
    }

    @Test(enabled=false)
    public void updateUserInfoTest() throws Exception {
        LOG.info("Update user data and try to use them");
        // 1. Update user info
        String payload = TestUtils.readResource("/testdata/update-user.json");
        Request request = httpClient.newRequest(PATH_PREFIX + USERS_PATH + "/newUser@sdn");
        ContentResponse response = request
                .method(HttpMethod.PUT)
                .header(AUTH, this.authorization)
                .content(new StringContentProvider(payload), MediaType.APPLICATION_JSON)
                .send();
        Assert.assertEquals(response.getStatus(), HttpStatus.OK_200);
        // 2. Read updated user and check if its info is as expected
        getAndCheckSpecificUser(NEW_USER, this.httpClient, this.authorization, false);
        // 3. Use new credentials
        getAllAndCheck(httpClientUser, this.updatedAuthorization, HttpStatus.OK_200);
    }

    @Test(enabled=false)
    public void deleteUserTest() throws Exception {
        LOG.info("delete user");
        // 1. Try to delete user (self)
        deleteUser(true, this.updatedAuthorization);
    }

    @Test(enabled=false)
    public void readNotExistingUserExpectError() throws Exception {
        LOG.info("get specific not existing user");
        // 1. Read specific user that does not exist and expect error
        getAndCheckSpecificUser(NEW_USER, this.httpClient, this.authorization, true);
    }

    @Test(enabled=false)
    public void deleteNotExistingUserExpectError() throws Exception {
        LOG.info("delete specific not existing user");
        // 1. Try to delete user that does not exist and expect error
        deleteUser(false, this.authorization);
    }

    @Test(enabled=false)
    public void makeRequestCorrectCredentials() throws Exception {
        LOG.info("try to get modules state with correct credentials");
        // 1. Try to make request with wrong credentials expect HttpStatus.UNAUTHORIZED_401 unauthorized
        getModulesState(this.authorization, HttpStatus.OK_200);
    }

    @Test(enabled=false)
    public void makeRequestWrongCredentials() throws Exception {
        LOG.info("try to get modules state with incorrect credentials");
        // 1. Try to make request with correct credentials expect response
        getModulesState(this.wrongAuthorization, HttpStatus.UNAUTHORIZED_401);
    }

    private ContentResponse getAllAndCheck(final HttpClient client, final String authorization, final int status)
            throws Exception {
        final Request request = client.newRequest(PATH_PREFIX + USERS_PATH);
        final ContentResponse response = request
                .method(HttpMethod.GET)
                .header(AUTH, authorization)
                .send();
        Assert.assertEquals(response.getStatus(), status);
        return response;
    }

    private void getModulesState(final String authorization, final int status) throws Exception {
        final Request request = httpClient.newRequest(PATH_PREFIX + "restconf/data/ietf-yang-library:modules-state");
        final ContentResponse response = request
                .method(HttpMethod.GET)
                .header(AUTH, authorization)
                .send();
        Assert.assertEquals(response.getStatus(), status);
    }

    private void deleteUser(final boolean existingUser, final String authorization) throws Exception {
        Request request = httpClient.newRequest(PATH_PREFIX + USERS_PATH + "/newUser@sdn");
        ContentResponse response = request
                .method(HttpMethod.DELETE)
                .header(AUTH, authorization)
                .send();
        if (existingUser) {
            Assert.assertEquals(response.getStatus(), HttpStatus.NO_CONTENT_204);
            // 2. Check if the user is really missing
            checkInitialData();
        } else {
            Assert.assertEquals(response.getStatus(), HttpStatus.NOT_FOUND_404);
        }
    }

    private void checkInitialData() throws Exception {
        final Request request = httpClient.newRequest(PATH_PREFIX + USERS_PATH);
        final ContentResponse response = request
                .method(HttpMethod.GET)
                .header(AUTH, this.authorization)
                .send();
        // 2. Check if their info is as expected
        Assert.assertEquals(response.getStatus(), HttpStatus.OK_200);
        final JSONObject responseObject = new JSONObject(response.getContentAsString());
        final JSONArray users = responseObject.getJSONArray(USERS);
        Assert.assertEquals(users.length(), 1);
        final JSONObject user = (JSONObject) responseObject.getJSONArray(USERS).get(0);
        Assert.assertEquals(user.getString(NAME), ADMIN);
        Assert.assertEquals(user.getString("description"), "admin user");
    }

    private void getAndCheckSpecificUser(final String user, final HttpClient httpClient, final String authorization,
                                         final boolean statusNotFound) throws Exception {
        final String id = user + "@sdn";
        Request request = httpClient.newRequest(PATH_PREFIX + USERS_PATH + "/" + id);
        ContentResponse response = request
                .method(HttpMethod.GET)
                .header(AUTH, authorization)
                .send();
        if (statusNotFound) {
            Assert.assertEquals(response.getStatus(), HttpStatus.NOT_FOUND_404);
        } else {
            Assert.assertEquals(response.getStatus(), HttpStatus.OK_200);
            JSONObject userInfo = new JSONObject(response.getContentAsString());
            Assert.assertEquals(userInfo.getString(USER_ID), id);
            Assert.assertEquals(userInfo.getString(NAME), user);
        }
    }

    @AfterMethod
    public void cleanUp() throws Exception {
        LOG.info("stopping all the http clients");
        httpClient.stop();
        httpClientUser.stop();
        httpClientWrongCredentials.stop();
    }

    @AfterClass
    public void shutdown() throws Exception {
        LOG.info("removing db files");
        File currentDirFile = new File(".");
        String lightyTestsPath = currentDirFile.getAbsolutePath();
        FileUtils.deleteDirectory(new File(lightyTestsPath + "/configuration"));
        FileUtils.deleteDirectory(new File(lightyTestsPath + "/data"));

        LOG.info("shutdown.");
        final ExecutorService executorService = Executors.newFixedThreadPool(5);
        executorService.shutdown();
        executorService.awaitTermination(15, TimeUnit.SECONDS);
    }

}
