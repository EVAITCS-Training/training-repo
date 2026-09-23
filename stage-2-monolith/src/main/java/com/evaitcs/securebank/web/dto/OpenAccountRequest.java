package com.evaitcs.securebank.web.dto;

import com.evaitcs.securebank.domain.AccountType;
import jakarta.validation.constraints.NotNull;

/** POST /api/accounts body — "open me a CHECKING or SAVINGS account". */
public record OpenAccountRequest(
        @NotNull AccountType type
) {
}
