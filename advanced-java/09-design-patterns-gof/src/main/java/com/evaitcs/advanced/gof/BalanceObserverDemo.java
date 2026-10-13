package com.evaitcs.advanced.gof;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo 8 — OBSERVER: the subject keeps a listener list and publishes events.
 * (README section "Observer: from Swing listeners to Kafka".)
 *
 * Problem: when a balance changes, the statement printer, the fraud watcher and the
 * SMS alerter all want to know — but Account must not KNOW those three classes.
 * Solution: Account holds a list of BalanceListeners and notifies them; who listens
 * is decided elsewhere, at runtime.
 *
 * You already met it: Swing's ActionListener, CompletableFuture's whenComplete
 * callbacks (module 06), and java.util.Observer — deprecated in Java 9 and removed
 * thereafter, because a stringly-typed update(Object) could not carry typed events.
 * The pattern did not die: stage-5 runs it across process boundaries as Kafka events.
 *
 * DETERMINISM: everything here is single-threaded — notification order is exactly
 * registration order (ArrayList iteration), so tests can assert exact event lists.
 */
public class BalanceObserverDemo {

    /** The event, as an immutable record — typed, unlike deprecated java.util.Observer's Object arg. */
    public record BalanceEvent(String accountId, BigDecimal newBalance) { }

    /** The Observer role: one method, so a lambda can BE a listener (java8-features 01). */
    public interface BalanceListener {
        void onBalanceChanged(BalanceEvent event);
    }

    /** The Subject: owns the balance AND the listener list. */
    public static final class Account {

        private final String id;
        private BigDecimal balance;
        private final List<BalanceListener> listeners = new ArrayList<>();

        public Account(String id, BigDecimal balance) {
            this.id = id;
            this.balance = balance;
        }

        public void addListener(BalanceListener listener) {
            listeners.add(listener);
        }

        public void removeListener(BalanceListener listener) {
            listeners.remove(listener);
        }

        /**
         * The notify loop: iterate a COPY, so a listener that registers another
         * listener mid-notification cannot cause a ConcurrentModificationException
         * (fail-fast iterators — collections-framework 09).
         */
        public void deposit(BigDecimal amount) {
            balance = balance.add(amount);
            BalanceEvent event = new BalanceEvent(id, balance);
            for (BalanceListener listener : List.copyOf(listeners)) {
                listener.onBalanceChanged(event);
            }
        }

        public BigDecimal balance() {
            return balance;
        }
    }

    public static void main(String[] args) {
        banner("1. Wiring: the subject does not know WHO listens");

        Account account = new Account("ACC-1", new BigDecimal("100.00"));
        List<BalanceEvent> statementPrinter = new ArrayList<>();   // "listener" #1: records events
        List<BalanceEvent> fraudWatcher = new ArrayList<>();       // "listener" #2: records events
        account.addListener(statementPrinter::add);                // method ref IS the observer
        account.addListener(fraudWatcher::add);
        System.out.println("step 1: Account constructed with balance 100.00");
        System.out.println("step 2: two listeners registered (registration order: printer, watcher)");
        System.out.println("verdict: Account's source names NEITHER listener — the list is filled at runtime.");

        banner("2. Deposit publishes: exact events, in registration order");

        account.deposit(new BigDecimal("50.00"));
        account.deposit(new BigDecimal("25.00"));
        System.out.println("step 1: deposit(50.00) -> balance 150.00, event to BOTH listeners");
        System.out.println("step 2: deposit(25.00) -> balance 175.00, event to BOTH listeners");
        System.out.println("printer received = " + statementPrinter);
        System.out.println("watcher received = " + fraudWatcher);
        System.out.println("verdict: 2 deposits x 2 listeners = 4 notifications, each list in deposit order.");

        banner("3. Deregister: silence is immediate and exact");

        BalanceListener watcher = fraudWatcher::add;
        account.removeListener(watcher);
        System.out.println("step 1: removeListener called — but a NEW method reference is a NEW object,");
        System.out.println("        so remove(watcher) removed NOTHING (== identity on lambdas!).");
        System.out.println("step 2: fraudWatcher still has " + fraudWatcher.size() + " events after the failed removal.");
        account.deposit(new BigDecimal("5.00"));
        System.out.println("step 3: deposit(5.00) -> printer = " + statementPrinter.size()
                + " events, watcher = " + fraudWatcher.size() + " events (still subscribed).");
        System.out.println("trap named: to deregister a lambda, keep THE ORIGINAL reference around.");
        System.out.println("            The test does exactly that — registers one object, removes the same object.");

        banner("4. Where you already met it — and where it runs today");

        System.out.println("Swing ActionListener: button = subject, your handler = observer.");
        System.out.println("module 06: future.whenComplete(callback) — one-shot observer on a result.");
        System.out.println("java.util.Observer: deprecated (Java 9), removed — untyped Object events.");
        System.out.println("stage-5: payment-service publishes TransferEvents to Kafka; notification-service");
        System.out.println("         observes them across a NETWORK — same pattern, bigger distance.");

        banner("5. Trap: when NOT to use Observer");

        System.out.println("Every listener must handle exceptions itself: one throwing listener must not");
        System.out.println("silently starve the ones after it (decide a policy and document it).");
        System.out.println("A forgotten removeListener keeps the subject holding the listener alive —");
        System.out.println("a classic GC-root leak (module 07). And if events cross threads, the list");
        System.out.println("needs a thread-safe copy (CopyOnWriteArrayList — collections-framework 09).");

        banner("6. In one sentence");
        System.out.println("Observer inverts the dependency: the subject publishes to a list it never");
        System.out.println("chooses, so new reactions (printers, watchers, Kafka consumers) plug in untouched.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
