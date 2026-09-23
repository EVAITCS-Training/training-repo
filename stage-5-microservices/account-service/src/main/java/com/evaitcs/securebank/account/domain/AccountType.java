package com.evaitcs.securebank.account.domain;

/** Account kinds (duplicated-per-context rationale: see auth-service Role). */
public enum AccountType {
    CHECKING,
    SAVINGS
}
