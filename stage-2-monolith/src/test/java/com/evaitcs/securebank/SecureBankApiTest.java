package com.evaitcs.securebank;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * END-TO-END API TEST — the whole stack, minus the network.
 *
 * WHAT'S REAL HERE: Spring Security filter chain (JWT issuing AND verifying),
 * controllers + validation, services, JPA/Hibernate, the H2 database, the
 * DataSeeder's demo data, and the exception handler. Only the socket is
 * mocked (MockMvc calls controllers in-process), so this catches ~everything
 * a real HTTP client would hit.
 *
 * BOOT 4 IMPORT NOTE (see docs/refs/BOOT4_CHEATSHEET.md): @AutoConfigureMockMvc
 * moved to org.springframework.boot.webmvc.test.autoconfigure and must be
 * declared explicitly — @SpringBootTest no longer implies it.
 *
 * THE STORY (ordered on purpose — later tests use earlier results):
 *   1. no token          -> 401 (security works)
 *   2. login as alice    -> 200 + JWT
 *   3. list accounts     -> 200, alice sees HER two accounts
 *   4. transfer 100      -> 201 + reference; balances changed
 *   5. overdraw transfer -> 400 problem+json (business rule survives HTTP)
 *   6. alice reads bob's account -> 404 (ownership, no existence leak)
 *   7. alice approves a loan -> 403 (role check); teller -> 200
 *
 * Seeded ids (DataSeeder): alice=cid 1 (accounts 1,2), bob=cid 2 (account 3),
 * alice's pending loan = 1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecureBankApiTest {

    @Autowired
    MockMvc mvc;

    /** Tokens are minted once and reused — like a real frontend session. */
    static String aliceToken;
    static String tellerToken;

    private static String login(MockMvc mvc, String username) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "password123"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        // JsonPath (ships with spring-boot-starter-test) pulls the token out
        // of the raw JSON — the same trick the frontend uses with response.json().
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    @Test
    @Order(1)
    @DisplayName("unauthenticated request is rejected with 401")
    void noTokenNoData() throws Exception {
        mvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    @DisplayName("login returns a usable Bearer token with identity claims")
    void loginWorks() throws Exception {
        aliceToken = login(mvc, "alice");
        tellerToken = login(mvc, "teller");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "alice", "password": "password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.customerId").value(1));
    }

    @Test
    @Order(3)
    @DisplayName("alice sees exactly her own accounts")
    void listOwnAccounts() throws Exception {
        mvc.perform(get("/api/accounts").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].customerId").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(1))));
    }

    @Test
    @Order(4)
    @DisplayName("transfer moves money atomically and returns a correlation reference")
    void transferHappyPath() throws Exception {
        // balances before: checking(1)=1500.00, savings(2)=5200.00
        mvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromAccountId": 1, "toAccountId": 2, "amount": 100.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").isNotEmpty())
                .andExpect(jsonPath("$.fromAccountId").value(1))
                .andExpect(jsonPath("$.toAccountId").value(2));

        mvc.perform(get("/api/accounts/1").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1400.00));

        mvc.perform(get("/api/accounts/2").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(5300.00));
    }

    @Test
    @Order(5)
    @DisplayName("insufficient funds -> 400 problem+json, balances untouched")
    void overdraftRejected() throws Exception {
        mvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromAccountId": 1, "toAccountId": 2, "amount": 999999.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Insufficient funds"))
                .andExpect(jsonPath("$.status").value(400));

        mvc.perform(get("/api/accounts/1").header("Authorization", "Bearer " + aliceToken))
                .andExpect(jsonPath("$.balance").value(1400.00)); // unchanged — rollback worked
    }

    @Test
    @Order(6)
    @DisplayName("bob's account is invisible to alice (404, not 403)")
    void ownershipEnforced() throws Exception {
        mvc.perform(get("/api/accounts/3").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNotFound()); // existence deliberately not revealed
    }

    @Test
    @Order(7)
    @DisplayName("loan approval is teller-only: alice 403, teller 200 with payment schedule")
    void loanApprovalRbac() throws Exception {
        mvc.perform(post("/api/loans/1/approve").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden()); // @PreAuthorize(hasAnyRole TELLER/ADMIN)

        mvc.perform(post("/api/loans/1/approve").header("Authorization", "Bearer " + tellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.monthlyPayment").isNumber()); // annuity formula ran

        // approving twice is a state conflict
        mvc.perform(post("/api/loans/1/approve").header("Authorization", "Bearer " + tellerToken))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(8)
    @DisplayName("statement pages the ledger, newest first")
    void statementPaging() throws Exception {
        mvc.perform(get("/api/accounts/1/transactions?page=0&size=5")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }
}
