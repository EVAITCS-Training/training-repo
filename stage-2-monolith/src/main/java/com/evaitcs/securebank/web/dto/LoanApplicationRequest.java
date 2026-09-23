package com.evaitcs.securebank.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * POST /api/loans body. The rate is NOT client-supplied — the bank prices
 * the loan (LoanService.STANDARD_ANNUAL_RATE). Letting clients send their
 * own interest rate would be a business-logic vulnerability (Week 9:
 * "mass assignment" — never bind more fields than the user should control).
 */
public record LoanApplicationRequest(
        @NotNull
        @DecimalMin("500.00")
        @Digits(integer = 13, fraction = 2)
        BigDecimal principal,

        @NotNull @Min(6) @Max(360)
        Integer termMonths
) {
}
