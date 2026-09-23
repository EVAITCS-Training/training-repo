package com.evaitcs.securebank.service;

import com.evaitcs.securebank.domain.Account;
import com.evaitcs.securebank.domain.AccountType;
import com.evaitcs.securebank.domain.Customer;
import com.evaitcs.securebank.domain.InsufficientFundsException;
import com.evaitcs.securebank.domain.ResourceNotFoundException;
import com.evaitcs.securebank.domain.Transaction;
import com.evaitcs.securebank.repository.AccountRepository;
import com.evaitcs.securebank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UNIT TEST — TransferService in complete isolation (Week 8: the bottom of
 * the testing pyramid — fast, no Spring context, no database).
 *
 * TECHNIQUE: Mockito @Mock replaces each repository with a programmable stub.
 * The service under test is REAL; everything it talks to is FAKE. That's why
 * constructor injection matters: we can `new TransferService(mock, mock)`
 * with zero framework involvement.
 *
 * (JPA entities have no setId — ids come from the database. ReflectionTestUtils
 * is the standard test seam for assigning them; production code never does this.)
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    AccountRepository accounts;
    @Mock
    TransactionRepository transactions;

    TransferService service;

    Customer alice;
    Account checking; // id 1 — alice's
    Account savings;  // id 2 — alice's

    @BeforeEach
    void setUp() {
        service = new TransferService(accounts, transactions);

        alice = new Customer("Alice Nguyen", "alice@securebank.dev");
        ReflectionTestUtils.setField(alice, "id", 1L);

        checking = new Account(alice, AccountType.CHECKING);
        ReflectionTestUtils.setField(checking, "id", 1L);
        checking.deposit(new BigDecimal("1500.00"));

        savings = new Account(alice, AccountType.SAVINGS);
        ReflectionTestUtils.setField(savings, "id", 2L);
        savings.deposit(new BigDecimal("5200.00"));
    }

    @Test
    @DisplayName("happy path: balances move, two ledger rows share one reference")
    void transfersMoneyAndRecordsBothSides() {
        when(accounts.findById(1L)).thenReturn(Optional.of(checking));       // ownership probe
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(checking));
        when(accounts.findByIdForUpdate(2L)).thenReturn(Optional.of(savings));

        String reference = service.transfer(1L, 2L, new BigDecimal("100.00"), 1L);

        assertThat(checking.getBalance()).isEqualByComparingTo("1400.00");
        assertThat(savings.getBalance()).isEqualByComparingTo("5300.00");
        // isEqualByComparingTo — BigDecimal: 1400.00 vs 1400.0 differ by equals()
        // (scale!) but not compareTo. Money assertions ALWAYS use compareTo semantics.

        var saved = org.mockito.ArgumentCaptor.forClass(Transaction.class);
        verify(transactions, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues())
                .allSatisfy(t -> assertThat(t.getReference()).isEqualTo(reference))
                .extracting(Transaction::getAmount)
                .allSatisfy(a -> assertThat(a).isEqualByComparingTo("100.00"));
        assertThat(reference).isNotBlank();
    }

    @Test
    @DisplayName("deadlock prevention: locks are always taken in ascending id order")
    void locksAccountsInAscendingIdOrder() {
        when(accounts.findById(2L)).thenReturn(Optional.of(savings));        // transfer FROM id 2
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(checking));
        when(accounts.findByIdForUpdate(2L)).thenReturn(Optional.of(savings));

        service.transfer(2L, 1L, new BigDecimal("10.00"), 1L); // note: from=2, to=1

        // Even though the transfer direction is 2->1, the LOCK order must be 1,2.
        // If every transfer locks low-id-first, two opposing transfers can never
        // hold-one-wait-one on each other => no deadlock.
        InOrder locking = inOrder(accounts);
        locking.verify(accounts).findByIdForUpdate(1L);
        locking.verify(accounts).findByIdForUpdate(2L);
    }

    @Test
    @DisplayName("insufficient funds: throws and writes NO ledger rows")
    void rejectsOverdraft() {
        when(accounts.findById(1L)).thenReturn(Optional.of(checking));
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(checking));
        when(accounts.findByIdForUpdate(2L)).thenReturn(Optional.of(savings));

        assertThatThrownBy(() -> service.transfer(1L, 2L, new BigDecimal("9999.00"), 1L))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("1500.00");

        verify(transactions, never()).save(any()); // rollback means: nothing persisted
        assertThat(savings.getBalance()).isEqualByComparingTo("5200.00"); // target untouched
    }

    @Test
    @DisplayName("ownership: cannot transfer FROM someone else's account (404-style)")
    void rejectsForeignSourceAccount() {
        when(accounts.findById(1L)).thenReturn(Optional.of(checking)); // alice's account...

        // ...but the initiator claims to be customer 999 (not alice = 1).
        assertThatThrownBy(() -> service.transfer(1L, 2L, new BigDecimal("10.00"), 999L))
                .isInstanceOf(ResourceNotFoundException.class); // deliberately 404, not 403

        verify(accounts, never()).findByIdForUpdate(any()); // never even reached the locking
    }

    @Test
    @DisplayName("guard rails: self-transfer and non-positive amounts rejected")
    void rejectsNonsenseTransfers() {
        assertThatThrownBy(() -> service.transfer(1L, 1L, BigDecimal.TEN, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.transfer(1L, 2L, BigDecimal.ZERO, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
