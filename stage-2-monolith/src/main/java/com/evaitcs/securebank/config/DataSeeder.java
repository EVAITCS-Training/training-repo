package com.evaitcs.securebank.config;

import com.evaitcs.securebank.domain.Account;
import com.evaitcs.securebank.domain.AccountType;
import com.evaitcs.securebank.domain.AppUser;
import com.evaitcs.securebank.domain.Customer;
import com.evaitcs.securebank.domain.Loan;
import com.evaitcs.securebank.domain.Role;
import com.evaitcs.securebank.domain.Transaction;
import com.evaitcs.securebank.repository.AccountRepository;
import com.evaitcs.securebank.repository.AppUserRepository;
import com.evaitcs.securebank.repository.CustomerRepository;
import com.evaitcs.securebank.repository.LoanRepository;
import com.evaitcs.securebank.repository.TransactionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * DEVELOPMENT SEEDER — an ApplicationRunner executes right AFTER the context
 * is up, so the app boots into a usable demo state:
 *
 *   customers: Alice (cid 1), Bob (cid 2)
 *   logins:    alice/password123 (CUSTOMER), bob/password123 (CUSTOMER),
 *              teller/password123 (TELLER), admin/password123 (ADMIN)
 *   accounts:  Alice CHECKING 1,500.00 + SAVINGS 5,200.00; Bob CHECKING 300.00
 *   ledger:    opening transactions so statements aren't empty
 *   loans:     one APPLIED application for Alice (teller approves it in class)
 *
 * WHY A SEPARATE @Component (instead of a @Bean lambda calling a method here)?
 * @Transactional works through PROXIES: Spring only intercepts calls that
 * arrive from OUTSIDE the bean. A @Bean lambda invoking its own class's
 * method = self-invocation = NO transaction. The bug that produces is
 * spectacular and educational: every accounts.save() commits immediately,
 * the returned entity becomes DETACHED, and the deposit() calls afterwards
 * mutate a Java object nobody will ever write back — the database keeps
 * balance 0.00 while the code "worked". As a runner COMPONENT, Spring calls
 * run() from outside, the proxy applies, all entities stay MANAGED inside
 * one transaction, and dirty-checking flushes every balance on commit.
 * (Week 6 AOP + Week 6 JPA lifecycle, meeting in one bug.)
 *
 * WHY JAVA AND NOT data.sql? Passwords must be BCrypt-hashed with the SAME
 * encoder the login flow uses. Production note: seeding belongs in migrations
 * (Flyway), not app code — this runner is a dev convenience, guarded by the
 * emptiness check so it never double-inserts.
 *
 * IDs are deterministic (IDENTITY starts at 1, insertion order fixed) — the
 * integration test relies on it: alice=1, CHECKING=1, SAVINGS=2, bob=2,
 * bob's CHECKING=3, alice's loan=1.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final LoanRepository loans;
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(CustomerRepository customers,
                      AccountRepository accounts,
                      TransactionRepository transactions,
                      LoanRepository loans,
                      AppUserRepository users,
                      PasswordEncoder passwordEncoder) {
        this.customers = customers;
        this.accounts = accounts;
        this.transactions = transactions;
        this.loans = loans;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional // one atomic seed — see the proxy explanation above
    public void run(ApplicationArguments args) {
        if (customers.count() > 0) {
            return; // already seeded (e.g. restart against a persistent DB)
        }

        // --- Customers ------------------------------------------------------
        Customer alice = customers.save(new Customer("Alice Nguyen", "alice@securebank.dev"));
        Customer bob = customers.save(new Customer("Bob Rivera", "bob@securebank.dev"));

        // --- Login identities (hash via the SAME encoder login verifies with)
        users.save(new AppUser("alice", passwordEncoder.encode("password123"), Role.CUSTOMER, alice));
        users.save(new AppUser("bob", passwordEncoder.encode("password123"), Role.CUSTOMER, bob));
        users.save(new AppUser("teller", passwordEncoder.encode("password123"), Role.TELLER, null));
        users.save(new AppUser("admin", passwordEncoder.encode("password123"), Role.ADMIN, null));

        // --- Accounts (ids 1,2,3 via the identity sequence) -----------------
        Account aliceChecking = accounts.save(new Account(alice, AccountType.CHECKING));
        Account aliceSavings = accounts.save(new Account(alice, AccountType.SAVINGS));
        Account bobChecking = accounts.save(new Account(bob, AccountType.CHECKING));

        // Balances + matching OPENING ledger rows. Because run() is
        // transactional, these entities are MANAGED: deposit() marks them
        // dirty and Hibernate flushes the UPDATEs at commit — no save()
        // needed (dirty-checking, Week 6 JPA lifecycle).
        aliceChecking.deposit(new BigDecimal("1500.00"));
        aliceSavings.deposit(new BigDecimal("5200.00"));
        bobChecking.deposit(new BigDecimal("300.00"));
        transactions.save(Transaction.credit(aliceChecking, new BigDecimal("1500.00"), "Opening deposit", null));
        transactions.save(Transaction.credit(aliceSavings, new BigDecimal("5200.00"), "Opening deposit", null));
        transactions.save(Transaction.credit(bobChecking, new BigDecimal("300.00"), "Opening deposit", null));

        // --- One loan awaiting teller approval ------------------------------
        loans.save(new Loan(alice, new BigDecimal("10000.00"), new BigDecimal("7.50"), 36));
    }
}
