package com.evaitcs.securebank.account.service;

import com.evaitcs.securebank.account.domain.Account;
import com.evaitcs.securebank.account.domain.AccountType;
import com.evaitcs.securebank.account.domain.Customer;
import com.evaitcs.securebank.account.domain.ResourceNotFoundException;
import com.evaitcs.securebank.account.domain.Transaction;
import com.evaitcs.securebank.account.repository.AccountRepository;
import com.evaitcs.securebank.account.repository.CustomerRepository;
import com.evaitcs.securebank.account.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Account operations — TWO families of methods, one per API surface:
 *
 *   PUBLIC (ownership-checked, called by AccountController with the JWT's
 *   customer id): openAccount / listForCustomer / getOwnedAccount /
 *   deposit / withdraw / statement.
 *
 *   INTERNAL (id-addressed, NO ownership check, called by
 *   InternalAccountController on payment-service's behalf): debit / credit /
 *   view. Authorization for these lives in the NETWORK: the gateway has no
 * route for /internal/**, and in Kubernetes a NetworkPolicy restricts the
 * endpoints to payment-service's pods (see infra/k8s). "Don't trust the
 * network" and "don't re-check what the perimeter guaranteed" are in tension
 * — real estates resolve it with mTLS between services; we document the gap
 * honestly instead of pretending it doesn't exist.
 *
 * Both families write ledger rows with the caller's `reference`, so a saga
 * (reference "xfer-<id>") and a compensation ("xfer-<id>-comp") are traceable
 * end to end in ONE table.
 */
@Service
public class AccountService {

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

    // ------------------------------------------------------------------
    // PUBLIC family (ownership enforced)
    // ------------------------------------------------------------------

    @Transactional
    public Account openAccount(Long customerId, AccountType type) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        return accounts.save(new Account(customer, type));
    }

    @Transactional(readOnly = true)
    public List<Account> listForCustomer(Long customerId) {
        return accounts.findByCustomerIdOrderByTypeAsc(customerId);
    }

    /** 404 (not 403) when the account belongs to someone else — no existence leak. */
    @Transactional(readOnly = true)
    public Account getOwnedAccount(Long accountId, Long customerId) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        if (!account.getCustomer().getId().equals(customerId)) {
            throw new ResourceNotFoundException("Account", accountId);
        }
        return account;
    }

    @Transactional
    public Account deposit(Long accountId, Long customerId, BigDecimal amount, String description) {
        Account account = getOwnedAccount(accountId, customerId);
        account.deposit(amount);
        transactions.save(Transaction.credit(account, amount,
                description == null ? "Deposit" : description, UUID.randomUUID().toString()));
        return account;
    }

    @Transactional
    public Account withdraw(Long accountId, Long customerId, BigDecimal amount, String description) {
        Account account = getOwnedAccount(accountId, customerId);
        account.withdraw(amount);
        transactions.save(Transaction.debit(account, amount,
                description == null ? "Withdrawal" : description, UUID.randomUUID().toString()));
        return account;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> statement(Long accountId, Pageable pageable) {
        return transactions.findByAccountIdOrderByOccurredAtDesc(accountId, pageable);
    }

    // ------------------------------------------------------------------
    // INTERNAL family (network-authorized; saga primitives)
    // ------------------------------------------------------------------

    /** Locked read used by payment-service for ownership/balance checks. */
    @Transactional(readOnly = true)
    public Account view(Long accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
    }

    /**
     * Saga DEBIT primitive: pessimistic lock -> guarded withdraw -> ledger row.
     * One local ACID transaction — atomicity INSIDE this service is exactly
     * what the saga gets to keep; atomicity ACROSS services is what it lost
     * (and compensates for). Insufficient funds -> InsufficientFundsException
     * -> HTTP 400 -> payment-service marks the transfer FAILED without
     * compensation (nothing was debited).
     */
    @Transactional
    public Account debit(Long accountId, BigDecimal amount, String reference, String description) {
        Account account = lockOrThrow(accountId);
        account.withdraw(amount);
        transactions.save(Transaction.debit(account, amount, description, reference));
        return account;
    }

    /**
     * Saga CREDIT primitive — also used for COMPENSATION (undoing a debit):
     * same operation, reference suffixed "-comp" by the caller so the ledger
     * tells the two apart.
     */
    @Transactional
    public Account credit(Long accountId, BigDecimal amount, String reference, String description) {
        Account account = lockOrThrow(accountId);
        account.deposit(amount);
        transactions.save(Transaction.credit(account, amount, description, reference));
        return account;
    }

    private Account lockOrThrow(Long id) {
        return accounts.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }
}
