package com.evaitcs.securebank.account.web;

import com.evaitcs.securebank.account.domain.Account;
import com.evaitcs.securebank.account.domain.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * DTO records for the account API — same shapes as stage-2 (the contract the
 * frontend already consumes). Colocated in one file: at this service's size
 * the ceremony of ten files obscures more than it organizes; the RULE that
 * matters is "entities never cross the web boundary" (see stage-2 web/dto
 * comments for why).
 */
public final class Dtos {

    private Dtos() {
    }

    public record AccountResponse(Long id, String type, BigDecimal balance, Long customerId) {
        public static AccountResponse from(Account a) {
            return new AccountResponse(a.getId(), a.getType().name(), a.getBalance(),
                    a.getCustomer().getId());
        }
    }

    public record TransactionResponse(Long id, String type, BigDecimal amount, String description,
                                      String reference, Instant occurredAt) {
        public static TransactionResponse from(Transaction t) {
            return new TransactionResponse(t.getId(), t.getType().name(), t.getAmount(),
                    t.getDescription(), t.getReference(), t.getOccurredAt());
        }
    }

    public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <E, T> PageDto<T> from(org.springframework.data.domain.Page<E> p, Function<E, T> mapper) {
            return new PageDto<>(p.getContent().stream().map(mapper).toList(),
                    p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }

    public record OpenAccountRequest(@jakarta.validation.constraints.NotNull
                                     com.evaitcs.securebank.account.domain.AccountType type) {
    }

    public record CashRequest(
            @jakarta.validation.constraints.NotNull
            @jakarta.validation.constraints.DecimalMin("0.01")
            @jakarta.validation.constraints.Digits(integer = 13, fraction = 2)
            BigDecimal amount,
            @jakarta.validation.constraints.Size(max = 200) String description) {
    }

    /** Body of the INTERNAL debit/credit calls (payment-service -> here). */
    public record MoneyMovementRequest(
            @jakarta.validation.constraints.NotNull
            @jakarta.validation.constraints.DecimalMin("0.01")
            @jakarta.validation.constraints.Digits(integer = 13, fraction = 2)
            BigDecimal amount,
            @jakarta.validation.constraints.NotBlank String reference,
            String description) {
    }

    /** Result of an internal movement — the saga reports balances back. */
    public record MoneyMovementResponse(Long accountId, BigDecimal newBalance) {
        public static MoneyMovementResponse from(Account a) {
            return new MoneyMovementResponse(a.getId(), a.getBalance());
        }
    }

    /** Internal read model used by payment-service for ownership checks. */
    public record AccountView(Long id, Long customerId, String type, BigDecimal balance) {
        public static AccountView from(Account a) {
            return new AccountView(a.getId(), a.getCustomer().getId(), a.getType().name(), a.getBalance());
        }
    }

    public record CustomerResponse(Long id, String name, String email) {
    }
}
