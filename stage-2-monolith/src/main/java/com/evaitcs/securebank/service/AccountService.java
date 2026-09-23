package com.evaitcs.securebank.service;

import com.evaitcs.securebank.domain.Account;
import com.evaitcs.securebank.domain.AccountType;
import com.evaitcs.securebank.domain.Customer;
import com.evaitcs.securebank.domain.ResourceNotFoundException;
import com.evaitcs.securebank.domain.Transaction;
import com.evaitcs.securebank.repository.AccountRepository;
import com.evaitcs.securebank.repository.CustomerRepository;
import com.evaitcs.securebank.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * SERVICE LAYER — "service" package.
 *
 * Services sit BETWEEN controllers (HTTP) and repositories (SQL). Their jobs:
 *   1. Enforce BUSINESS RULES (ownership, invariants, workflow order).
 *   2. Own TRANSACTION BOUNDARIES (@Transactional) — one service method =
 *      one atomic unit of work: all saves flush together at commit, or
 *      everything rolls back on a RuntimeException.
 *   3. Stay framework-light and UNIT TESTABLE — no HTTP types (that's the
 *      controller's job), no SQL strings (that's the repository's job).
 *
 * CONNECTION MAP for this class:
 *   AccountController ──calls──> AccountService ──calls──> AccountRepository
 *                                       │
 *                                       └──guards──> Account entity invariants
 *                                                    (deposit/withdraw)
 */
@Service
public class AccountService {

    // Constructor injection (NOT @Autowired on fields):
    //  - dependencies are visible and final (immutable),
    //  - the class is instantiable in plain unit tests (new AccountService(mock1, mock2, mock3)),
    //  - a circular dependency fails at startup instead of silently.
    private final AccountRepository accounts;
    private final CustomerRepository customers;
    private final TransactionRepository transactions;

    public AccountService(AccountRepository accounts,
                          CustomerRepository customers,
                          TransactionRepository transactions) {
        this.accounts = accounts;
        this.customers = customers;
        this.transactions = transactions;
    }

    /**
     * Open a new account for a customer.
     *
     * @Transactional here means: the account INSERT and anything else we add
     * later (welcome transaction, audit row...) commit or roll back together.
     */
    @Transactional
    public Account openAccount(Long customerId, AccountType type) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        Account account = new Account(customer, type);
        return accounts.save(account);
    }

    /** List the accounts belonging to ONE customer (the dashboard query). */
    @Transactional(readOnly = true) // readOnly = hint to skip flush/dirty-checking; slightly faster
    public List<Account> listForCustomer(Long customerId) {
        return accounts.findByCustomerIdOrderByTypeAsc(customerId);
    }

    /**
     * Fetch an account AND verify the caller owns it.
     *
     * SECURITY PATTERN — "don't leak existence": if the account belongs to
     * someone else we throw ResourceNotFound (-> HTTP 404), NOT Forbidden
     * (403). A 403 would tell an attacker "this id EXISTS, it's just not
     * yours", letting them enumerate valid account numbers. Week 9 OWASP
     * lesson: Broken Access Control.
     */
    @Transactional(readOnly = true)
    public Account getOwnedAccount(Long accountId, Long customerId) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        if (!account.getCustomer().getId().equals(customerId)) {
            throw new ResourceNotFoundException("Account", accountId);
        }
        return account;
    }

    /**
     * Cash/cheque deposit into an OWNED account.
     *
     * The full pattern for every money mutation, in order:
     *   1. authorization (ownership)      — getOwnedAccount
     *   2. state change via domain method — account.deposit (guards invariants)
     *   3. ledger entry                   — Transaction.credit (audit history)
     *   4. one transaction wraps it all   — @Transactional
     * Steps 2+3 MUST live in the same transaction: a balance change without
     * a ledger row (or vice versa) is how books stop balancing.
     */
    @Transactional
    public Account deposit(Long accountId, Long customerId, BigDecimal amount, String description) {
        Account account = getOwnedAccount(accountId, customerId);
        account.deposit(amount);
        transactions.save(Transaction.credit(account, amount,
                description == null ? "Deposit" : description,
                UUID.randomUUID().toString()));
        return account; // dirty-checking flushes the new balance on commit —
        // with JPA you often don't even call save() on a managed entity.
    }

    /** Withdrawal — mirror image of deposit, with the balance check inside the entity. */
    @Transactional
    public Account withdraw(Long accountId, Long customerId, BigDecimal amount, String description) {
        Account account = getOwnedAccount(accountId, customerId);
        account.withdraw(amount); // throws InsufficientFundsException -> 400 + rollback
        transactions.save(Transaction.debit(account, amount,
                description == null ? "Withdrawal" : description,
                UUID.randomUUID().toString()));
        return account;
    }
}
