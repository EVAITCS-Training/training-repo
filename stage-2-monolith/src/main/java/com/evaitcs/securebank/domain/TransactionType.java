package com.evaitcs.securebank.domain;

/** Direction of a ledger entry. */
public enum TransactionType {
    DEBIT,   // money leaving the account
    CREDIT   // money entering the account
}
