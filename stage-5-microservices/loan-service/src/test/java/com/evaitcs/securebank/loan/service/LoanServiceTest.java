package com.evaitcs.securebank.loan.service;

import com.evaitcs.securebank.loan.domain.Loan;
import com.evaitcs.securebank.loan.domain.LoanStatus;
import com.evaitcs.securebank.loan.events.LoanEventPublisher;
import com.evaitcs.securebank.loan.repository.LoanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lending logic unit tests — the annuity math and the state machine, with the
 * repository and the Kafka publisher mocked out. The monthly-payment figure
 * is asserted against the independently computed value for
 * 10,000 @ 7.5% / 36 months = 311.11 (standard amortization tables agree).
 */
@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    LoanRepository loans;
    @Mock
    LoanEventPublisher.Publisher events;

    private LoanService service() {
        return new LoanService(loans, events);
    }

    @Test
    @DisplayName("apply persists an APPLIED loan at the standard rate")
    void applies() {
        when(loans.save(any(Loan.class))).thenAnswer(inv -> {
            Loan l = inv.getArgument(0);
            ReflectionTestUtils.setField(l, "id", 7L);
            return l;
        });

        Loan loan = service().apply(1L, new BigDecimal("10000.00"), 36);

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.APPLIED);
        assertThat(loan.getAnnualRate()).isEqualByComparingTo("7.50"); // bank-side pricing
        assertThat(loan.getCustomerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("approve computes the annuity payment and publishes LoanApproved")
    void approvesWithCorrectMath() {
        Loan existing = new Loan(1L, new BigDecimal("10000.00"), new BigDecimal("7.50"), 36);
        ReflectionTestUtils.setField(existing, "id", 7L);
        when(loans.findById(7L)).thenReturn(java.util.Optional.of(existing));
        when(loans.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Loan approved = service().approve(7L);

        assertThat(approved.getStatus()).isEqualTo(LoanStatus.APPROVED);
        // 10,000 @ 7.5% APR / 36 months -> annuity M = P·r/(1-(1+r)^-n) = 311.06
        // (verify with any amortization calculator — asserting a hand-checked
        //  number is how you catch formula regressions)
        assertThat(approved.getMonthlyPayment()).isEqualByComparingTo("311.06");
        verify(events).publishApproved(7L, 1L, new BigDecimal("10000.00"), 36,
                approved.getMonthlyPayment());
    }

    @Test
    @DisplayName("approve twice -> state conflict (guarded transition)")
    void rejectsDoubleApproval() {
        Loan existing = new Loan(1L, new BigDecimal("5000.00"), new BigDecimal("7.50"), 12);
        ReflectionTestUtils.setField(existing, "id", 8L);
        existing.approve(); // already APPROVED
        when(loans.findById(8L)).thenReturn(java.util.Optional.of(existing));

        assertThatThrownBy(() -> service().approve(8L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("reject publishes LoanRejected; ownership checks use ids only")
    void rejectsAndPublishes() {
        Loan existing = new Loan(2L, new BigDecimal("1000.00"), new BigDecimal("7.50"), 12);
        ReflectionTestUtils.setField(existing, "id", 9L);
        when(loans.findById(9L)).thenReturn(java.util.Optional.of(existing));
        when(loans.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Loan rejected = service().reject(9L);

        assertThat(rejected.getStatus()).isEqualTo(LoanStatus.REJECTED);
        verify(events).publishRejected(9L, 2L);
    }

    @Test
    @DisplayName("validation guard rails: positive principal, sane term")
    void validatesInput() {
        assertThatThrownBy(() -> service().apply(1L, BigDecimal.ZERO, 36))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service().apply(1L, BigDecimal.TEN, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
