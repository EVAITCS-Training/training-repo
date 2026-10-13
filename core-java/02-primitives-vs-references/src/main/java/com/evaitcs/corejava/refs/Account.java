package com.evaitcs.corejava.refs;

import java.math.BigDecimal;

/**
 * The tiny SecureBank domain object every reference demo in this module shares.
 *
 * WHY IT LIVES HERE (say this in class): a demo about "what a variable holds" needs a real
 * OBJECT to point at. A primitive can't be aliased or mutated through a second name, but an
 * Account can — so it is the perfect subject for the stack-vs-heap story.
 *
 * NOTE FOR CLASS: this is Java 8, so no records — a plain class with getters. And it is
 * deliberately MUTABLE (deposit/setBalance): mutation is the whole point of the aliasing and
 * pass-by-value demos. We do NOT override equals()/hashCode() yet — the default equals IS `==`,
 * which is exactly the "same object?" question this module asks. Module 08 fixes equals to
 * compare CONTENT; for now leaving it inherited is the honest, teachable state.
 */
public class Account {

    private final String id;
    private final String owner;
    private BigDecimal balance; // money is ALWAYS BigDecimal, scale 2 — MoneyDoubleVsBigDecimalDemo shows why

    public Account(String id, String owner, BigDecimal balance) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
    }

    public String getId() { return id; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }

    /** A mutation we can drive through a reference to prove aliasing / pass-by-value. */
    public void deposit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return String.format("Account[%s owner=%s balance=%s]", id, owner, balance.toPlainString());
    }
}
