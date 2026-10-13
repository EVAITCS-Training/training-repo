package com.evaitcs.advanced.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * Demo 1 — THE RUNTIME DATA AREAS, NARRATED NEXT TO LIVE NUMBERS.
 * (README section "Runtime data areas: the map of the machine".)
 *
 * The claim: your program lives in four areas — HEAP (objects, shared by all threads),
 * STACKS (one per thread: frames with locals and references), METASPACE (class
 * metadata), CODE CACHE (JIT-compiled machine code) — and the public management API
 * (MXBeans) lets you READ all of them while the JVM runs.
 *
 * Every number here is PRINTED, never asserted: heap-used wobbles with background
 * activity, and this module's rule (README "The traps", trap 1) is that GC numbers are
 * observations, not contracts.
 */
public class MemoryMapDemo {

    public static void main(String[] args) {
        // An MXBean ("em-X-bean") is a Managed Bean — the JVM's standardized self-reporting
        // object, read through java.lang.management. Public API only: no com.sun internals,
        // no --add-opens, nothing a production JVM would refuse.
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();

        banner("1. The four areas — what lives where");
        System.out.println("step 1: HEAP      — every object you new into existence; SHARED by all threads; the GC's territory.");
        System.out.println("step 2: STACKS    — one per thread; a FRAME per active method call, holding locals and references.");
        System.out.println("step 3: METASPACE — the blueprints: class metadata (methods, field layouts, constants); native memory.");
        System.out.println("step 4: CODE CACHE— the JIT compiler's output: machine code translated from hot bytecode.");
        System.out.println("verdict: variables live on stacks; VALUES live on the heap; classes and machine code live off to the side.");

        banner("2. The same areas, measured live through MXBeans");
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();
        System.out.println("step 1: HEAP      used " + mb(heap.getUsed()) + " | committed " + mb(heap.getCommitted())
                + " | max " + mb(heap.getMax()));
        System.out.println("step 2: NON-HEAP  used " + mb(nonHeap.getUsed()) + "  (metaspace + code cache + compressed classes)");
        System.out.println("step 3: THREADS   live = " + threads.getThreadCount()
                + " — each owns a private stack (budget: -Xss, default ~1 MB), INVISIBLE to heap numbers");

        banner("3. The named pools — these strings ARE the generations from the README diagram");
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            MemoryUsage usage = pool.getUsage();
            String used = usage == null ? "not measurable" : mb(usage.getUsed());
            System.out.printf(Locale.ROOT, "  pool %-32s type %-8s used %s%n", pool.getName(), pool.getType(), used);
        }
        System.out.println("verdict: on default Java 21 you just read G1's Eden, Survivor and Old pools — the nursery");
        System.out.println("         and the old-age home are real, named, measurable places (GcTourDemo tours them).");

        banner("4. References on the stack, objects on the heap (core-java 02, grown up)");
        Account first = new Account("A-101", "Dana", new BigDecimal("1500.00"));
        Account second = first;   // copies the REFERENCE (a stack slot), not the object
        MemoryUsage before = memory.getHeapMemoryUsage();
        Account third = new Account("A-202", "Ana", new BigDecimal("250.75"));   // one more heap object
        MemoryUsage after = memory.getHeapMemoryUsage();

        System.out.println("step 1: 'first' and 'second' are two stack references to ONE heap object:");
        System.out.println("        identityHashCode equal = " + (System.identityHashCode(first) == System.identityHashCode(second)));
        System.out.println("step 2: allocating 'third' moved heap-used from " + mb(before.getUsed()) + " to " + mb(after.getUsed())
                + " (delta printed, never asserted — background GC and JIT noise are normal)");
        System.out.println("step 3: 'third' goes out of scope when main returns; its frame pops, the reference dies,");
        System.out.println("        and the object becomes unreachable — collectable, at the GC's convenience.");
        System.out.println("verdict: the frame is cheap and dies young; the object's fate depends ONLY on reachability.");

        banner("5. In one sentence");
        System.out.println("The JVM is four rooms — heap for objects, stacks for frames, metaspace for classes,");
        System.out.println("code cache for machine code — and the MXBeans let you stand in the doorway and watch.");
    }

    private static String mb(long bytes) {
        return bytes < 0 ? "undefined" : String.format(Locale.ROOT, "%.1f MB", bytes / 1_048_576.0);
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
