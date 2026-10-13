package com.evaitcs.collections.threadsafety;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Demo 4 — FIX LEVEL (d) DOES NOT EXIST: compound actions stay broken under EVERY wrapper.
 *
 * A "check-then-act" is a SEQUENCE with a business decision in the middle:
 *
 *     if (!accounts.containsKey(id)) {          // CHECK  — one atomic call
 *         accounts.put(id, withMyDeposit);      // ACT    — another atomic call
 *     }                                         // the GAP BETWEEN THEM is nobody's
 *
 * Collections.synchronizedMap makes each call atomic. The gap is not a call. Two threads
 * can both pass the check while the map is still empty, then act one after the other — and
 * the second write lands on top of the first. Deposit gone. This is the LOST UPDATE, the
 * single most expensive bug in concurrent business code, and no lock granularity below
 * "one lock around the whole sequence" prevents it — not the wrappers, not Vector, not
 * Hashtable (demo 5 proves Vector falls to the exact same interleaving).
 *
 * DETERMINISM: instead of racing real threads and HOPING for the bad interleaving, this
 * demo PLAYS the two threads step by step, in the doomed order, on one thread. The
 * interleaving IS the lesson; scripting it makes the bug reproduce every run. This is also
 * how you debug a real one — write the interleaving down, then play it.
 *
 * Teaser, cashed in module 10: java.util.concurrent deletes the gap by fusing check and act
 * into ONE call — putIfAbsent, computeIfAbsent, compute, merge, replace(k, old, new).
 */
public class CheckThenActDemo {

    public static void main(String[] args) {
        banner("1. The setup — two tellers registering the SAME new account, concurrently");

        final Map<String, Account> accounts = Collections.synchronizedMap(new HashMap<String, Account>());
        System.out.println("the rule both tellers follow (every call is wrapper-synchronized — looks safe!):");
        System.out.println("    if (!accounts.containsKey(id)) {                       // CHECK");
        System.out.println("        accounts.put(id, new Account(id, owner, myDeposit)); // ACT");
        System.out.println("    }");
        System.out.println("Dana walks into window A with 100.00 and window B with 250.00 — same new");
        System.out.println("account id ACC-1027 (equals-by-id, core-java 08: both tellers mean ONE account).");

        banner("2. The doomed interleaving, played step by step (thread A, thread B, one thread's script)");

        // Teller A arrives first and checks. The wrapper's lock is taken around containsKey
        // ...and RELEASED on return. That release is the whole tragedy.
        boolean aSawKey = accounts.containsKey("ACC-1027");
        System.out.println("A: containsKey(ACC-1027) -> " + aSawKey + "    (lock taken, answered, RELEASED)");
        System.out.println("   *** A is descheduled HERE. The gap. Nothing holds anything. ***");

        // Teller B walks into the same open door — and sees the same empty map.
        boolean bSawKey = accounts.containsKey("ACC-1027");
        System.out.println("B: containsKey(ACC-1027) -> " + bSawKey + "    (A has not written yet — B's answer is ALSO 'absent')");

        // A resumes and acts on its — now stale — answer.
        accounts.put("ACC-1027", new Account("ACC-1027", "Dana", new BigDecimal("100.00")));
        System.out.println("A: put(ACC-1027, Dana 100.00)    (acting on a check that is already history)");

        // B acts on ITS stale answer too. containsKey said 'absent', so B runs the CREATE
        // branch — with only B's own deposit — straight over the top of A's write.
        accounts.put("ACC-1027", new Account("ACC-1027", "Dana", new BigDecimal("250.00")));
        System.out.println("B: put(ACC-1027, Dana 250.00)    (B read balance 0.00 before A's put — B REPLACES A)");

        banner("3. The aftermath — the lost update");

        System.out.println("final balance: " + accounts.get("ACC-1027").getBalance());
        System.out.println("A deposited 100.00, B deposited 250.00 — the account should show 350.00.");
        System.out.println("It shows 250.00. A's money is GONE — and not one method call was 'unsafe':");
        System.out.println("containsKey was atomic. put was atomic. The COMPOUND ACTION was not.");
        System.out.println("verdict: check-then-act on a synchronized wrapper = lost update, every run, BY CONSTRUCTION.");

        banner("4. Fix 1, live — ONE lock around the WHOLE compound action");

        Map<String, Account> ledger = Collections.synchronizedMap(new HashMap<String, Account>());
        String id = "ACC-1027";

        // Teller A, complete action under one lock — check AND act, with no gap between:
        synchronized (ledger) {
            if (!ledger.containsKey(id)) {
                ledger.put(id, new Account(id, "Dana", new BigDecimal("100.00")));
                System.out.println("A: absent -> created with 100.00 (check AND act inside the lock)");
            }
        }
        // Teller B, same lock — and because the check happens INSIDE it, B finally SEES the
        // truth: the account exists, so B runs the other branch and ADDS to the balance it
        // re-reads under the lock. This is the part scripting-by-wrapper can never do: the
        // stale answer of banner 2 becomes impossible.
        synchronized (ledger) {
            if (!ledger.containsKey(id)) {
                System.out.println("B: absent -> would create (cannot happen now)");
            } else {
                Account existing = ledger.get(id);
                ledger.put(id, new Account(id, existing.getOwner(),
                        existing.getBalance().add(new BigDecimal("250.00"))));
                System.out.println("B: present -> re-read " + existing.getBalance()
                        + " INSIDE the lock, added 250.00");
            }
        }
        System.out.println("final balance: " + ledger.get(id).getBalance() + " — no lost update, every run.");
        System.out.println("The price, printed for the record: while A holds the lock, EVERY other");
        System.out.println("operation on this map waits — readers included. One global lock serializes");
        System.out.println("the world. Correct, and slow by design.");

        banner("5. Fix 2 — delete the gap instead of guarding it (module 10)");

        System.out.println("java.util.concurrent fuses check and act into ONE atomic call:");
        System.out.println("    accounts.putIfAbsent(id, newAccount)   <- returns null if YOU won,");
        System.out.println("                                              the current value if someone beat you");
        System.out.println("    counts.computeIfAbsent(key, factory)   <- build-once, installed atomically");
        System.out.println("    accounts.replace(id, oldVal, newVal)   <- write only if nothing changed since the read");
        System.out.println("No gap to guard, no global lock to pay. That is the next module — the top of");
        System.out.println("this module's fix ladder is a door, not a ceiling.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
