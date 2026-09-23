package com.evaitcs.securebank.domain;

/** Lifecycle states of a loan — see Loan for the guarded transitions. */
public enum LoanStatus {
    APPLIED,
    APPROVED,
    REJECTED,
    PAID_OFF
}
