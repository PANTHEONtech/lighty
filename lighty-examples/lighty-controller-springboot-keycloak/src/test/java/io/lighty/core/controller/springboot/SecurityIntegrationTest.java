package io.lighty.core.controller.springboot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class SecurityIntegrationTest {

    private static final String ALICE_USER = "alice";
    private static final String BOB_ADMIN = "bob";
    private static final String BASE_URL = "http://localhost:8888";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @Before
    public void setup() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @WithAnonymousUser
    @Test
    public void givenUnauthenticatedUser_accessShouldFailWith401() throws Exception {
        getAndCheckResult("/services/data/topology/list", HttpStatus.UNAUTHORIZED);
    }

    @WithAnonymousUser
    @Test
    public void givenUnauthenticatedUser_accessToErrorPageShouldSucceedWith200() throws Exception {
        getAndCheckResult("/error", HttpStatus.OK);
    }

    @WithMockUser(username = "jane", roles = {})
    @Test
    public void givenUserWithoutRoles_accessShouldFailWith403() throws Exception {
        getAndCheckResult("/services/data/topology/list", HttpStatus.FORBIDDEN);
    }

    @WithMockUser(username = ALICE_USER, roles = {"USER"})
    @Test
    public void givenUserAlice_GETshouldSucceedWith200() throws Exception {
        getAndCheckResult("/services/data/topology/list", HttpStatus.OK)
                .andExpect(MockMvcResultMatchers.jsonPath("$").exists())    // must be a valid JSON
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())   // must be an array
                .andExpect(MockMvcResultMatchers.jsonPath("$[?(@ == 'topology-netconf')]").exists());   // 'topology-netconf' must exist
    }

    @WithMockUser(username = ALICE_USER, roles = {"USER"})
    @Test
    public void givenUserAlice_PUTshouldFailWith403() throws Exception {
        putAndCheckResult("/services/data/topology/id/test-topology-id", HttpStatus.FORBIDDEN);
    }

    @WithMockUser(username = BOB_ADMIN, roles = {"USER", "ADMIN"})
    @Test
    public void givenAdminBob_GETshouldSucceedWith200() throws Exception {
        getAndCheckResult("/services/data/topology/list", HttpStatus.OK)
                .andExpect(MockMvcResultMatchers.jsonPath("$").exists())    // must be a valid JSON
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())   // must be an array
                .andExpect(MockMvcResultMatchers.jsonPath("$[?(@ == 'topology-netconf')]").exists());   // 'topology-netconf' must exist
    }

    @WithMockUser(username = BOB_ADMIN, roles = {"USER", "ADMIN"})
    @Test
    public void givenAdminBob_PUTshouldSucceedWith200() throws Exception {
        String topologyId = "test-topology-id";

        putAndCheckResult("/services/data/topology/id/" + topologyId, HttpStatus.OK);
        getAndCheckResult("/services/data/topology/list", HttpStatus.OK)
                .andExpect(MockMvcResultMatchers.jsonPath("$").exists())    // must be a valid JSON
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())   // must be an array
                .andExpect(MockMvcResultMatchers.jsonPath("$[?(@ == '" + topologyId + "')]").exists());   // inserted topology id should exist
    }

    @WithMockUser(username = BOB_ADMIN, roles = {"USER", "ADMIN"})
    @Test
    public void givenAdminBob_DELETEshouldSucceedWith200() throws Exception {
        String topologyId = "test-delete-topology-id";

        /** Create test topology **/
        putAndCheckResult("/services/data/topology/id/" + topologyId, HttpStatus.OK);
        /** Check that the topology was created **/
        getAndCheckResult("/services/data/topology/list", HttpStatus.OK)
                .andExpect(MockMvcResultMatchers.jsonPath("$").exists())    // must be a valid JSON
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())   // must be an array
                .andExpect(MockMvcResultMatchers.jsonPath("$[?(@ == '" + topologyId + "')]").exists());

        /** Delete the dopology **/
        deleteAndCheckResult("/services/data/topology/id/" + topologyId, HttpStatus.OK);
        /** Check that the topology is no longer present **/
        getAndCheckResult("/services/data/topology/list", HttpStatus.OK)
                .andExpect(MockMvcResultMatchers.jsonPath("$").exists())    // must be a valid JSON
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())   // must be an array
                .andExpect(MockMvcResultMatchers.jsonPath("$[?(@ == '" + topologyId + "')]").doesNotExist());
    }

    private ResultActions getAndCheckResult(String uri, HttpStatus status) throws Exception {
        return mvc.perform(MockMvcRequestBuilders
                .get(BASE_URL + uri)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(status.value()));
    }

    private ResultActions putAndCheckResult(String uri, HttpStatus status) throws Exception {
        return mvc.perform(MockMvcRequestBuilders
                .put(BASE_URL + uri)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(status.value()));
    }

    private ResultActions deleteAndCheckResult(String uri, HttpStatus status) throws Exception {
        return mvc.perform(MockMvcRequestBuilders
                .delete(BASE_URL + uri)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(status.value()));
    }
}
