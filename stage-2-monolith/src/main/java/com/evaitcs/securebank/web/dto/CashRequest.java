package com.evaitcs.securebank.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Deposit/withdraw body.
 *
 * @Digits(integer=13, fraction=2) mirrors DECIMAL(15,2) from stage-1: the API
 * rejects amounts the database column could never store — validation should
 * match the STRICTEST layer's limits, or errors surface late and ugly.
 */
public record CashRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        @Size(max = 200) String description
) {
}
