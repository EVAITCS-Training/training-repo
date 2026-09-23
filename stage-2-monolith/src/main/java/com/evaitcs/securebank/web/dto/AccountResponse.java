package com.evaitcs.securebank.web.dto;

import com.evaitcs.securebank.domain.Account;

import java.math.BigDecimal;

/**
 * Account JSON shape. The static factory `from(entity)` is the mapping
 * seam — entities never escape the web layer (see LoginRequest for WHY).
 *
 * Note what is NOT here: the @Version field (internal locking detail),
 * the Customer object (we flatten to customerId — sending nested entities
 * risks lazy-loading errors and over-exposure).
 */
public record AccountResponse(
        Long id,
        String type,
        BigDecimal balance,
        Long customerId
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getType().name(),
                account.getBalance(),
                account.getCustomer().getId());
    }
}
