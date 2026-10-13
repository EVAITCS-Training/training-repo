package com.evaitcs.advanced.jvm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 07-jvm-memory-and-gc test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Determinism contract for a GC module (README "Class flow"):
 *  - EVERY GC-dependent test goes through the ONE shared bounded-retry helper
 *    GcProbe.collectUntil(condition, deadlineMs) — request, observe, retry until a
 *    5 s deadline; never sleep-and-hope (its javadoc explains why System.gc() being a
 *    HINT is fine here: default HotSpot honors it, and we only assert OBSERVABLES —
 *    cleared / not cleared / enqueued — never collector internals);
 *  - we assert STRUCTURE (reachability outcomes, monotonic counts, map sizes), never
 *    heap sizes, exact GC counts, or timings;
 *  - strong-reachability claims (a held object survives, a registry entry exists) are
 *    facts the collector GUARANTEES, so those assertions are unconditional.
 */
class JvmGcTest {

    @Test
    @DisplayName("recursion without a base case spends the frame budget: StackOverflowError after more than 1000 frames")
    void stackOverflowCountsFrames() {
        int[] depth = {0};
        StackOverflowError error = assertThrows(StackOverflowError.class, () -> recurse(0, depth));
        assertNotNull(error);
        assertTrue(depth[0] > 1000, "a default stack buys thousands of frames; we claim only a generous floor");
        System.out.println("CAUGHT -> StackOverflowError at depth " + depth[0]
                + " (floor 1000) — the budget is -Xss per thread, printed not asserted");
    }

    /** No base case on purpose: every call pushes one more frame and records the depth. */
    private static void recurse(int depth, int[] deepest) {
        deepest[0] = depth;
        recurse(depth + 1, deepest);
    }

    @Test
    @DisplayName("a weakly reachable object is cleared: weak.get() is null within the bounded-retry deadline")
    void weakReferenceClearedAfterGc() {
        // The referent is created INSIDE the constructor call — no strong local ever held
        // it, so it is weakly reachable (README ladder: 'one pass') from the first instant.
        WeakReference<Account> weak = new WeakReference<>(
                new Account("A-303", "Bo", new BigDecimal("10.00")));

        boolean cleared = GcProbe.collectUntil(() -> weak.get() == null, GcProbe.DEADLINE_MS);

        assertTrue(cleared, "System.gc() is a HINT default HotSpot honors; 5 s is a generous deadline");
        assertNull(weak.get());
        System.out.println("weak.get() = null within " + GcProbe.DEADLINE_MS + " ms — weak means 'clear me at the next pass'");
    }

    @Test
    @DisplayName("a strongly reachable object survives every collection that clears its weak-only twin: same object, assertSame")
    void strongReferenceSurvivesTheSameGc() {
        Account held = new Account("A-101", "Dana", new BigDecimal("1500.00"));   // 'held' is a stack local: a GC root path
        WeakReference<Account> observer = new WeakReference<>(held);               // also observed weakly...
        WeakReference<Account> doomed = new WeakReference<>(                       // ...while this twin has NO strong holder
                new Account("A-202", "Ana", new BigDecimal("250.75")));

        // One bounded-retry pass: wait until the DOOMED twin is cleared — which proves a
        // real full GC ran — while the observer's referent stays strongly reachable.
        boolean twinCleared = GcProbe.collectUntil(() -> doomed.get() == null, GcProbe.DEADLINE_MS);

        assertTrue(twinCleared, "the weak-only twin must fall: it proves the collector really ran");
        assertSame(held, observer.get(), "strong reachability wins: the observer still sees the SAME object");
        System.out.println("same GC pass cleared the weak-only twin; the strongly held account survived: " + observer.get().id());
    }

    @Test
    @DisplayName("a reference cycle with no outside root is collected whole: tracing beats reference counting")
    void referenceCycleIsCollected() {
        CycleObservers observers = buildIsolatedCycle();

        boolean collected = GcProbe.collectUntil(
                () -> observers.a().get() == null && observers.b().get() == null,
                GcProbe.DEADLINE_MS);

        assertTrue(collected, "both halves of the rootless cycle become unreachable together");
        assertNull(observers.a().get());
        assertNull(observers.b().get());
        System.out.println("cycle A<->B collected: the JVM asks 'reachable from a ROOT?', not 'how many point here?'");
    }

    /** Mirrors ReachabilityCyclesDemo: the cycle is built in a frame that pops before we GC. */
    private record CycleObservers(WeakReference<ReachabilityCyclesDemo.Vault> a,
                                  WeakReference<ReachabilityCyclesDemo.Vault> b) { }

    private static CycleObservers buildIsolatedCycle() {
        ReachabilityCyclesDemo.Vault a = new ReachabilityCyclesDemo.Vault("VAULT-A");
        ReachabilityCyclesDemo.Vault b = new ReachabilityCyclesDemo.Vault("VAULT-B");
        a.partner = b;
        b.partner = a;
        return new CycleObservers(new WeakReference<>(a), new WeakReference<>(b));
        // frame pops here: a and b die as ROOTS, the island is unreachable — cycle and all
    }

    @Test
    @DisplayName("the ReferenceQueue hands back the very reference object once its referent is collected")
    void referenceQueueReceivesClearedReference() {
        ReferenceQueue<Account> queue = new ReferenceQueue<>();
        WeakReference<Account> weak = new WeakReference<>(
                new Account("A-999", "Zed", new BigDecimal("0.01")), queue);

        // Bounded retry through the SAME shared helper; the poll result is captured
        // because poll() removes — the condition must not lose the evidence.
        AtomicReference<Reference<? extends Account>> polled = new AtomicReference<>();
        boolean enqueued = GcProbe.collectUntil(() -> {
            Reference<? extends Account> r = queue.poll();
            if (r != null) {
                polled.set(r);
            }
            return r != null;
        }, GcProbe.DEADLINE_MS);

        assertTrue(enqueued, "the cleared reference must arrive on its queue within the deadline");
        assertSame(weak, polled.get(), "the queue returns the SAME reference object we registered");
        assertNull(weak.get());
        System.out.println("queue.poll() returned our WeakReference — this notification is exactly how WeakHashMap expunges entries");
    }

    @Test
    @DisplayName("WeakHashMap evicts every entry whose key lost all strong references, keeping the retained one")
    void weakHashMapShrinksAfterKeyDrop() {
        Map<String, String> sessions = new WeakHashMap<>();
        String retained = new String("RETAINED-SESSION");   // held strongly by this frame -> must survive
        sessions.put(retained, "open");
        List<String> droppable = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            String key = new String("SESSION-" + i);   // new String: NOT interned — a literal would be a root forever (core-java 03)
            droppable.add(key);                        // held strongly for now, so the starting size is exact
            sessions.put(key, "open");
        }
        assertEquals(1_001, sessions.size());

        droppable.clear();                             // drop 1000 roots AT ONCE — deterministic start gun
        boolean evicted = GcProbe.collectUntil(() -> sessions.size() <= 1, GcProbe.DEADLINE_MS);

        assertTrue(evicted, "only the retained key may survive; the rest were weakly held");
        assertEquals(1, sessions.size());
        assertTrue(sessions.containsKey(retained), "the strongly held key's entry is untouched");
        System.out.println("size 1001 -> " + sessions.size() + " after bounded-retry GC: the map cleans ITSELF via its ReferenceQueue");
    }

    @Test
    @DisplayName("management MXBeans report a live JVM: heap used > 0, collectors exist, and counts never decrease")
    void mxBBeansReportSaneNumbers() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long heapUsed = memory.getHeapMemoryUsage().getUsed();
        assertTrue(heapUsed > 0, "a running JVM always has SOME heap in use");

        List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
        assertFalse(collectors.isEmpty(), "every HotSpot configuration exposes at least one collector bean");

        long before = totalCollections();
        byte[] ballast = new byte[8 * 1024 * 1024];   // 8 MB nudge: MAY provoke a young GC — or not
        ballast[0] = 1;                               // touch it so the allocation is real
        long after = totalCollections();
        assertTrue(after >= before, "collection counts are CUMULATIVE and monotonic — >= is the ONLY safe assertion");

        System.out.println("heap used = " + (heapUsed / 1_048_576) + " MB; collectors = "
                + collectors.stream().map(GarbageCollectorMXBean::getName).toList());
        System.out.println("counts before/after the 8 MB nudge: " + before + " -> " + after + " (exact values printed, never asserted)");
    }

    private static long totalCollections() {
        long total = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            total += Math.max(0, gc.getCollectionCount());   // -1 = undefined on some beans
        }
        return total;
    }

    @Test
    @DisplayName("the leak registry is structural: a static map grows with every put and empties on clear — no heap numbers involved")
    void leakRegistryGrowsAndClears() {
        // Uses the demo's OWN static registry — the leak's body, package-visible.
        // The proof is pure structure: size follows puts, clear() empties it. Whether the
        // heap visibly drops is a PRINTED observation in LeakHuntDemo, never an assertion.
        LeakHuntDemo.REGISTRY.clear();                 // start from a known state
        int entries = 1_000;
        for (int i = 0; i < entries; i++) {
            LeakHuntDemo.REGISTRY.put("SESSION-" + i, new byte[64]);
        }
        assertEquals(entries, LeakHuntDemo.REGISTRY.size(),
                "every put is immortal while the static root holds it — that IS the leak");

        LeakHuntDemo.REGISTRY.clear();
        assertEquals(0, LeakHuntDemo.REGISTRY.size(),
                "clear() lets go of every root at once — the heap gets it back at the next GC");
        System.out.println("static registry: 0 -> " + entries + " -> 0 entries. Reachable but useless = leak; REMOVE is the only fix.");
    }
}
