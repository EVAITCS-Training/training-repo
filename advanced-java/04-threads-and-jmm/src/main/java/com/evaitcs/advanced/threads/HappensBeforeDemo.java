package com.evaitcs.advanced.threads;

import java.math.BigDecimal;

/**
 * Demo 4 — HAPPENS-BEFORE, the backbone of the whole memory model.
 * (README sections "join() is an edge" and "The five happens-before rules".)
 *
 * "Happens-before" (HB) is the JMM's one promise: if action A happens-before action B,
 * then A's writes are VISIBLE to B and ordered before it. It is not "earlier on the
 * clock" — it is a guaranteed edge the compiler and CPU may not reorder across.
 *
 * This demo proves the most useful edge live — Thread.join() — then narrates all five
 * rules. The join proof: a worker writes SEVERAL plain (non-volatile) fields; main calls
 * worker.join(); after join returns, main reads every field and sees every write. No
 * volatile, no lock — join() alone is the edge. Everything the worker did happens-before
 * the moment join() returns.
 */
public class HappensBeforeDemo {

    /** A result holder with nothing but PLAIN fields — no volatile, no final, no lock. */
    static final class Report {
        int rowsProcessed;
        String message;
        BigDecimal total;
        boolean complete;
    }

    public static void main(String[] args) throws Exception {
        banner("1. join() is an edge: a worker writes plain fields, main reads them all after join");

        Report report = new Report();
        Account account = new Account("ACC-1027", "Dana", new BigDecimal("100.00"));

        Thread worker = new Thread(() -> {
            report.rowsProcessed = 42;                              // plain write
            report.message = "statement reconciled";                // plain write
            report.total = new BigDecimal("1250.75");               // plain write
            account.deposit(new BigDecimal("250.00"));             // writes balance AND version
            report.complete = true;                                 // plain write, last
        }, "report-writer");

        worker.start();
        worker.join();                     // THE EDGE: everything the worker did happens-before this returns

        System.out.println("step 1: worker wrote 4 plain Report fields + deposited into a plain Account field");
        System.out.println("step 2: main called worker.join() — a happens-before edge");
        System.out.println("step 3: after join, main reads:");
        System.out.println("        report.rowsProcessed = " + report.rowsProcessed);
        System.out.println("        report.message       = " + report.message);
        System.out.println("        report.total         = " + report.total);
        System.out.println("        report.complete      = " + report.complete);
        System.out.println("        account              = " + account);
        System.out.println("verdict: every write crossed with NO volatile and NO lock. join() published them all.");

        banner("2. start() is an edge too: writes BEFORE start() are seen INSIDE the thread");

        final int[] config = {0};          // plain array cell
        config[0] = 99;                     // written on main BEFORE start()
        CountdownHolder seen = new CountdownHolder();
        Thread reader = new Thread(() -> seen.value = config[0], "config-reader"); // reads inside
        reader.start();                     // start() happens-before every action in the new thread
        reader.join();
        System.out.println("step 1: main wrote config[0] = 99 BEFORE calling start()");
        System.out.println("step 2: the new thread read config[0] inside run()");
        System.out.println("step 3: value seen inside the thread = " + seen.value);
        System.out.println("verdict: start() is an edge — everything main did before it is visible inside.");

        banner("3. The five happens-before rules, narrated");
        System.out.println("rule 1 — PROGRAM ORDER: within one thread, each action HB every action after it.");
        System.out.println("         (Proven trivially: a thread sees its own writes in order.)");
        System.out.println("rule 2 — MONITOR LOCK: an unlock HB every subsequent lock of the SAME monitor.");
        System.out.println("         (synchronized blocks publish writes on exit — module 05 proves this live.)");
        System.out.println("rule 3 — VOLATILE: a write to a volatile field HB every subsequent read of it.");
        System.out.println("         (VisibilityDemo section 2 proved this: the spinner always saw the write.)");
        System.out.println("rule 4 — THREAD.START(): start() HB every action in the started thread.");
        System.out.println("         (Proven in section 2 above: config[0]=99 was visible inside.)");
        System.out.println("rule 5 — THREAD.JOIN(): every action in a thread HB the moment join() returns.");
        System.out.println("         (Proven in section 1 above: all plain fields crossed after join.)");
        System.out.println("plus   — TRANSITIVITY: if A HB B and B HB C, then A HB C. Chain the edges and");
        System.out.println("         visibility flows along the chain — that is how real programs stay correct.");

        banner("4. Safe publication: why immutable records are thread-safe for free");
        // A record's components are FINAL. The JMM freezes final fields at the end of the
        // constructor, so any thread that sees the reference sees a fully-built object —
        // even across a plain data race with no other edge. That is "safe publication".
        final Transaction[] handoff = new Transaction[1];
        Thread publisher = new Thread(() ->
                handoff[0] = Transaction.of("T-1040", "Payroll", "2450.00"), "publisher");
        publisher.start();
        publisher.join();                                  // edge for the reference handoff itself
        Transaction t = handoff[0];
        System.out.println("step 1: a worker built a Transaction RECORD and stored it in a plain array slot");
        System.out.println("step 2: main read it back: " + t);
        System.out.println("step 3: its fields are FINAL — the JMM froze them at construction, so they are");
        System.out.println("        fully visible even without the join edge. That is safe publication.");
        System.out.println("verdict: immutable records (Java 21) are inherently thread-safe — no lock, no volatile,");
        System.out.println("         no half-built window. Prefer them for anything shared between threads.");

        banner("5. In one sentence");
        System.out.println("Do not ask 'is my write flushed?' — ask 'is there a happens-before edge to the");
        System.out.println("reader?' join(), start(), volatile, monitor unlock and final fields ARE the edges.");
    }

    /** One plain cell, written by a worker and read after join — start()/join() publish it. */
    static final class CountdownHolder {
        int value;
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
