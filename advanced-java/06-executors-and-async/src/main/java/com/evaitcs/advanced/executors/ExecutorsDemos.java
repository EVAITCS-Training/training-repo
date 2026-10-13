package com.evaitcs.advanced.executors;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 06-executors-and-async compile exec:java
 *
 * The order is the argument: first WHY pools exist and what every factory hides
 * (ThreadPoolExecutor anatomy, reuse proven by thread names, rejection provoked with
 * latches), then the Future receipt (get blocks, exceptions captured, cancel cooperates),
 * then chaining receipts instead of blocking on them (the SecureBank report pipeline
 * with a planted failure), then Java 21's answer to "why pool at all" (10,000 virtual
 * threads), and finally time-based scheduling with an EXACT tick count.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 06-executors-and-async compile exec:java "-Dexec.mainClass=com.evaitcs.advanced.executors.CompletableFuturePipelineDemo"
 */
public class ExecutorsDemos {

    public static void main(String[] args) throws Exception {
        section("PoolAnatomyDemo — pools are reuse + queue + admission policy; rejection caught red-handed");
        PoolAnatomyDemo.main(args);

        section("FutureBasicsDemo — Runnable vs Callable, get() blocks, cancel(true) is a request");
        FutureBasicsDemo.main(args);

        section("CompletableFuturePipelineDemo — loadAccount -> enrich -> riskCheck -> renderReport, one planted failure");
        CompletableFuturePipelineDemo.main(args);

        section("VirtualThreadExecutorDemo — 10,000 blocking tasks, exact count, thread-per-task is cheap again");
        VirtualThreadExecutorDemo.main(args);

        section("ScheduledExecutorDemo — scheduleAtFixedRate with an EXACT tick count: latch + self-cancel + shutdown");
        ScheduledExecutorDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
