package com.evaitcs.advanced.locks;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 05-locks-and-atomics compile exec:java
 *
 * The order is the argument: first the MONITOR you already have (reentrancy, ownership,
 * wait-releases-the-lock), then how ReentrantLock SELLS IT IN PIECES (tryLock, timeouts,
 * fairness), then the piece that gives you MULTIPLE waiting queues (Conditions, via the
 * bounded buffer), then READ/WRITE separation (and StampedLock's optimistic reads), then
 * the other way to be atomic without a queue at all (CAS and the lost update), then the
 * scar CAS leaves (ABA and the version stamp), and finally what queues do wrong (deadlock,
 * detected and fixed).
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 05-locks-and-atomics compile exec:java "-Dexec.mainClass=com.evaitcs.advanced.locks.CasRaceDemo"
 */
public class LocksDemos {

    public static void main(String[] args) throws Exception {
        section("MonitorReentrancyDemo — reentrancy, wait/notify ownership, and wait() releases the lock");
        MonitorReentrancyDemo.main(args);

        section("TryLockDemo — lock/unlock discipline, tryLock(), tryLock(timeout), fairness");
        TryLockDemo.main(args);

        section("BoundedBufferDemo — one lock, two Conditions: the notFull/notEmpty dance");
        BoundedBufferDemo.main(args);

        section("ReadWriteLockDemo — readers share, a writer owns; StampedLock reads optimistically");
        ReadWriteLockDemo.main(args);

        section("CasRaceDemo — the lost update, CAS to the rescue, LongAdder vs AtomicLong");
        CasRaceDemo.main(args);

        section("AbaDemo — when 'unchanged' lies, and the version stamp that catches it");
        AbaDemo.main(args);

        section("DeadlockDemo — a real deadlock, detected by name, then fixed two ways");
        DeadlockDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
