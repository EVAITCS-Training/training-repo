package com.evaitcs.securebank.loan.domain;

/** Loan lifecycle — guarded transitions live on the entity (approve/reject). */
public enum LoanStatus {
    APPLIED,
    APPROVED,
    REJECTED,
    PAID_OFF
}
