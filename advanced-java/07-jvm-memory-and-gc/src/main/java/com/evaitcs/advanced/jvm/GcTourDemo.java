package com.evaitcs.advanced.jvm;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Demo 6 — MEET YOUR COLLECTOR: GarbageCollectorMXBeans, an allocation burst, and how
 * to watch the whole thing live with -Xlog:gc.
 * (README sections "Heap generations" and "Collector tour".)
 *
 * The claim: the generational hypothesis — most objects die young — is measurable. A
 * burst of short-lived allocations fills Eden and fires YOUNG collections, visible as
 * the young collector's cumulative count climbing. Counts are MONOTONIC (they never
 * decrease) but their exact values depend on Eden size, heap and workload — so this
 * demo prints them, and the test asserts only ">=".
 */
public class GcTourDemo {

    public static void main(String[] args) {
        banner("1. Who is on duty? (on default Java 21: G1's young, old and concurrent collectors)");
        List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gc : collectors) {
            System.out.println("  collector " + gc.getName()
                    + " | collections so far = " + gc.getCollectionCount()
                    + " | total pause = " + gc.getCollectionTime() + " ms");
        }
        System.out.println("note: on G1 you see the YOUNG collector (Eden + survivors: frequent, cheap — it copies");
        System.out.println("      only SURVIVORS), the OLD/mixed collector (the old-age home: rare, expensive), and");
        System.out.println("      G1 Concurrent GC (the marking cycles that keep pauses short). Other JVM flags would");
        System.out.println("      print 'PS Scavenge'/'PS MarkSweep' (Parallel), 'ZGC' or 'Shenandoah'.");

        banner("2. Allocation burst: 200,000 short-lived 1 KB objects — the hypothesis at work");
        long countBefore = totalCollections();
        long checksum = 0;
        for (int i = 0; i < 200_000; i++) {
            byte[] shortLived = new byte[1024];   // born in Eden...
            checksum += shortLived[0];            // ...touched once (keeps the loop honest against dead-code elimination)...
            // ...and dead RIGHT HERE: the next iteration overwrites the only reference.
            // Most objects die young — that is WHY Eden can be swept by copying the few
            // survivors instead of scanning the many corpses. (Once the JIT decides this
            // loop is hot, escape analysis may even scalar-replace some allocations away —
            // README "Escape analysis": allocation is cheap, sometimes free.)
        }
        long countAfter = totalCollections();
        System.out.println("step 1: total collections before burst = " + countBefore + ", after = " + countAfter
                + " (checksum " + checksum + ")");
        System.out.println("step 2: delta = " + (countAfter - countBefore) + " — young collections fired while Eden filled and emptied.");
        System.out.println("        NEVER assert this number: it depends on Eden size, heap size and JIT mood.");
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.println("  collector NOW " + gc.getName()
                    + " | count = " + gc.getCollectionCount()
                    + " | pause = " + gc.getCollectionTime() + " ms   (cumulative, monotonic — the test asserts only >=)");
        }

        banner("3. See it LIVE in class: unified GC logging on a separate JVM");
        System.out.println("Compile once, then run this demo in its OWN process with GC logging on:");
        System.out.println("  java -Xlog:gc -cp 07-jvm-memory-and-gc\\target\\classes com.evaitcs.advanced.jvm.GcTourDemo");
        System.out.println("Every collection then prints a line like:");
        System.out.println("  [1.234s][info][gc] GC(3) Pause Young (Normal) (G1 Evacuation Pause) 24M->2M(256M) 4.321ms");
        System.out.println("  read as: 4th GC, young pause, heap went 24M -> 2M of a 256M heap, took 4.3 ms.");
        System.out.println("verdict: the MXBeans tell you WHO collected and HOW OFTEN; -Xlog:gc tells you WHY and HOW LONG.");

        banner("4. In one sentence");
        System.out.println("Generations exist because most objects die young: Eden is a cheap mass grave,");
        System.out.println("survivors are copied and aged, and only the tenured reach the expensive old-age home.");
    }

    /** Sum of all collectors' cumulative counts; -1 (undefined on some beans) counts as 0. */
    private static long totalCollections() {
        long total = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            total += Math.max(0, gc.getCollectionCount());
        }
        return total;
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
