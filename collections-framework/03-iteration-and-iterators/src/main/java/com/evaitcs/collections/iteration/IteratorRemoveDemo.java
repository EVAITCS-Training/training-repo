package com.evaitcs.collections.iteration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Demo 3 — iterator.remove(): THE ONLY SAFE REMOVAL DURING A WALK.
 *
 * The mission: an audit pass — remove every transaction below a threshold from the statement,
 * in ONE walk, without detonating the iterator.
 *
 * WHY it.remove() is safe and list.remove(...) is not (Demo 4): the list counts structural
 * modifications in a field called modCount; the iterator snapshots it at birth as
 * expectedModCount and re-checks it in every next(). it.remove() deletes the element the LAST
 * next() returned AND syncs expectedModCount with the new modCount — the iterator is in on the
 * secret. (The NAME modCount and this one-paragraph summary belong here; the full machinery,
 * line by line, is module 09.)
 *
 * The contract's fine print, also demonstrated below: remove() is legal exactly ONCE PER
 * next() — before any next(), or twice in a row, it is an IllegalStateException (the iterator's
 * OWN contract, not the list's fail-fast alarm — two different bugs, two different exceptions).
 *
 * Amount checks use BigDecimal.compareTo, never equals: "3.75" and "3.750" are equals()-different
 * but compareTo-equal (the consistency lesson from core-java 10).
 */
public class IteratorRemoveDemo {

    /** The audit rule: anything strictly below this amount must go. */
    private static final BigDecimal AUDIT_MINIMUM = new BigDecimal("100.00");

    public static void main(String[] args) {
        banner("1. The mission — drop every row below " + AUDIT_MINIMUM.toPlainString());

        List<Transaction> statement = Transaction.sampleStatement();
        for (Transaction t : statement) {
            String flag = t.getAmount().compareTo(AUDIT_MINIMUM) < 0 ? "   <- below the audit minimum" : "";
            System.out.println("  " + t + flag);
        }

        banner("2. The one safe in-loop removal — it.remove()");

        int removed = 0;
        Iterator<Transaction> it = statement.iterator();
        while (it.hasNext()) {
            Transaction t = it.next();
            if (t.getAmount().compareTo(AUDIT_MINIMUM) < 0) {
                it.remove();     // deletes what the LAST next() returned — and syncs expectedModCount
                removed++;
                System.out.println("  it.remove() dropped " + t.getId() + " — the iterator was told, so the walk survives");
            }
        }
        System.out.println("removed " + removed + " of 6 rows; survivors: " + statement);

        banner("3. The fine print — remove() is legal exactly once per next()");

        Iterator<Transaction> strict = statement.iterator();
        try {
            strict.remove();                                   // next() was never called — nothing is "last returned"
        } catch (IllegalStateException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " on remove() BEFORE next() — there is no last-returned element");
        }
        strict.next();
        strict.remove();
        try {
            strict.remove();                                   // a second remove() for the same next()
        } catch (IllegalStateException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " on remove() TWICE per next() — lastRet was already reset");
        }
        System.out.println("Note the exception TYPE: IllegalStateException, not ConcurrentModificationException —");
        System.out.println("the iterator's own contract, not the list's fail-fast alarm. Two different bugs, two different exceptions.");

        banner("4. The always-safe alternative — rebuild into a new list");

        List<Transaction> fresh = Transaction.sampleStatement();
        List<Transaction> kept = new ArrayList<Transaction>();
        for (Transaction t : fresh) {                          // for-each is 100% safe here: fresh is only READ
            if (t.getAmount().compareTo(AUDIT_MINIMUM) >= 0) {
                kept.add(t);
            }
        }
        System.out.println("kept: " + kept);
        System.out.println("Costs one extra list; can never throw; reads exactly like the requirement.");
        System.out.println("Java 8 mechanizes this very loop: list.removeIf(...) does the in-place removal internally,");
        System.out.println("and stream().filter(...).collect(...) builds 'kept' — java8-features 01 and 04.");
        System.out.println("(Grouping the kept rows by category: Collectors.groupingBy — java8-features 06.)");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
