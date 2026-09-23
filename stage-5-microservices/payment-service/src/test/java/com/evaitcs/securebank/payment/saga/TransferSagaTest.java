package com.evaitcs.securebank.payment.saga;

import com.evaitcs.securebank.payment.client.AccountClient;
import com.evaitcs.securebank.payment.client.AccountServiceUnavailableException;
import com.evaitcs.securebank.payment.client.InsufficientFundsException;
import com.evaitcs.securebank.payment.domain.Transfer;
import com.evaitcs.securebank.payment.domain.TransferStatus;
import com.evaitcs.securebank.payment.events.TransferEventPublisher;
import com.evaitcs.securebank.payment.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SAGA UNIT TESTS — every branch of the state machine, in milliseconds,
 * with the entire distributed cast played by Mockito:
 *   AccountClient   -> the remote account-service (success / refusal / outage)
 *   Publisher       -> Kafka
 *   Repository      -> paymentdb
 *
 * This is the Week 8 pyramid point made concrete: the scariest logic in the
 * system (money + partial failure) is the CHEAPEST to test once the seams
 * (interfaces/beans) are right. The compose-stack smoke test then proves the
 * wiring; this file proves the branches.
 *
 * References are UUIDs ("xfer-<uuid>"), so tests CAPTURE the reference from
 * the first persisted transfer and assert the same value flows through the
 * debit, credit, compensation and events — correlation integrity is part of
 * what these tests protect.
 */
@ExtendWith(MockitoExtension.class)
class TransferSagaTest {

    @Mock
    TransferRepository transfers;
    @Mock
    AccountClient accountClient;
    @Mock
    TransferEventPublisher.Publisher events;

    TransferSagaService saga;

    @BeforeEach
    void setUp() {
        saga = new TransferSagaService(transfers, accountClient, events);
        // repository.save stub: assign an id like IDENTITY would, return the entity.
        // lenient(): the guard-rail test throws BEFORE any save, and Mockito's
        // strict stubbing would otherwise fail it as "unnecessary stubbing" —
        // a shared-setup vs strictness trade-off worth knowing (Week 8).
        Mockito.lenient().when(transfers.save(any(Transfer.class))).thenAnswer(inv -> {
            Transfer t = inv.getArgument(0);
            if (ReflectionTestUtils.getField(t, "id") == null) {
                ReflectionTestUtils.setField(t, "id", 42L);
            }
            return t;
        });
    }

    /** The reference from the FIRST persisted transfer row (the saga intent). */
    private String firstReference() {
        ArgumentCaptor<Transfer> saved = ArgumentCaptor.forClass(Transfer.class);
        verify(transfers, atLeastOnce()).save(saved.capture());
        return saved.getAllValues().get(0).getReference();
    }

    private Transfer lastSaved() {
        ArgumentCaptor<Transfer> saved = ArgumentCaptor.forClass(Transfer.class);
        verify(transfers, atLeastOnce()).save(saved.capture());
        return saved.getValue();
    }

    private AccountClient.AccountView aliceView() {
        return new AccountClient.AccountView(1L, 1L, "CHECKING", new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("happy path: debit -> credit -> COMPLETED + TransferCompleted event")
    void completesWhenBothLegsSucceed() {
        when(accountClient.view(1L)).thenReturn(aliceView());

        Transfer result = saga.transfer(1L, 2L, new BigDecimal("100.00"), 1L);

        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        String ref = firstReference();
        assertThat(ref).startsWith("xfer-");
        assertThat(result.getReference()).isEqualTo(ref);
        // the SAME reference rides on both ledger movements and the event
        verify(accountClient).debit(eq(1L), eq(new BigDecimal("100.00")), eq(ref), anyString());
        verify(accountClient).credit(eq(2L), eq(new BigDecimal("100.00")), eq(ref), anyString());
        verify(events).publishCompleted(ref, 1L, 2L, new BigDecimal("100.00"));
        verify(events, never()).publishFailed(anyString(), anyLong(), anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("business refusal at debit: FAILED, no credit, no compensation")
    void insufficientFundsFailsCleanly() {
        when(accountClient.view(1L)).thenReturn(aliceView());
        when(accountClient.debit(eq(1L), any(), anyString(), anyString()))
                .thenThrow(new InsufficientFundsException("Account 1 has 3.00 available but 9999.00 was requested"));

        assertThatThrownBy(() -> saga.transfer(1L, 2L, new BigDecimal("9999.00"), 1L))
                .isInstanceOf(InsufficientFundsException.class);

        Transfer failed = lastSaved();
        assertThat(failed.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(failed.getReason()).contains("available");

        verify(accountClient, never()).credit(anyLong(), any(), anyString(), anyString()); // nothing to undo
        verify(events).publishFailed(eq(failed.getReference()), eq(1L), eq(2L), any(), contains("available"));
    }

    @Test
    @DisplayName("credit leg dies: debit is COMPENSATED with '-comp' reference")
    void compensatesWhenCreditFails() {
        when(accountClient.view(1L)).thenReturn(aliceView());
        when(accountClient.credit(eq(2L), any(), anyString(), anyString()))
                .thenThrow(new AccountServiceUnavailableException("timeout on credit"));

        assertThatThrownBy(() -> saga.transfer(1L, 2L, new BigDecimal("100.00"), 1L))
                .isInstanceOf(TransferSagaService.SagaCompensatedException.class);

        // capture the reference the debit used...
        ArgumentCaptor<String> debitRef = ArgumentCaptor.forClass(String.class);
        verify(accountClient).debit(eq(1L), any(), debitRef.capture(), anyString());

        // ...and assert the compensation credits the SOURCE with ref + "-comp"
        verify(accountClient).credit(eq(1L), eq(new BigDecimal("100.00")),
                eq(debitRef.getValue() + "-comp"), contains("Compensation"));

        assertThat(lastSaved().getStatus()).isEqualTo(TransferStatus.COMPENSATED);
        verify(events).publishFailed(eq(debitRef.getValue()), eq(1L), eq(2L), any(), anyString());
    }

    @Test
    @DisplayName("worst case: credit AND compensation fail -> COMPENSATION_FAILED (the alarm state)")
    void alarmStateWhenCompensationFails() {
        when(accountClient.view(1L)).thenReturn(aliceView());
        when(accountClient.credit(anyLong(), any(), anyString(), anyString()))
                .thenThrow(new AccountServiceUnavailableException("account-service is down"));

        assertThatThrownBy(() -> saga.transfer(1L, 2L, new BigDecimal("100.00"), 1L))
                .isInstanceOf(TransferSagaService.SagaCompensatedException.class);

        Transfer alarm = lastSaved();
        assertThat(alarm.getStatus()).isEqualTo(TransferStatus.COMPENSATION_FAILED);
        verify(events).publishFailed(eq(alarm.getReference()), eq(1L), eq(2L), any(),
                contains("manual reconciliation"));
    }

    @Test
    @DisplayName("ownership: transferring from someone else's account fails with 404-style error")
    void rejectsForeignSourceAccount() {
        when(accountClient.view(3L)).thenReturn(new AccountClient.AccountView(3L, 2L, "CHECKING", new BigDecimal("300.00")));

        // initiator claims cid=1 but account 3 belongs to customer 2
        assertThatThrownBy(() -> saga.transfer(3L, 1L, BigDecimal.TEN, 1L))
                .isInstanceOf(TransferSagaService.TransferNotFoundException.class);

        verify(accountClient, never()).debit(anyLong(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("guard rails: positive amounts and distinct accounts only")
    void rejectsNonsense() {
        assertThatThrownBy(() -> saga.transfer(1L, 1L, BigDecimal.TEN, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> saga.transfer(1L, 2L, BigDecimal.ZERO, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(transfers, never()).save(any()); // never even persisted the intent
    }
}
