package com.evaitcs.corejava.tostring;

import java.math.BigDecimal;

/**
 * The AFTER picture: the same account with a GOOD toString() — readable at a glance,
 * built with StringBuilder (module 04), and SAFE: secrets are masked, never exported.
 *
 * Three design decisions worth pointing at on the projector, top to bottom:
 *
 * 1. getClass().getSimpleName() instead of a hardcoded "AccountPrinted[".
 *    toString is inherited like any other method, but `this` is always the RUNTIME
 *    object — so getClass() asks "what are you ACTUALLY?" and subclasses print their
 *    own name automatically (InheritedToStringDemo proves it with SavingsAccount).
 *
 * 2. key=value style — the Eclipse/IntelliJ default shape: ClassName[id=..., owner=...].
 *    It is greppable ("search the logs for id=ACC-1001"), diffable, and unambiguous.
 *    The alternative is a human sentence ("Ada Lovelace's account, balance ...") —
 *    nicer in UI messages, worse in logs. Customer shows that style; pick per audience.
 *
 * 3. Secrets are MASKED, not printed. toString is a data-export API: every log line,
 *    error message and debug print is an export. pin becomes "****" (ALL digits hidden —
 *    a 4-digit pin "ending in 1234" would be the whole pin), the card keeps its last 4
 *    ("****1234") because support staff genuinely need that to identify a card.
 */
public class AccountPrinted {

    private final String id;
    private final String owner;
    private final BigDecimal balance;
    private final String cardNumber;
    private final String pin; // SECRET — this field must NEVER appear in toString output

    public AccountPrinted(String id, String owner, BigDecimal balance, String cardNumber, String pin) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
        this.cardNumber = cardNumber;
        this.pin = pin;
    }

    public String getId() { return id; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }
    public String getCardNumber() { return cardNumber; }

    @Override
    public String toString() {
        // StringBuilder, the CLASSIC assembly idiom (module 04): one mutable buffer,
        // append piece by piece, one String at the end. No += chains rebuilding
        // intermediate Strings, no String.format ceremony for simple key=value lines.
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append('[');   // decision 1: runtime class name
        sb.append("id=").append(id);                          // decision 2: key=value, greppable
        sb.append(", owner=").append(owner);
        sb.append(", balance=").append(balance.toPlainString()); // toPlainString: never "1.2E+3" for money
        sb.append(", card=").append(maskCard(cardNumber));    // decision 3: masked, not exported
        sb.append(", pin=").append(maskPin(pin));
        sb.append(']');
        return sb.toString();
    }

    /**
     * Card masking keeps the LAST 4 digits: "4111111111111234" -> "****1234".
     * That is enough for a human to pick the right card, far too little to use it.
     */
    public static String maskCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****"; // dirty data must not crash toString — a debug method that throws is worse than useless
        }
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * PIN masking keeps NOTHING. The trap to name out loud: the "show last 4" card rule
     * applied to a 4-digit pin would print the ENTIRE secret. Different field, different rule.
     */
    public static String maskPin(String pin) {
        return "****"; // no part of a pin ever reaches a log line, an exception message, or a UI
    }
}
