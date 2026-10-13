package com.evaitcs.advanced.locks;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Demo 6 — ABA: when "unchanged" is a lie, and the stamp that catches it.
 * (README section "ABA: the scar".)
 *
 * CAS asks one question: "is the value STILL what I read?" If yes, it swaps. But "still A"
 * cannot tell the difference between "nothing happened" and "A -> B -> A happened." When a
 * value returns to A after a detour, a stale CAS succeeds on a world that MOVED. That is ABA.
 *
 * DETERMINISM: like collections-framework 09's scripted interleavings, this demo PLAYS both
 * workers step by step on ONE thread, in the doomed order. Racing real threads and HOPING
 * for A->B->A would be flaky; scripting it makes the scar appear every run — and writing the
 * interleaving down is exactly how you debug a real one.
 *
 * The fix: AtomicStampedReference pairs the value with a VERSION STAMP that increments on
 * every write. A stale CAS carries the old stamp, so it is refused even though the value
 * matches. A database @Version column (stage-2 optimistic locking) is this same idea in SQL.
 */
public class AbaDemo {

    public static void main(String[] args) {
        banner("1. The setup — a payment sits in status PENDING");
        System.out.println("A payment worker follows the safe-looking CAS rule:");
        System.out.println("    if status is STILL 'PENDING', flip it to 'APPROVED'.");
        System.out.println("Meanwhile a fraud service may REJECT and then RESET the same payment.");

        banner("2. The doomed interleaving, played step by step (AtomicReference)");
        AtomicReference<String> status = new AtomicReference<>("PENDING");
        System.out.println("step 1: worker reads status = " + status.get() + " and plans CAS(PENDING -> APPROVED)");
        String seen = status.get();                        // worker captures "PENDING"

        System.out.println("step 2: *** worker is descheduled HERE. It holds no lock; nothing is frozen. ***");
        System.out.println("step 3: fraud service sets PENDING -> REJECTED   (status = "
                + swap(status, "REJECTED") + ")");
        System.out.println("step 4: a retry resets   REJECTED -> PENDING   (status = "
                + swap(status, "PENDING") + ")");
        System.out.println("        The value is 'PENDING' again — but a REJECTION happened in between.");

        banner("3. The scar — the stale CAS SUCCEEDS on a world that moved");
        boolean approved = status.compareAndSet(seen, "APPROVED");   // expects "PENDING": it IS "PENDING"
        System.out.println("step 5: worker resumes: CAS(PENDING -> APPROVED) -> " + approved + " (status = "
                + status.get() + ")");
        System.out.println("verdict: the CAS SUCCEEDED because the value matched. It cannot see that the");
        System.out.println("         payment was briefly REJECTED. The worker approved something it should");
        System.out.println("         have re-examined. That is the ABA scar: A -> B -> A fools a value-only CAS.");

        banner("4. The fix — AtomicStampedReference: value AND version must both match");
        AtomicStampedReference<String> stamped = new AtomicStampedReference<>("PENDING", 0);
        int[] stampBox = new int[1];
        String seenStamped = stamped.get(stampBox);        // captures value AND stamp
        int seenStamp = stampBox[0];
        System.out.println("step 1: worker reads (value=" + seenStamped + ", stamp=" + seenStamp + ")");

        stamped.set("REJECTED", 1);                        // fraud service, stamp advances
        System.out.println("step 2: fraud service sets REJECTED with stamp 1");
        stamped.set("PENDING", 2);                         // retry, stamp advances again
        System.out.println("step 3: retry sets PENDING with stamp 2 (value is 'PENDING' again, stamp is NOT 0)");

        boolean staleApproved = stamped.compareAndSet(seenStamped, "APPROVED", seenStamp, 3);
        System.out.println("step 4: worker CAS(value PENDING->APPROVED, expectedStamp " + seenStamp
                + ") -> " + staleApproved);
        System.out.println("verdict: REFUSED. The value matched but the STAMP had moved 0 -> 2, so the stale");
        System.out.println("         write was rejected. The worker must re-read and re-decide. ABA defeated.");

        banner("5. A FRESH stamp is accepted (the fix does not block legitimate writes)");
        int[] freshBox = new int[1];
        String freshValue = stamped.get(freshBox);         // re-read: (PENDING, 2)
        boolean freshApproved = stamped.compareAndSet(freshValue, "APPROVED", freshBox[0], freshBox[0] + 1);
        System.out.println("step 1: worker re-reads (value=" + freshValue + ", stamp=" + freshBox[0] + ")");
        System.out.println("step 2: CAS with the FRESH stamp -> " + freshApproved + " (status = "
                + stamped.getReference() + ", stamp now " + stamped.getStamp() + ")");
        System.out.println("verdict: a current (value, stamp) pair is accepted — the stamp only rejects STALE writers.");

        banner("6. Where you already know this trick");
        System.out.println("A database @Version column IS a stamp: UPDATE ... SET v=4 WHERE id=? AND version=3");
        System.out.println("refuses a stale row exactly like step 4 refused the worker. (stage-2 optimistic");
        System.out.println("locking = this demo, in SQL.) In plain lock-based Java, ABA is rare because a lock");
        System.out.println("freezes the world between check and act — ABA lives in LOCK-FREE, value-only CAS code.");

        banner("7. In one sentence");
        System.out.println("CAS proves a value is unchanged, not that nothing happened; add a version stamp and");
        System.out.println("'unchanged' finally means 'no one touched it' — the whole ABA class of bugs disappears.");
    }

    /** Small helper so the scripted steps print the value they just set. */
    private static String swap(AtomicReference<String> ref, String next) {
        ref.set(next);
        return ref.get();
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
