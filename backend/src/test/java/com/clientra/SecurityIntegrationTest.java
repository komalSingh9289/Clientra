package com.clientra;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end test of authentication + RBAC against an in-memory database seeded with the demo data
 * (system testing, synopsis 10.3). Covers: public login, 401 without a token, 403 for the wrong role,
 * and the data-scope rule (a rep sees only their own leads).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:clientra-test;DB_CLOSE_DELAY=-1",
        "clientra.seed-demo-data=true"})
class SecurityIntegrationTest {
    @Autowired MockMvc mvc;

    private String login(String email, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty()).andReturn();
        String body = r.getResponse().getContentAsString();
        return body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test void protectedEndpointNeedsToken() throws Exception {
        mvc.perform(get("/api/leads")).andExpect(status().isUnauthorized());
    }

    @Test void wrongPasswordIs401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@clientra.demo\",\"password\":\"nope\"}")).andExpect(status().isUnauthorized());
    }

    @Test void repCannotOpenAdminEndpoints() throws Exception {
        String t = login("rep@clientra.demo", "rep123");
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit-logs").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
        mvc.perform(get("/api/reports").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
    }

    @Test void adminCanOpenAdminEndpoints() throws Exception {
        String t = login("admin@clientra.demo", "admin123");
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        mvc.perform(get("/api/audit-logs").header("Authorization", "Bearer " + t)).andExpect(status().isOk());
    }

    @Test void repSeesOnlyOwnLeadsManagerSeesAll() throws Exception {
        String rep = login("rep@clientra.demo", "rep123");
        String mgr = login("manager@clientra.demo", "manager123");
        mvc.perform(get("/api/leads").header("Authorization", "Bearer " + rep))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].ownerName", everyItem(is("Rohan Verma"))));
        mvc.perform(get("/api/leads").header("Authorization", "Bearer " + mgr))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()", greaterThan(10)));
    }

    @Test void dashboardLoadsForEveryRole() throws Exception {
        for (String[] c : new String[][]{{"admin@clientra.demo", "admin123"}, {"rep@clientra.demo", "rep123"}}) {
            mvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + login(c[0], c[1])))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.kpis.activeLeads").isNumber());
        }
    }
}
