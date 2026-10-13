package com.evaitcs.advanced.executors;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Demo 5 — SCHEDULING WITH AN EXACT TICK COUNT: latches + self-cancel, never sleep-and-hope.
 * (README factory-tour row "newScheduledThreadPool" and the shutdown walkthrough.)
 *
 * The claim: scheduleAtFixedRate re-queues a task FOREVER until it is cancelled or the
 * pool shuts down. To print an EXACT tick count (3), the task cancels ITS OWN future on
 * the third run: a periodic task never runs concurrently with itself, cancel(false)
 * flips its state to CANCELLED, so the scheduler never re-queues it. No race, no timing
 * assumption beyond a generous latch deadline.
 */
public class ScheduledExecutorDemo {

    static final int PLANNED_TICKS = 3;

    public static void main(String[] args) throws Exception {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        try {
            banner("1. One-shot schedule(): a receipt for work that runs LATER");

            ScheduledFuture<String> once = scheduler.schedule(
                    () -> "audit ping ran on " + Thread.currentThread().getName(),
                    100, TimeUnit.MILLISECONDS);
            System.out.println("step 1: scheduled 100 ms out; isDone() right after scheduling = " + once.isDone());
            System.out.println("step 2: get() blocked until it ran: " + once.get());

            banner("2. scheduleAtFixedRate: EXACTLY " + PLANNED_TICKS + " ticks — the task cancels itself");

            AtomicInteger ticks = new AtomicInteger();
            CountDownLatch plannedTicksDone = new CountDownLatch(PLANNED_TICKS);
            AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();

            ScheduledFuture<?> heartbeat = scheduler.scheduleAtFixedRate(() -> {
                int n = ticks.incrementAndGet();
                System.out.println("step " + n + ": tick " + n + " on " + Thread.currentThread().getName());
                if (n == PLANNED_TICKS) {
                    // Self-cancel from inside the run (initial delay of 50 ms guarantees
                    // self.set() below happened first). Deterministic: the scheduler checks
                    // the cancelled state before re-queueing, so tick 4 cannot exist.
                    ScheduledFuture<?> me = self.get();
                    if (me != null) {
                        me.cancel(false);
                    }
                }
                plannedTicksDone.countDown();
            }, 50, 100, TimeUnit.MILLISECONDS);
            self.set(heartbeat);

            // Generous deadline (5 s) for ~250 ms of planned ticking — the floor exists so a
            // slow machine can never turn this demo into a hang OR a wrong count.
            boolean onTime = plannedTicksDone.await(5, TimeUnit.SECONDS);
            heartbeat.cancel(false);   // belt-and-braces: a no-op if the self-cancel already won
            System.out.println("step 4: latch cleared in time = " + onTime + "; tick count = " + ticks.get() + " (EXACT: cancelled from inside run 3)");
            System.out.println("note: scheduleAtFixedRate measures start-to-start; scheduleWithFixedDelay measures end-to-start.");

            banner("3. shutdownNow(): whatever is still queued never runs");

            scheduler.schedule(() -> System.out.println("never printed — cancelled before it ran"), 10, TimeUnit.SECONDS);
            List<Runnable> dropped = scheduler.shutdownNow();
            boolean terminated = scheduler.awaitTermination(5, TimeUnit.SECONDS);
            System.out.println("step 1: shutdownNow() handed back " + dropped.size() + " queued task(s) that will never run");
            System.out.println("step 2: terminated = " + terminated);
            System.out.println("verdict: a periodic task re-queues FOREVER until cancelled or the pool dies —");
            System.out.println("         latches + cancel + shutdown beat sleep-and-hope, every single time.");
        } finally {
            scheduler.shutdownNow();   // idempotent: guarantees no non-daemon thread outlives the demo
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
