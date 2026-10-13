package com.evaitcs.advanced.patterns;

import com.evaitcs.advanced.patterns.MoneyValueObjectDemo.Money;

/**
 * Demo B1 — THE CONCURRENCY FAMILY: seven patterns, and where you already met each.
 * (README section "Concurrency patterns: the recap with depth".)
 *
 * Modules 04-08 taught the MECHANISMS (volatile, locks, pools, selectors). This demo
 * names the recurring SHAPES those mechanisms serve — because in a design review,
 * "use a producer-consumer queue" says in four words what a paragraph of mechanism
 * cannot. Every row points back at the module where you already ran it.
 *
 * The live scene (banner 2) is deliberately SINGLE-THREADED: thread confinement and
 * immutability are patterns that REMOVE concurrency, so the safest possible scene
 * demonstrates them best. The shared-mutable anti-scene (banner 3) is narrated with
 * module 04's real observed numbers, never re-run — races are for module 04, where
 * they have latches and joins.
 */
public class ConcurrencyPatternMapDemo {

    /** An account state that CANNOT change: a record of id + immutable Money. */
    record AccountState(String id, Money balance) { }

    /** The "message" of the scene: also immutable, also safe to hand between threads. */
    record TransferResult(AccountState from, AccountState to) { }

    /**
     * A transfer as a PURE FUNCTION: nothing shared is mutated; the next states are
     * RETURNED. Two confined threads could each own a state and exchange messages
     * like this with zero locks — the java.util.concurrent shape module 06 fed.
     */
    static TransferResult transfer(AccountState from, AccountState to, Money amount) {
        return new TransferResult(
                new AccountState(from.id(), from.balance().subtract(amount)),
                new AccountState(to.id(), to.balance().add(amount)));
    }

    public static void main(String[] args) {
        banner("1. The concurrency pattern map — seven shapes you already ran");

        row("Producer-consumer", "a queue decouples the rate of producing from consuming",
                "BlockingQueue (collections-framework 06 + 10); every pool in module 06");
        row("Immutability by default", "records as messages: nothing to corrupt, nothing to lock",
                "module 04 (safe publication), module 01 (records); today's scene below");
        row("Thread confinement", "data seen by exactly one thread needs no synchronization",
                "module 04 (why confined state is the cheap win); today's scene below");
        row("Worker pool", "rent threads instead of selling them one by one",
                "module 06 (Executors); virtual threads made one-per-task cheap again");
        row("Future / promise", "a handle now, the result later, chainable",
                "module 06 (CompletableFuture pipelines)");
        row("Reactor", "one thread, many connections, events instead of blocking",
                "module 08 (the Selector IS a reactor; Netty/Tomcat NIO run on it)");
        row("Fork-join divide-and-conquer", "split the work, solve halves, combine",
                "java8-features 07 (parallel streams); module 06 (ForkJoinPool underneath)");

        banner("2. Live scene: confined accounts + immutable Money = no locks in sight");

        AccountState dana = new AccountState("ACC-1", Money.of("500.00"));
        AccountState ana = new AccountState("ACC-2", Money.of("100.00"));
        System.out.println("step 1: before  -> dana = " + dana + ", ana = " + ana);
        TransferResult after = transfer(dana, ana, Money.of("250.00"));
        System.out.println("step 2: transfer(dana, ana, 250.00) RETURNED new states; it mutated nothing");
        System.out.println("step 3: after   -> dana = " + after.from() + ", ana = " + after.to());
        System.out.println("step 4: the OLD states are unchanged and still usable: dana = " + dana.balance().amount().toPlainString());
        System.out.println("verdict: nothing shared, nothing mutable — modules 04-05's entire toolbox");
        System.out.println("         (volatile, monitors, CAS) is UNNEEDED for state that never changes.");

        banner("3. The anti-scene, narrated: shared mutable state (module 04's live numbers)");

        System.out.println("scene: two threads each do 50,000 balance++ on ONE shared account, no lock.");
        System.out.println("module 04 observed: expected 100000, actual 73412 -> updates LOST.");
        System.out.println("why: read-modify-write is three steps; threads interleaved between them.");
        System.out.println("the fixes, in order of preference:");
        System.out.println("  1. do not share (today's scene: immutable states, confined threads);");
        System.out.println("  2. confine it (one owner thread mutates; others send messages);");
        System.out.println("  3. serialize access (module 05: locks, or atomics for single cells).");
        System.out.println("verdict: shared MUTABLE state is the root anti-pattern — patterns 1-3 above");
        System.out.println("         all exist to help you avoid it, not to make it safe to sprawl.");

        banner("4. In one sentence");
        System.out.println("Every concurrency pattern is an answer to one question: who is allowed to");
        System.out.println("touch this state, and when — queues, pools and reactors just organize the answer.");
    }

    /** One map row: pattern | what it is | where you already saw it. */
    private static void row(String pattern, String what, String where) {
        System.out.printf("  %-28s | %s%n", pattern, what);
        System.out.printf("  %-28s | seen in: %s%n", "", where);
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
