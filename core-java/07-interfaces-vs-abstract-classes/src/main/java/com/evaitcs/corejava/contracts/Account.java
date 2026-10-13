package com.evaitcs.corejava.contracts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The abstract class of the module: the IS-A family "SecureBank account" with
 * everything relatives SHARE — state, a constructor, plumbing — plus two blanks
 * each subclass must fill.
 *
 * What this class has that NO interface can have (pre-Java-8):
 *   - INSTANCE FIELDS  (id, balance, auditLog): one copy PER OBJECT — that is state
 *   - A CONSTRUCTOR    (called by subclasses via super(...))
 *   - METHOD BODIES    (deposit, withdraw, pay, audit plumbing, the template method)
 *   - PROTECTED members (monthlyFee, withdraw): visible to the family only
 *
 * It also proves a subtlety: an abstract class may IMPLEMENT interfaces and
 * leave nothing behind (Auditable and PaymentMethod are fully concrete here)
 * or leave everything behind — either way the debt passes to concrete subclasses.
 *
 * And the line nobody can write, word for word from javac:
 *   Account a = new Account("X-1", BigDecimal.ZERO);
 *   error: Account is abstract; cannot be instantiated
 */
public abstract class Account implements Auditable, PaymentMethod {

    // ---- STATE: each Account object carries its OWN copy of these fields ----
    protected final String id;                       // protected: the family sees it
    private BigDecimal balance;                      // private: the world uses getBalance()
    private final List<String> auditLog = new ArrayList<String>();

    // ---- CONSTRUCTOR: interfaces cannot have one; subclasses reach it via super(...) ----
    protected Account(String id, BigDecimal openingBalance) {
        this.id = id;
        this.balance = openingBalance.setScale(2, RoundingMode.UNNECESSARY);
        recordAudit("opened with " + balance);
        // Teaching println — makes the super()-first ordering VISIBLE in the demo.
        System.out.println("   [Account ctor]         finished: " + id + " now holds " + balance);
    }

    // ---- BLANKS: every concrete subclass MUST fill these (or be abstract too) ----

    /** Public blank: what yearly rate does this family of accounts earn? */
    public abstract BigDecimal interestRate();

    /** Protected blank: the family sees it, the outside world does not.
     *  An interface member could never be protected — only public. */
    protected abstract BigDecimal monthlyFee();

    /**
     * THE TEMPLATE METHOD — the abstract class killer app (TemplateMethodDemo).
     *
     * `final` here means: subclasses may NOT break the skeleton. The ORDER of
     * the steps (interest -> fee -> audit) is a compliance policy written once;
     * the AMOUNTS are the blanks subclasses fill. A subclass that tries to
     * override it gets, word for word:
     *   error: processMonthEnd() in GreedyAccount cannot override processMonthEnd() in Account
     *   overridden method is final
     */
    public final String processMonthEnd() {
        BigDecimal interest = balance.multiply(interestRate())          // BLANK 1
                .setScale(2, RoundingMode.HALF_UP);
        if (interest.signum() > 0) {                                    // skeleton step — the skeleton
            deposit(interest);                                          // knows a blank may answer 0.00
        }                                                               // (current accounts earn nothing)
        BigDecimal fee = monthlyFee();                                  // BLANK 2
        if (fee.signum() > 0) {                                         // skeleton step — and a blank may
            withdraw(fee);                                              // answer 0.00 (savings are fee-free)
        }
        recordAudit("month-end: interest +" + interest + ", fee -" + fee); // skeleton, uses STATE
        return auditTrail();
    }

    // ---- CONCRETE PLUMBING written ONCE for the whole family ----

    public void deposit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
    }

    protected void withdraw(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.subtract(amount);
    }

    /** The PaymentMethod contract, implemented with the shared plumbing above. */
    @Override
    public void pay(BigDecimal amount) {
        withdraw(amount);
        recordAudit("payment of " + amount);
    }

    private void requirePositive(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive, was " + amount);
        }
    }

    // ---- Auditable, fully concrete here, backed by STATE (auditLog) ----
    // AUDIT_PREFIX below is inherited from the interface — constants DO inherit.

    @Override
    public void recordAudit(String event) {
        auditLog.add(AUDIT_PREFIX + " " + id + " " + event);
    }

    @Override
    public String auditTrail() {
        StringBuilder sb = new StringBuilder();
        for (String entry : auditLog) {              // classic loop on purpose — no streams here
            sb.append(entry).append(System.lineSeparator());
        }
        return sb.toString();
    }

    public String getId()             { return id; }
    public BigDecimal getBalance()    { return balance; }
    public int getAuditCount()        { return auditLog.size(); }
}
