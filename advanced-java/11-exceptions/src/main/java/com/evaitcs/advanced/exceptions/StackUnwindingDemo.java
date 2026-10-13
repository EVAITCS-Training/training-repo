package com.evaitcs.advanced.exceptions;

import java.math.BigDecimal;

/**
 * Demo 2 — STACK UNWINDING: what `throw` actually DOES at runtime.
 * (README "What throw actually does at runtime: stack unwinding" — the module's aha.)
 *
 * The claim: `throw` does not "jump" anywhere. It starts UNWINDING — the JVM pops stack
 * frames, deepest first, discarding each method's local state, until it finds a frame whose
 * try/catch matches the thrown type. If it reaches the bottom with no match, the THREAD dies
 * and the uncaught-exception handler prints the trace.
 *
 * The evidence is the caught exception's StackTraceElement[] — a snapshot, taken at the
 * throw, of exactly the frames that are about to be popped. Print it DEEPEST FIRST and you
 * are literally looking at the unwind path.
 *
 * We use the real domain chain (TransferService.transfer -> authorize -> checkBalance -> throw)
 * so the frames have meaningful names, then catch at the very top.
 */
public class StackUnwindingDemo {

    public static void main(String[] args) {
        banner("1. Walkthrough — what happens the instant `throw` executes");
        System.out.println("step 1: the exception object is built, and fillInStackTrace() snapshots the CURRENT stack.");
        System.out.println("step 2: the JVM stops running the throwing method and begins to UNWIND.");
        System.out.println("step 3: it pops the top frame. Does that frame have a catch matching the thrown type?");
        System.out.println("step 4: if NO, the frame is discarded (locals and all) and the JVM pops the next one up.");
        System.out.println("step 5: if YES, control transfers into that catch block; unwinding stops there.");
        System.out.println("step 6: if the JVM reaches the bottom of the thread with no match, the THREAD terminates");
        System.out.println("        and the thread's uncaught-exception handler prints the trace to stderr.");
        System.out.println("verdict: a stack trace is a PHOTOGRAPH of the frames unwinding is about to pop, deepest first.");

        banner("2. Throw from the deepest frame, catch at the top — then read the unwind path");

        Account checking = new Account("ACC-1001", new BigDecimal("50.00"));
        Account savings = new Account("ACC-2002", new BigDecimal("0.00"));
        try {
            attemptTransfer(checking, savings, new BigDecimal("80.00"));   // 80 > 50: will throw deep inside
            System.out.println("never reached: the transfer cannot succeed");
        } catch (BankingError.InsufficientFunds e) {
            System.out.println("step 1: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("        WHY it happened: checkBalance(), three frames down, found 50.00 < 80.00.");
            System.out.println("step 2: the caught exception's StackTraceElement[] — the frames that were popped, deepest first:");

            StackTraceElement[] trace = e.getStackTrace();
            // Print only the top frames that matter; the tail is demo/JVM plumbing.
            int shown = Math.min(5, trace.length);
            for (int i = 0; i < shown; i++) {
                StackTraceElement frame = trace[i];
                String role = switch (i) {
                    case 0 -> "  <- the THROW site (deepest; popped first)";
                    case 1, 2 -> "  <- popped while unwinding";
                    default -> "  <- the frame whose catch STOPPED the unwinding";
                };
                System.out.println("        frame " + i + ": " + frame.getMethodName()
                        + "  (" + frame.getClassName() + ":" + frame.getLineNumber() + ")" + role);
            }
            System.out.println("step 3: getStackTrace().length = " + trace.length
                    + " (the rest is main/JVM plumbing below the catch).");
            System.out.println("verdict: checkBalance -> authorize -> transfer -> attemptTransfer were all discarded;");
            System.out.println("         the frames you see are the unwind path, and this catch block is where it ended.");
        }

        banner("3. No matching catch anywhere: the THREAD dies (default handler prints the trace)");

        // We run the uncaught throw on a SEPARATE thread so this demo's main thread survives.
        // A custom uncaught-exception handler stands in for the JVM default so we can PRINT A
        // CLEAN LINE instead of dumping a raw trace to stderr — the mechanism is identical.
        Thread doomed = new Thread(() -> {
            throw new IllegalStateException("nothing on this thread catches me");
        }, "doomed-worker");
        doomed.setUncaughtExceptionHandler((thread, error) ->
                System.out.println("step 1: NO catch matched, so thread '" + thread.getName()
                        + "' terminated; its handler saw -> "
                        + error.getClass().getSimpleName() + ": " + error.getMessage()));
        doomed.start();
        try {
            doomed.join();                 // deterministic: wait for the thread to finish dying
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("step 2: doomed.isAlive() after join = " + doomed.isAlive() + "  -> the thread is TERMINATED.");
        System.out.println("        (the stock default handler would have printed the full stack trace to stderr here)");
        System.out.println("verdict: an uncaught exception ends its thread — the trace you see in a crash log is this.");

        banner("4. In one sentence");
        System.out.println("`throw` unwinds the stack, popping frames deepest-first until a matching catch is found;");
        System.out.println("the exception's stack trace is the photograph of that unwind path — and no match means the thread dies.");
    }

    /** One more named frame above the service, so the catch in main() is visibly several frames up. */
    static void attemptTransfer(Account from, Account to, BigDecimal amount) {
        new TransferService().transfer(from, to, amount);   // descends: transfer -> authorize -> checkBalance -> throw
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
