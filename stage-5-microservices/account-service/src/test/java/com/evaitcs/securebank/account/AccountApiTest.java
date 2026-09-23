package com.evaitcs.securebank.account;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Account-service integration test — public surface (JWT + ownership) AND
 * internal surface (the saga primitives payment-service will call).
 *
 * Boots standalone: H2, Eureka off, config client off (test application.yml).
 * Tokens are forged by TestTokens with the same secret — proving the
 * cross-service token contract without running auth-service.
 *
 * ORDERED on purpose: the debit/credit tests mutate the seeded balances and
 * later assertions depend on earlier effects (1500 -> debit 100 -> 1400 ->
 * credit 50 -> 1450). JUnit's default order is deterministic-but-unspecified;
 * @Order makes the dependency explicit instead of accidental.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountApiTest {

    @Autowired
    MockMvc mvc;

    private static String aliceAuth() {
        return "Bearer " + TestTokens.customer("alice", 1L);
    }

    @Test
    @Order(1)
    @DisplayName("public: no token -> 401")
    void unauthenticatedRejected() throws Exception {
        mvc.perform(get("/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    @DisplayName("public: alice lists exactly her two accounts")
    void listOwnAccounts() throws Exception {
        mvc.perform(get("/accounts").header("Authorization", aliceAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].customerId").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(1))));
    }

    @Test
    @Order(3)
    @DisplayName("public: bob's account is invisible to alice (404, not 403)")
    void ownershipEnforced() throws Exception {
        mvc.perform(get("/accounts/3").header("Authorization", aliceAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(4)
    @DisplayName("internal: debit moves the balance and writes a ledger row")
    void internalDebitWorks() throws Exception {
        // The internal API needs NO token — it is unreachable from outside
        // (no gateway route); the test talks to it like payment-service does.
        mvc.perform(post("/internal/accounts/1/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 100.00, "reference": "xfer-test-1", "description": "saga debit"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.newBalance").value(1400.00)); // 1500 - 100

        // ...and the ledger row is visible in alice's statement, with the
        // saga reference — the audit trail crossing the service boundary.
        mvc.perform(get("/accounts/1/transactions?page=0&size=5")
                        .header("Authorization", aliceAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reference").value("xfer-test-1"))
                .andExpect(jsonPath("$.content[0].type").value("DEBIT"));
    }

    @Test
    @Order(5)
    @DisplayName("internal: overdraw debit -> 400 problem+json (the saga's 'business refusal')")
    void internalDebitInsufficientFunds() throws Exception {
        mvc.perform(post("/internal/accounts/1/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 9999999.00, "reference": "xfer-test-2", "description": "doomed"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Insufficient funds"));
    }

    @Test
    @Order(6)
    @DisplayName("internal: credit can compensate (reference suffix -comp by convention)")
    void internalCreditCompensation() throws Exception {
        mvc.perform(post("/internal/accounts/1/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 50.00, "reference": "xfer-test-3-comp", "description": "compensation"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newBalance").value(1450.00)); // 1400 from previous test + 50
    }
}
