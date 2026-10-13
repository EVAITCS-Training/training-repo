package com.evaitcs.advanced.annotations;

import java.math.BigDecimal;

/**
 * The SecureBank account MiniValidator validates. This module duplicates the domain on
 * purpose (README, "Shared conventions") so it stands alone — but here the interesting part
 * is the STICKERS on the fields, not the fields themselves.
 *
 * Three different validation annotations are planted so MiniValidatorDemo can show a reader
 * walking the fields, reading each annotation, and reporting violations. The annotations do
 * NOTHING on their own — they are inert metadata until MiniValidator (module 02's reflection)
 * reads them. That is the central claim of this whole module.
 */
public class Account {

    @NotNull
    private final String id;

    @NotNull
    private final String owner;

    // Two stickers on one field: both are checked. A negative balance breaks @Min(0);
    // a wrong number of decimals would break @CurrencyScale(2).
    @Min(0)
    @CurrencyScale(2)
    private final BigDecimal balance;

    @CurrencyScale(2)
    private final BigDecimal withdrawalLimit;

    public Account(String id, String owner, BigDecimal balance, BigDecimal withdrawalLimit) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
        this.withdrawalLimit = withdrawalLimit;
    }

    public String getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getWithdrawalLimit() {
        return withdrawalLimit;
    }

    @Override
    public String toString() {
        return id + " | " + owner + " | " + balance + " | limit " + withdrawalLimit;
    }
}
