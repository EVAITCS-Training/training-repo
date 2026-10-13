package com.evaitcs.collections.choosing;

import java.math.BigDecimal;
import java.util.List;

/**
 * Demo 5 — ENCAPSULATION: three getters, three contracts, one internal list.
 *
 * "Which collection did you choose?" has a follow-up that breaks more bank software than
 * the choice itself: "and how do you HAND IT OUT?" AccountRegistry exposes the same
 * private List<Account> three ways, and each getter is a different promise:
 *
 *   getAccountsLeaky()    — the internal list itself. The caller's add() writes straight
 *                           into the bank's state. The signature looks innocent; the bug
 *                           is invisible at the call site. This is what `return accounts;`
 *                           means, and it is the default Java gives you if you think
 *                           "it's just a getter".
 *   getAccountsCopy()     — new ArrayList<Account>(internal): a SNAPSHOT (module 07's
 *                           copy idiom). Caller mutations die in the copy; later internal
 *                           changes never reach the caller. Cost: one O(n) copy per call.
 *   getAccountsReadOnly() — Collections.unmodifiableList(internal): the live WINDOW from
 *                           module 07. Writes throw UnsupportedOperationException at no
 *                           copy cost — but internal changes SHOW THROUGH, so two reads
 *                           in one caller method can disagree.
 *
 * The choice is the promise you owe: a printed report wants a snapshot (freeze the world
 * while you render it), a live dashboard wants the window (and must tolerate motion),
 * and nothing ever wants the leak. All three behaviors are asserted in ChoosingTest.
 */
public class EncapsulationDemo {

    public static void main(String[] args) {
        AccountRegistry registry = new AccountRegistry();

        banner("1. The leak — 'just returning the field' hands over the bank");

        List<Account> leaky = registry.getAccountsLeaky();
        System.out.println("registry.size() before = " + registry.size());
        leaky.add(new Account("ACC-3000", "Mallory", new BigDecimal("0.01")));   // through a GETTER
        System.out.println("caller does leaky.add(Mallory) — and registry.size() after = " + registry.size());
        System.out.println("The 'getter' was the internal list itself. Mallory is now a customer.");
        leaky.remove(leaky.size() - 1);   // undo the damage before the next banners
        System.out.println("(removed again to keep the demo honest — size back to " + registry.size() + ")");

        banner("2. The snapshot — a copy severs the wire in BOTH directions");

        List<Account> copy = registry.getAccountsCopy();
        copy.add(new Account("ACC-4000", "Fay", new BigDecimal("5.00")));
        System.out.println("caller adds Fay to the COPY -> registry.size() = " + registry.size() + " (unchanged, the copy absorbed it)");
        registry.addAccount(new Account("ACC-5000", "Gus", new BigDecimal("9.00")));
        System.out.println("bank adds Gus internally    -> copy.size() = " + copy.size()
                + " (Dana, Ana, Bo, Fay — Gus never reached the snapshot; it froze at call time)");

        banner("3. The window — writes throw, but the bank's own changes show through");

        List<Account> readOnly = registry.getAccountsReadOnly();   // taken NOW, size 4 (Dana, Ana, Bo, Gus)
        System.out.println("window taken: readOnly.size() = " + readOnly.size());
        try {
            readOnly.add(new Account("ACC-6000", "Mallory", new BigDecimal("0.01")));
        } catch (UnsupportedOperationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from readOnly.add(...)");
        }
        registry.addAccount(new Account("ACC-7000", "Hal", new BigDecimal("3.00")));   // AFTER the window was handed out
        System.out.println("bank adds Hal internally -> readOnly.size() = " + readOnly.size() + "   <- the window GREW");
        System.out.println("readOnly.contains(Hal) = " + readOnly.contains(new Account("ACC-7000", "anyone", BigDecimal.ZERO))
                + "   (equals by id — the window looks at the LIVE list, module 07)");

        banner("4. The taxonomy — one internal list, three contracts");

        System.out.println("  return accounts;                          LEAK     — caller owns your state (never)");
        System.out.println("  return new ArrayList<Account>(accounts);  SNAPSHOT — frozen both ways, costs O(n) per call");
        System.out.println("  return unmodifiableList(accounts);        WINDOW   — free, writes throw, reads are LIVE");
        System.out.println("Pick by the promise: reports snapshot, dashboards window, and the leak is a bug,");
        System.out.println("not an option. (Java 9 later adds List.of(...) for truly frozen lists — outside");
        System.out.println("this course's --release 8 world, but you will see it in stage-2-monolith code.)");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
