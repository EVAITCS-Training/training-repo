package com.evaitcs.advanced.executors;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Demo 2 — RUNNABLE vs CALLABLE, AND THE Future RECEIPT.
 * (README section "Runnable vs Callable, and the Future receipt".)
 *
 * The claim: a submitted task hands back a Future — a receipt whose get() BLOCKS until
 * the result exists, whose exceptions are CAPTURED (they surface at get(), wrapped in
 * ExecutionException), and whose cancel(true) is an interrupt REQUEST that only
 * cooperative tasks honor (module 04's rule, enforced live here).
 *
 * Determinism: every outcome is joined (get / awaitTermination) before it is printed;
 * cancel targets a latch-started task, so "running" is a proven state, not a guess.
 */
public class FutureBasicsDemo {

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            banner("1. Runnable vs Callable: no result vs a result");

            // Runnable = "do this", returns nothing. Callable<V> = "compute this", returns V.
            // submit(Callable) is why Future<V> is generic: the receipt knows the result type.
            Future<?> fromRunnable = pool.submit(() ->
                    System.out.println("step 1: Runnable ran on " + Thread.currentThread().getName() + " — nothing to return"));
            fromRunnable.get();

            List<Transaction> statement = List.of(
                    new Transaction("T-1", "Payroll", new BigDecimal("2450.00")),
                    new Transaction("T-2", "Rent", new BigDecimal("-1200.00")));
            Future<BigDecimal> fromCallable = pool.submit(() -> sumOf(statement));
            System.out.println("step 2: Callable submitted; receipt = Future<BigDecimal>");

            banner("2. get() BLOCKS until the result exists; isDone tells you when");

            System.out.println("step 1: isDone() BEFORE get() = " + fromCallable.isDone() + " (may be false — get() will wait)");
            BigDecimal total = fromCallable.get();   // parks THIS thread until the worker finishes
            System.out.println("step 2: get() returned " + total.toPlainString() + " — the calling thread blocked, the worker computed");
            System.out.println("step 3: isDone() AFTER get()  = " + fromCallable.isDone());
            System.out.println("verdict: get() is the blocking handshake; get(timeout, unit) adds a fuse (TimeoutException).");

            banner("3. Exceptions are CAPTURED into the Future — they surface at get()");

            Future<BigDecimal> doomed = pool.submit(() -> {
                throw new IllegalStateException("risk service unreachable");
            });
            System.out.println("step 1: a throwing Callable was submitted — nothing exploded YET (this is trap 2)");
            try {
                doomed.get();
            } catch (ExecutionException e) {
                System.out.println("step 2: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.out.println("step 3: the REAL failure is the cause -> " + e.getCause());
                System.out.println("WHY: submit() routes exceptions into the receipt; execute() would instead have sent");
                System.out.println("     it to the thread's UncaughtExceptionHandler. A forgotten Future hides the crash.");
            }

            banner("4. cancel(true): an interrupt REQUEST — cooperation decides (module 04)");

            CountDownLatch started = new CountDownLatch(1);
            Future<String> longTask = pool.submit(() -> {
                started.countDown();
                try {
                    // A "long" task built from interruptible waiting — cancel(true) can stop it
                    // ONLY because await() throws on interrupt. A loop that ignores the flag
                    // (while(true) { compute(); }) would run to completion regardless.
                    new CountDownLatch(1).await();
                    return "never returned";
                } catch (InterruptedException e) {
                    return "stopped cooperatively: felt the interrupt, cleaned up, returned";
                }
            });
            started.await();                          // start gun: the task is provably RUNNING
            boolean accepted = longTask.cancel(true); // true = mayInterruptIfRunning
            System.out.println("step 1: cancel(true) accepted = " + accepted + ", isCancelled() = " + longTask.isCancelled());
            try {
                longTask.get();
            } catch (CancellationException e) {
                System.out.println("step 2: CAUGHT -> " + e.getClass().getSimpleName() + ": get() on a cancelled Future refuses to return a value");
                System.out.println("WHY: once cancelled, the receipt is void — even though the task itself saw the");
                System.out.println("     interrupt and returned cleanly. cancel(false) on a QUEUED task simply un-queues it.");
            }
            System.out.println("verdict: cancel is a request, not a kill — module 04's interrupt cooperation, verbatim.");
        } finally {
            pool.shutdown();
            System.out.println();
            System.out.println("pool terminated cleanly = " + pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    /** The Callable's whole job: sum a statement, money as BigDecimal (repo convention). */
    static BigDecimal sumOf(List<Transaction> statement) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction t : statement) {
            total = total.add(t.amount());
        }
        return total.setScale(2);
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
