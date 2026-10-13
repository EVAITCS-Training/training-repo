package com.evaitcs.advanced.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Demo 5 — THE LEAK HUNT: in a GC language, a leak is REACHABLE BUT USELESS.
 * (README section "Memory leaks in a garbage-collected language".)
 *
 * The classic shape: a STATIC registry that only ever grows. A static field of a loaded
 * class is a GC ROOT, so every entry it holds is immortal until somebody removes it —
 * even when no code will ever read it again. Java killed the C-style leak (forgot to
 * free); it cannot kill the registry-style leak (forgot to REMOVE).
 *
 * Heap checkpoints are printed through the MemoryMXBean around fill -> clear -> gc.
 * The NUMBERS are observations (they wobble with JIT and background activity); the
 * STRUCTURE — registry grows, heap follows; registry clears, heap follows back — is the
 * fact. Nothing here is asserted, and JvmGcTest proves the structure without touching
 * a single heap number.
 */
public class LeakHuntDemo {

    /**
     * THE LEAK: a static Map is one hop from a GC root; whatever it holds is immortal.
     * Package-private so the test can prove grows/clears structurally.
     */
    static final Map<String, byte[]> REGISTRY = new HashMap<>();

    private static final int ENTRIES = 100_000;
    private static final int PAYLOAD_BYTES = 64;   // each "audit record" gets weight: ~100k x (key + node + payload)

    public static void main(String[] args) {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();

        banner("1. Checkpoint: a calm baseline");
        System.gc();   // HINT (see GcProbe): best-effort full GC so the baseline is calm — printed, never asserted
        settle();
        long baseline = memory.getHeapMemoryUsage().getUsed();
        System.out.println("step 1: heap used = " + mb(baseline) + " | registry entries = " + REGISTRY.size());

        banner("2. Fill the static registry — 'sessions' nobody ever closes");
        for (int i = 0; i < ENTRIES; i++) {
            // A real bank leaks audit records, HTTP sessions, listener lists — same shape
            // (static, only-grows), bigger objects. Key strings are fresh (not interned),
            // so the ONLY root path to any entry is the registry itself.
            REGISTRY.put("SESSION-" + i, new byte[PAYLOAD_BYTES]);
        }
        long filled = memory.getHeapMemoryUsage().getUsed();
        System.out.println("step 1: put " + ENTRIES + " entries; registry size = " + REGISTRY.size());
        System.out.println("step 2: heap used = " + mb(filled) + " (growth " + mb(filled - baseline) + ")");
        System.out.println("step 3: every byte is REACHABLE from the static field — the collector must keep ALL of it,");
        System.out.println("        although nothing in the program will ever read it again. This is the leak.");

        banner("3. The fix: REMOVE — then watch the heap give it back");
        REGISTRY.clear();   // the root lets go: everything just dropped became unreachable garbage at once
        System.gc();        // HINT again; default HotSpot honors it with a full collection
        settle();
        long cleared = memory.getHeapMemoryUsage().getUsed();
        System.out.println("step 1: registry entries = " + REGISTRY.size());
        System.out.println("step 2: heap used = " + mb(cleared) + " | drop from filled = " + mb(filled - cleared));
        System.out.println("step 3: the drop is printed, never asserted — G1 may hand memory back lazily, and the");
        System.out.println("        exact bytes depend on JVM, heap size and whatever else is alive right now.");

        banner("4. Verdict, and how to feel the OutOfMemoryError in class");
        System.out.println("verdict: REACHABLE BUT USELESS = LEAK. The GC frees garbage; it cannot free dead weight");
        System.out.println("         you still hold. In production nobody calls clear() — the registry just grows.");
        System.out.println("Want the OOM live? Run THIS demo in its own JVM with a 16 MB heap (README shows the");
        System.out.println("command — separate process, on purpose; tests never provoke OutOfMemoryError):");
        System.out.println("  java -Xmx16m -cp 07-jvm-memory-and-gc\\target\\classes com.evaitcs.advanced.jvm.LeakHuntDemo");
        System.out.println("  expected: Exception in thread \"main\" java.lang.OutOfMemoryError: Java heap space");
    }

    /**
     * Let the JVM settle before reading a PRINTED heap number (200 ms generous floor —
     * background JIT/GC activity makes an immediate read noisy). Observation aid only:
     * no assertion in this module ever depends on it.
     */
    private static void settle() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String mb(long bytes) {
        return String.format(Locale.ROOT, "%.1f MB", bytes / 1_048_576.0);
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
