package io.lighty.kit.examples.community;

import io.lighty.kit.examples.community.aaa.restconf.Main;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CommunityAAARestconfAppTest {

    private static final String TEST_ADDRESS = "http://localhost:8888/restconf/data/ietf-yang-library:modules-state";
    private static final String BASIC_AUTH =
        "Basic " + Base64.getEncoder().encodeToString(("admin:admin").getBytes(StandardCharsets.UTF_8));
    private static final String AUTH = "Authorization";

    private HttpClient httpClient;

    @BeforeClass
    public void startUp() throws Exception {
        Main.start();

        this.httpClient = new HttpClient();
        this.httpClient.setIdleTimeout(60000 * 60);
        this.httpClient.start();
    }

    @Test
    public void readDataCorrectCredentials() throws Exception {
        Assert.assertEquals(sendGetRequestJSON(this.httpClient, TEST_ADDRESS).getStatus(), HttpStatus.OK_200);
    }

    private ContentResponse sendGetRequestJSON(final HttpClient client, final String path)
        throws InterruptedException, ExecutionException, TimeoutException {
        return client.newRequest(path)
            .method(HttpMethod.GET)
            .header(AUTH, BASIC_AUTH)
            .send();
    }

    @AfterClass
    public void tearDown() throws Exception {
        this.httpClient.stop();
        this.httpClient.destroy();
        this.httpClient = null;
    }
}
