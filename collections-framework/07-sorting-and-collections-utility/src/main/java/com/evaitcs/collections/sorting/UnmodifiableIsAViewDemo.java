package com.evaitcs.collections.sorting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Demo 4 — unmodifiableList IS A WINDOW, NOT A COPY. THE trap of this module.
 *
 * Collections.unmodifiableList(original) does not build anything new to hold the data.
 * It wraps the ORIGINAL list in a thin shell that forwards every READ (get, size,
 * iterator, contains) and throws UnsupportedOperationException on every WRITE (add, set,
 * remove, clear, sort...). One object, two consequences students keep getting wrong:
 *
 *   1. Through the wrapper: reads work, writes explode. Fine so far — that is the feature.
 *   2. Around the wrapper: whoever kept a reference to the ORIGINAL can still mutate it,
 *      and the "unmodifiable" view SHOWS those changes. It is a live window onto the
 *      original, not a photograph of it.
 *
 * So "unmodifiable" describes the VIEW, never the DATA. If callers can reach the original
 * (you returned it earlier, stored it in a field, captured it in a closure), the guarantee
 * is only as strong as your encapsulation. The fix when you want a frozen SNAPSHOT:
 * copy first, then wrap — Collections.unmodifiableList(new ArrayList<Account>(original)) —
 * and throw the original away. Module 08 turns this into the three-getter encapsulation
 * lesson; java8-features/04-streams-fundamentals has the collect-into-immutable sequel.
 *
 * The same window semantics apply to unmodifiableSet/unmodifiableMap (and every
 * Collections.unmodifiableX). And the SAME wrapper machinery, pointed at a different
 * problem, is Collections.synchronizedList — one-line teaser here, module 09 owns it.
 */
public class UnmodifiableIsAViewDemo {

    public static void main(String[] args) {
        banner("1. Through the wrapper — reads work, every write throws");

        // The bank's internal registry. Some service must hand it out read-only.
        List<Account> internal = Account.sampleAccounts();
        List<Account> readOnly = Collections.unmodifiableList(internal);

        System.out.println("readOnly.get(0) = " + readOnly.get(0) + "   <- reads forward to the original");
        System.out.println("readOnly.size() = " + readOnly.size());
        try {
            readOnly.add(new Account("ACC-3000", "Mallory", new BigDecimal("0.01")));
        } catch (UnsupportedOperationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from readOnly.add(...)");
        }
        try {
            readOnly.set(0, new Account("ACC-0001", "Mallory", new BigDecimal("0.01")));
        } catch (UnsupportedOperationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from readOnly.set(0, ...)");
        }
        try {
            readOnly.remove(0);
        } catch (UnsupportedOperationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from readOnly.remove(0)");
        }
        System.out.println("So far this looks like an immutable list. It is not. Watch the original.");

        banner("2. THE TRAP — mutating the ORIGINAL shows straight through the window");

        System.out.println("before: readOnly.size() = " + readOnly.size());
        internal.add(new Account("ACC-3000", "Mallory", new BigDecimal("0.01")));   // via the ORIGINAL
        System.out.println("internal.add(Mallory) — a list nobody was supposed to change...");
        System.out.println("after : readOnly.size() = " + readOnly.size() + "   <- the 'read-only' view GREW");
        System.out.println("readOnly.contains(Mallory) = "
                + readOnly.contains(new Account("ACC-3000", "anyone", BigDecimal.ZERO)) + "   (equals by id, module 01)");
        System.out.println("unmodifiable protects the VIEW, not the DATA. Anyone holding the original");
        System.out.println("walks right around the wrapper. The window is LIVE.");

        banner("3. Want a real snapshot? Copy first, THEN wrap");

        List<Account> frozen = Collections.unmodifiableList(new ArrayList<Account>(internal));
        internal.add(new Account("ACC-4000", "Late Addition", new BigDecimal("7.00")));
        System.out.println("internal grew again (size " + internal.size() + "), but frozen.size() = " + frozen.size()
                + "   <- the copy cut the wire");
        System.out.println("unmodifiableList(new ArrayList<Account>(internal)) — snapshot idiom. Module 08 makes");
        System.out.println("this a three-getter API lesson (leak vs copy vs window).");

        banner("4. The whole family — Set, Map, and the synchronized teaser");

        Set<Account> readOnlySet = Collections.unmodifiableSet(new LinkedHashSet<Account>(Account.sampleAccounts()));
        Map<String, Account> readOnlyMap = Collections.unmodifiableMap(new HashMap<String, Account>());
        System.out.println("unmodifiableSet / unmodifiableMap: SAME window semantics, same UnsupportedOperationException.");
        try {
            readOnlySet.clear();
        } catch (UnsupportedOperationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from readOnlySet.clear()");
        }
        System.out.println("readOnlyMap.size() = " + readOnlyMap.size() + " (an empty map can still be handed out safely)");
        System.out.println("And one sibling you will meet in module 09: Collections.synchronizedList(list) is the");
        System.out.println("SAME wrapper machinery, but every forwarded call goes through a lock — a window with a guard.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
