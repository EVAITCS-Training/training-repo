package com.evaitcs.collections.choosing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The bank's account registry — the subject of the encapsulation lesson.
 *
 * One private List<Account>, THREE ways to hand it out, and each getter is a different
 * answer to the question "how much of my internals does the caller now control?":
 *
 *   getAccountsLeaky()    -> the internal list ITSELF. The caller's add() mutates the
 *                            bank. This is not a bug you can see at the call site — the
 *                            method signature looks perfectly innocent.
 *   getAccountsCopy()     -> new ArrayList<Account>(internal): a SNAPSHOT. Caller
 *                            mutations die in the copy; later internal changes are
 *                            invisible to the caller. Costs one O(n) copy per call.
 *   getAccountsReadOnly() -> Collections.unmodifiableList(internal): a live WINDOW
 *                            (module 07). Caller writes throw UnsupportedOperationException,
 *                            no copy cost — but later INTERNAL changes show through, so
 *                            callers must not treat two reads as consistent.
 *
 * Which is right? It depends on the promise you owe: a report wants a snapshot, a live
 * dashboard wants the window, and nothing ever wants the leak. The tests in this module
 * (ChoosingTest) pin all three behaviors down; EncapsulationDemo prints them.
 */
public class AccountRegistry {

    /** THE internal state. Private, final reference, never returned directly... except by the leaky getter, on purpose. */
    private final List<Account> accounts = new ArrayList<Account>();

    public AccountRegistry() {
        accounts.addAll(Account.sampleAccounts());
    }

    /** The bank's own write path — the "internal change" the window getter will reveal. */
    public void addAccount(Account account) {
        accounts.add(account);
    }

    public int size() {
        return accounts.size();
    }

    /** WRONG on purpose: hands out the internal list itself. Caller mutations hit the bank directly. */
    public List<Account> getAccountsLeaky() {
        return accounts;
    }

    /** SNAPSHOT: a fresh ArrayList with the same elements (equals-by-id, module 01). */
    public List<Account> getAccountsCopy() {
        return new ArrayList<Account>(accounts);
    }

    /** WINDOW: writes throw, reads forward live — module 07's lesson, as an API decision. */
    public List<Account> getAccountsReadOnly() {
        return Collections.unmodifiableList(accounts);
    }
}
