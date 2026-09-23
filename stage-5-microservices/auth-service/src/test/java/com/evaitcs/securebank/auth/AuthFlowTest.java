package com.evaitcs.securebank.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auth-service integration test: the identity heartbeat of the whole system.
 * If login breaks here, NOTHING works downstream — every other service trusts
 * these tokens. (Boots fully standalone: H2, Eureka off, config import
 * optional — the test profile needs no infrastructure. That self-sufficiency
 * per service is what makes a microservices repo testable in CI at all.)
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    MockMvc mvc;

    @Test
    void loginMintsTokenWithIdentityClaims() throws Exception {
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "alice", "password": "password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.customerId").value(1))
                .andReturn();

        // A JWT is three base64url parts joined by dots — assert the shape and
        // peek into the payload (middle part) to prove the claims traveled.
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        String[] parts = token.split("\\.");
        org.assertj.core.api.Assertions.assertThat(parts).hasSize(3);

        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        org.assertj.core.api.Assertions.assertThat(payload)
                .contains("\"sub\":\"alice\"")
                .contains("\"role\":\"CUSTOMER\"")
                .contains("\"cid\":1");
    }

    @Test
    void wrongPasswordIsRejectedWithGeneric401() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "alice", "password": "wrongpassword"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownUserIsRejectedIdenticallyToWrongPassword() throws Exception {
        // Same status, same (empty) body — attackers cannot distinguish
        // "no such user" from "bad password" (enumeration defense).
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "mallory", "password": "whatever12"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffLoginHasNoCustomerClaim() throws Exception {
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "teller", "password": "password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TELLER"))
                .andExpect(jsonPath("$.customerId").doesNotExist())
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        org.assertj.core.api.Assertions.assertThat(payload).doesNotContain("\"cid\"");
    }
}
