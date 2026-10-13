package com.evaitcs.corejava.tostring;

import java.math.BigDecimal;

/**
 * The inheritance probe: a subclass of AccountPrinted that adds ONE field and
 * deliberately writes NO toString().
 *
 * What prints when you println(new SavingsAccount(...))?
 *
 *   SavingsAccount[id=ACC-2002, owner=Grace Hopper, balance=9300.10, card=****5678, pin=****]
 *
 * Two lessons in one line:
 *
 * 1. The NAME is right — "SavingsAccount", not "AccountPrinted" — even though the
 *    method that ran was the superclass's. That is the getClass().getSimpleName()
 *    trick: toString is looked up the inheritance chain, but inside it `this` is the
 *    runtime object, so getClass() reports SavingsAccount. (InheritedToStringDemo shows
 *    the hardcoded-name version where the subclass prints as "Account[...]" and lies.)
 *
 * 2. interestRate is MISSING. The trick fixes the name, not the fields: a superclass
 *    cannot print fields it has never heard of. So a subclass that adds state should
 *    still override toString to append its own fields — but even if you forget, the
 *    output is incomplete rather than deceptive about the type.
 */
public class SavingsAccount extends AccountPrinted {

    private final BigDecimal interestRate; // the new state the superclass knows nothing about

    public SavingsAccount(String id, String owner, BigDecimal balance,
                          String cardNumber, String pin, BigDecimal interestRate) {
        super(id, owner, balance, cardNumber, pin);
        this.interestRate = interestRate;
    }

    public BigDecimal getInterestRate() { return interestRate; }

    // No toString() here — on purpose. Run InheritedToStringDemo and watch the
    // inherited method still print the CORRECT class name thanks to getClass().
}
