package com.evaitcs.securebank.account.config;

import com.evaitcs.securebank.account.domain.Account;
import com.evaitcs.securebank.account.domain.AccountType;
import com.evaitcs.securebank.account.domain.Customer;
import com.evaitcs.securebank.account.domain.Transaction;
import com.evaitcs.securebank.account.repository.AccountRepository;
import com.evaitcs.securebank.account.repository.CustomerRepository;
import com.evaitcs.securebank.account.repository.TransactionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Seeds the Accounts context demo world — mirrors stage-1 seed.sql so the
 * database-per-service split feels continuous with the earlier stages:
 *   customers: 1=Alice, 2=Bob
 *   accounts:  1=Alice CHECKING 1500.00, 2=Alice SAVINGS 5200.00,
 *              3=Bob CHECKING 300.00
 *   ledger:    reconciles to the balances (1200+500-200 / 5000+200 / 500-200)
 *
 * CROSS-SERVICE ID CONTRACT with auth-service's seeder (alice->cid 1,
 * bob->cid 2): demo convenience, and an honest example of the coupling
 * distributed systems inherit when data is seeded independently. Real estates
 * propagate identity through events or provisioning workflows.
 *
 * (@Transactional works here because Spring calls run() from OUTSIDE the
 * bean — the proxy/self-invocation lesson from stage-2's DataSeeder.)
 */
@Component
public class AccountDataSeeder implements ApplicationRunner {

    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public AccountDataSeeder(CustomerRepository customers,
                             AccountRepository accounts,
                             TransactionRepository transactions) {
        this.customers = customers;
        this.accounts = accounts;
        this.transactions = transactions;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (customers.count() > 0) {
            return;
        }

        Customer alice = customers.save(new Customer("Alice Nguyen", "alice@securebank.dev"));
        Customer bob = customers.save(new Customer("Bob Rivera", "bob@securebank.dev"));

        Account aliceChecking = accounts.save(new Account(alice, AccountType.CHECKING));
        Account aliceSavings = accounts.save(new Account(alice, AccountType.SAVINGS));
        Account bobChecking = accounts.save(new Account(bob, AccountType.CHECKING));

        // Balances via the guarded domain methods + matching ledger rows.
        aliceChecking.deposit(new BigDecimal("1200.00"));
        aliceChecking.deposit(new BigDecimal("500.00"));
        aliceChecking.withdraw(new BigDecimal("200.00"));
        aliceSavings.deposit(new BigDecimal("5000.00"));
        aliceSavings.deposit(new BigDecimal("200.00"));
        bobChecking.deposit(new BigDecimal("500.00"));
        bobChecking.withdraw(new BigDecimal("200.00"));

        transactions.save(Transaction.credit(aliceChecking, new BigDecimal("1200.00"), "Opening deposit", null));
        transactions.save(Transaction.credit(aliceChecking, new BigDecimal("500.00"), "Payroll", null));
        transactions.save(Transaction.debit(aliceChecking, new BigDecimal("200.00"), "Transfer out to savings", "seed-transfer-0001"));
        transactions.save(Transaction.credit(aliceSavings, new BigDecimal("5000.00"), "Opening deposit", null));
        transactions.save(Transaction.credit(aliceSavings, new BigDecimal("200.00"), "Transfer in from checking", "seed-transfer-0001"));
        transactions.save(Transaction.credit(bobChecking, new BigDecimal("500.00"), "Opening deposit", null));
        transactions.save(Transaction.debit(bobChecking, new BigDecimal("200.00"), "ATM withdrawal", null));
    }
}
