package com.evaitcs.corejava.tostring;

import java.math.BigDecimal;

/**
 * The OVER-SHARE: a toString that exports every field, secrets included.
 *
 * This class exists so the damage is printable. IDEs generate toString from ALL fields
 * by default — if you accept the field list without reading it, you get exactly this.
 * SecurityMaskingDemo drops one of these objects into a single DEBUG log line and the
 * pin and secretKey end up in plaintext: log files, log-shipping pipelines, and
 * error-tracking sites all happily store and forward whatever toString handed them.
 *
 * THE RULE this class violates: toString is a data-export API. Treat the field list
 * like the payload of an HTTP response — passwords, PINs, tokens and full card numbers
 * never belong in it.
 */
public class AccountLeaky {

    private final String id;
    private final String owner;
    private final BigDecimal balance;
    private final String pin;       // SECRET
    private final String secretKey; // SECRET — an API key used by the online-banking backend

    public AccountLeaky(String id, String owner, BigDecimal balance, String pin, String secretKey) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
        this.pin = pin;
        this.secretKey = secretKey;
    }

    public String getId() { return id; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }

    @Override
    public String toString() {
        // Looks tidy. Looks "complete". Looks exactly like what an IDE generates when
        // you click "include all fields" — and it is a credential leak in method form.
        return "AccountLeaky[id=" + id
                + ", owner=" + owner
                + ", balance=" + balance.toPlainString()
                + ", pin=" + pin                 // <- the pin, in plaintext
                + ", secretKey=" + secretKey     // <- the backend API key, in plaintext
                + "]";
    }
}
