package com.evaitcs.corejava.tostring;

import java.math.BigDecimal;

/**
 * The BEFORE picture: a perfectly normal SecureBank account that overrides NOTHING.
 *
 * Every class you ever write starts life looking like this one. It has four fields —
 * including a secret `pin` — and because it inherits Object.toString(), printing it
 * shows ZERO of them. Instead you get:
 *
 *     com.evaitcs.corejava.tostring.AccountRaw@1b6d3586
 *
 * which is getClass().getName() + "@" + Integer.toHexString(hashCode()). The hex part
 * is the IDENTITY hash code (module 08: the default hashCode is identity-based), so it
 * works as a debug fingerprint — but it tells a human nothing about the account.
 *
 * Side-by-side with AccountPrinted (the fix) and AccountLeaky (the over-share), this
 * class is one corner of the three-way contrast the whole module is built on.
 */
public class AccountRaw {

    private final String id;
    private final String owner;
    private final BigDecimal balance; // money is ALWAYS BigDecimal (module 02), never double
    private final String pin;         // SECRET — and thanks to the missing toString, it stays hidden here

    public AccountRaw(String id, String owner, BigDecimal balance, String pin) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
        this.pin = pin;
    }

    public String getId() { return id; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }

    // No toString(). No hashCode(). No equals(). That absence IS the teaching point:
    // DefaultToStringDemo prints this object and decodes what the default actually says.
}
