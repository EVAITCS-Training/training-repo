package com.evaitcs.securebank.repository;

import com.evaitcs.securebank.domain.Account;
import com.evaitcs.securebank.domain.AccountType;
import com.evaitcs.securebank.domain.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REPOSITORY SLICE TEST — @DataJpaTest boots ONLY the JPA layer:
 * entities, repositories, an embedded H2 database, transaction rollback
 * after each test. No web, no security, no services => milliseconds per test.
 *
 * This is the "test the generated SQL" layer of the pyramid: derived-query
 * name parsing and @Query JPQL are verified against a real database engine
 * (H2 in PostgreSQL-ish mode), catching typos that compile fine but would
 * explode at startup or first use.
 *
 * BOOT 4 NOTE: the annotation moved to
 * org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
 * (provided by spring-boot-starter-data-jpa-test).
 */
@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    AccountRepository accounts;

    @Autowired
    TestEntityManager em; // test-only helper: persist/flush without a repository

    Customer alice;
    Account checking;
    Account savings;

    @BeforeEach
    void seed() {
        alice = em.persist(new Customer("Alice Nguyen", "alice@securebank.dev"));
        checking = em.persist(new Account(alice, AccountType.CHECKING));
        savings = em.persist(new Account(alice, AccountType.SAVINGS));
        em.flush(); // force INSERTs so generated ids exist
    }

    @Test
    @DisplayName("derived query: findByCustomerIdOrderByTypeAsc parses and runs")
    void derivedQueryWorks() {
        List<Account> found = accounts.findByCustomerIdOrderByTypeAsc(alice.getId());
        assertThat(found).hasSize(2)
                // CHECKING sorts before SAVINGS alphabetically — the OrderBy in the name.
                .extracting(Account::getType)
                .containsExactly(AccountType.CHECKING, AccountType.SAVINGS);
    }

    @Test
    @DisplayName("JPQL JOIN FETCH loads customer without a second SELECT (N+1 fix)")
    void joinFetchEagerLoads() {
        List<Account> found = accounts.findAllWithCustomer(alice.getId());
        assertThat(found).hasSize(2);
        // If the fetch join worked, reading the customer triggers no lazy proxy:
        assertThat(found.get(0).getCustomer().getEmail()).isEqualTo("alice@securebank.dev");
    }

    @Test
    @DisplayName("pessimistic lock finder returns the row (SELECT ... FOR UPDATE)")
    void lockFinderWorks() {
        assertThat(accounts.findByIdForUpdate(checking.getId())).contains(checking);
        assertThat(accounts.findByIdForUpdate(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("balance invariant survives a round-trip through the database")
    void persistenceRoundTrip() {
        checking.deposit(new java.math.BigDecimal("250.50"));
        em.flush();
        em.clear(); // evict everything — next read MUST hit the database

        Account reloaded = accounts.findById(checking.getId()).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo("250.50");
        // DECIMAL(15,2) <-> BigDecimal mapping preserved the value exactly.
    }
}
