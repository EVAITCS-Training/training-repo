package com.evaitcs.collections.threadsafety;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Demo 2 — FAILURE MODE #2: ConcurrentModificationException, MECHANICALLY.
 *
 * Module 03 taught the RULE ("the only safe remove during iteration is iterator.remove()")
 * and showed the explosion. This demo opens the hood and shows the MACHINE that produces it
 * — three moving parts, one int wide:
 *
 *   1. every java.util collection counts its STRUCTURAL modifications in a field, modCount
 *      (add, remove, clear, put-of-a-new-key... — anything that changes the SHAPE);
 *   2. iterator() copies that count into the iterator as expectedModCount — a snapshot that
 *      says "this is the shape I started walking";
 *   3. next() and remove() re-compare before every step (checkForComodification) and throw
 *      ConcurrentModificationException on mismatch.
 *
 * set() REPLACES a slot without changing the shape — no bump, no CME (banner 5).
 *
 * All of this is SINGLE-THREADED on purpose. CME does not need a second thread: the
 * "concurrent" modification it reports is any mutation from any source while an iterator is
 * live — most often your own loop body. And the detector is BEST-EFFORT: the javadoc's own
 * words are that fail-fast behavior "should be used only to detect bugs", because banner 4
 * proves a removal it never notices. Detect, not prevent — and not even always detect.
 *
 * Module 10 teaser: copy-on-write and weakly-consistent iterators are FAIL-SAFE — they walk
 * a snapshot or tolerate change, and can never throw CME at all.
 */
public class ModCountAnatomyDemo {

    public static void main(String[] args) {
        banner("1. The machinery — modCount vs expectedModCount, narrated");

        System.out.println("Inside every java.util collection (ArrayList shown; HashMap, Vector — all of them):");
        System.out.println("    int modCount;                       // bumped by EVERY structural modification");
        System.out.println("    add(e)      -> modCount++           // the shape changed");
        System.out.println("    remove(...) -> modCount++           // the shape changed");
        System.out.println("    set(i, e)   -> (nothing)            // a slot was REFILLED — same shape");
        System.out.println("    iterator()  -> expectedModCount = modCount;   // the iterator's snapshot");
        System.out.println("    next()/remove() -> if (modCount != expectedModCount)");
        System.out.println("                           throw new ConcurrentModificationException();");
        System.out.println("That comparison has a name in the source: checkForComodification(). Watch for it");
        System.out.println("in the stack trace below — the exception points at its own machinery.");

        banner("2. The explosion — list.remove() inside a for-each (module 03's rule, explained)");

        List<Transaction> transactions = Transaction.sampleTransactions();   // 4 items, "Reversed" at index 1
        System.out.println("before: " + idsOf(transactions));
        System.out.println("the loop body calls transactions.remove(tx) — which bumps modCount behind the");
        System.out.println("iterator's back. The iterator's snapshot (expectedModCount) is now stale:");
        try {
            for (Transaction tx : transactions) {
                if (tx.getDescription().startsWith("Reversed")) {
                    transactions.remove(tx);      // structural! modCount++ ... iterator not informed
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getName());
            StackTraceElement top = e.getStackTrace()[0];
            System.out.println("thrown from: " + top.getClassName() + "." + top.getMethodName()
                    + "  <- the modCount comparison itself, one frame from the crime scene");
        }
        System.out.println("ONE thread did all of this. CME is not 'threading happened' — it is 'the shape");
        System.out.println("changed under a LIVE iterator', and the for-each body counts as someone else.");

        banner("3. The sanctioned way — iterator.remove() keeps BOTH counters in sync");

        List<Transaction> safe = Transaction.sampleTransactions();
        Iterator<Transaction> it = safe.iterator();
        while (it.hasNext()) {
            Transaction tx = it.next();
            if (tx.getDescription().startsWith("Reversed")) {
                it.remove();                      // removes the element next() just returned, THEN
            }                                     // expectedModCount = modCount — snapshot refreshed
        }
        System.out.println("after : " + idsOf(safe) + "  <- Reversed gone, walk completed, no exception, every run.");
        System.out.println("iterator.remove() is the ONLY removal that updates the iterator's own snapshot.");
        System.out.println("(java8-features 01 replaces this whole loop with safe.removeIf(...) — which is");
        System.out.println(" iterator.remove() in a trench coat, with the same fail-fast semantics.)");

        banner("4. The escape — fail-fast is BEST-EFFORT, and here is the proof");

        // THREE items this time, target in the MIDDLE — the removal becomes the last thing
        // the iterator can reach, and the detector never fires. Position is everything.
        List<Transaction> shortList = new ArrayList<Transaction>();
        shortList.add(new Transaction("T-1001", "Salary", new java.math.BigDecimal("3100.00"), 1700000000000L));
        shortList.add(new Transaction("T-1002", "Reversed transfer", new java.math.BigDecimal("45.50"), 1700003600000L));
        shortList.add(new Transaction("T-1004", "Interest", new java.math.BigDecimal("12.34"), 1700010800000L));

        List<String> visited = new ArrayList<String>();
        for (Transaction tx : shortList) {
            visited.add(tx.getId());
            if (tx.getDescription().startsWith("Reversed")) {
                shortList.remove(tx);             // structural! modCount bumped! ...and NOTHING happens.
            }
        }
        System.out.println("visited  : " + visited + "   <- the loop ended EARLY: T-1004 was never visited");
        System.out.println("after    : " + idsOf(shortList) + "   <- the removal DID happen, silently");
        System.out.println("No exception. WHY: hasNext() is just `cursor != size` — it never looks at modCount.");
        System.out.println("Removing the second-to-last element leaves cursor == size, so hasNext() says");
        System.out.println("'done' — and next(), the ONLY place the modCount check lives, never runs.");
        System.out.println("(Remove the actual LAST element and cursor ends up PAST size: hasNext() stays");
        System.out.println(" true, next() runs the check, and you get the CME from banner 2 after all.)");
        System.out.println("The javadoc's own words: fail-fast behavior 'should be used only to detect bugs'.");
        System.out.println("It DETECTS — usually. It never PREVENTS. Never build logic on CME firing.");

        banner("5. set() mid-iteration — the non-structural change that trips nothing");

        List<Transaction> audited = Transaction.sampleTransactions();
        ListIterator<Transaction> lit = audited.listIterator();
        while (lit.hasNext()) {
            Transaction tx = lit.next();
            if (tx.getId().equals("T-1004")) {
                // set() replaces the element next() just returned. The list's SHAPE is
                // untouched — same slots, same size — so modCount does not move, so the
                // iterator's comparison still passes. (list.set(i, e) by index is equally
                // invisible; ListIterator.set is just the tidy way to say it.)
                lit.set(new Transaction(tx.getId(), tx.getDescription() + " (audited)",
                        tx.getAmount(), tx.getTimestampMillis()));
            }
        }
        System.out.println("after    : " + audited.get(3) + "   <- replaced mid-walk");
        System.out.println("No CME — replacing is not restructuring. STRUCTURAL means the shape changed:");
        System.out.println("add/remove/clear bump modCount; set never does. That one distinction explains");
        System.out.println("every 'why did THIS loop explode but that one didn't' question in the wild.");
    }

    /** ids only — the loop output stays readable from the back of the room. */
    static List<String> idsOf(List<Transaction> transactions) {
        List<String> ids = new ArrayList<String>();
        for (Transaction tx : transactions) {
            ids.add(tx.getId());
        }
        return ids;
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
