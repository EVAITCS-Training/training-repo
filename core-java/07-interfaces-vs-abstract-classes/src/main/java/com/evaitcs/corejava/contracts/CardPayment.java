package com.evaitcs.corejava.contracts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * MULTIPLE INHERITANCE OF TYPE, in one declaration:
 *
 *   class CardPayment implements Transferable, PaymentMethod, Auditable, Comparable&lt;CardPayment&gt;
 *
 * ONE class slot (spent on Object — there is no extends here), FOUR interface
 * slots. CardPayment is NOT an Account — different family, no shared ancestor —
 * yet a loop over List&lt;Transferable&gt; can hold both, because interfaces cut
 * ACROSS class trees. That is the whole trick.
 *
 * Being concrete, it MUST provide EVERY body of EVERY interface — miss one and
 * javac says, word for word:
 *   error: CardPayment is not abstract and does not override abstract method auditTrail() in Auditable
 *
 * And look at recordAudit/auditTrail below: near-identical plumbing to Account,
 * duplicated because CardPayment cannot inherit from Account (slot spent) and
 * pre-Java-8 interfaces cannot carry bodies. THAT duplication is the problem
 * Java 8 default methods were invented to fix — java8-features/02-interface-evolution.
 */
public class CardPayment implements Transferable, PaymentMethod, Auditable, Comparable<CardPayment> {

    private final String cardId;
    private BigDecimal loaded;                                    // its OWN state — interfaces gave it none
    private final List<String> auditLog = new ArrayList<String>();

    public CardPayment(String cardId, BigDecimal loaded) {
        this.cardId = cardId;
        this.loaded = loaded.setScale(2);
        recordAudit("card loaded with " + this.loaded);
    }

    @Override
    public void deposit(BigDecimal amount) {                      // Transferable
        loaded = loaded.add(amount);
        recordAudit("topped up " + amount);
    }

    @Override
    public boolean transferTo(Transferable target, BigDecimal amount) {  // Transferable
        if (amount.compareTo(loaded) > 0) {
            recordAudit("BLOCKED transfer of " + amount + " — card not loaded enough");
            return false;
        }
        loaded = loaded.subtract(amount);
        target.deposit(amount);
        recordAudit("transferred " + amount + " to " + target);
        return true;
    }

    @Override
    public void pay(BigDecimal amount) {                          // PaymentMethod
        loaded = loaded.subtract(amount);
        recordAudit("paid " + amount);
    }

    @Override
    public void recordAudit(String event) {                       // Auditable — the duplicated plumbing
        auditLog.add(AUDIT_PREFIX + " " + cardId + " " + event);
    }

    @Override
    public String auditTrail() {                                  // Auditable
        StringBuilder sb = new StringBuilder();
        for (String entry : auditLog) {
            sb.append(entry).append(System.lineSeparator());
        }
        return sb.toString();
    }

    @Override
    public int compareTo(CardPayment other) {                     // Comparable — natural order by card id
        return cardId.compareTo(other.cardId);
    }

    public String getCardId()      { return cardId; }
    public BigDecimal getLoaded()  { return loaded; }

    @Override
    public String toString() {
        return "CardPayment " + cardId + " loaded " + loaded;
    }
}
