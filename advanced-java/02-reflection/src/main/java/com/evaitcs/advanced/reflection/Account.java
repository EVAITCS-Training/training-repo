package com.evaitcs.advanced.reflection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The SecureBank account this whole module reflects ON. Every demo points its mirrors
 * at this one class, so its shape is deliberately rich enough to show every trick:
 *
 *   - private, private-final, and private mutable fields  -> InspectDemo / PrivateAccessDemo
 *   - a constructor with three typed parameters           -> ClassObjectDemo / newInstance
 *   - a method with a PRIMITIVE long parameter             -> InspectDemo getMethod exactness
 *   - a method that takes AND returns a value             -> MethodHandlesDemo
 *   - a generic List<Transaction> field                    -> InspectDemo getGenericType
 *
 * The domain is duplicated per module on purpose (README, "Shared conventions"): this
 * Account stands alone on screen and does not import module 01's Transaction.
 */
public class Account {

    private final String id;                 // private final: readable by reflection, not writable
    private String owner;                    // private MUTABLE: PrivateAccessDemo writes through it
    private BigDecimal balance;              // money is always BigDecimal (scale 2)
    private final List<Transaction> statement;   // generic signature survives erasure (module 01)

    public Account(String id, String owner, BigDecimal balance) {
        this.id = id;
        this.owner = owner;
        this.balance = balance.setScale(2, RoundingMode.HALF_UP);
        this.statement = new ArrayList<>();
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

    public List<Transaction> getStatement() {
        return List.copyOf(statement);
    }

    /**
     * Deposits and RETURNS the new balance. MethodHandlesDemo needs a method with both an
     * argument and a return value so it can prove a MethodHandle and a reflective
     * Method.invoke produce the SAME result on the SAME input.
     */
    public BigDecimal deposit(BigDecimal amount) {
        this.balance = this.balance.add(amount).setScale(2, RoundingMode.HALF_UP);
        this.statement.add(new Transaction("T-" + (statement.size() + 1), "Deposit", amount));
        return this.balance;
    }

    /**
     * A method whose parameter is the PRIMITIVE long. InspectDemo proves that
     * getMethod("transferCents", long.class) finds it, while
     * getMethod("transferCents", Long.class) throws NoSuchMethodException — reflection
     * matches the EXACT erased parameter type, and long is not Long.
     */
    public String transferCents(long cents) {
        BigDecimal amount = BigDecimal.valueOf(cents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        this.balance = this.balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        return id + " sent " + amount.toPlainString();
    }

    @Override
    public String toString() {
        return id + " | " + owner + " | " + balance.toPlainString();
    }
}
