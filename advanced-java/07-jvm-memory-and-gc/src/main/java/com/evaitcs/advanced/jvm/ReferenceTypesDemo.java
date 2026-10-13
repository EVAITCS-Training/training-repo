package com.evaitcs.advanced.jvm;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Demo 4 — THE java.lang.ref TOUR: strong, soft, weak, phantom — plus the ReferenceQueue
 * and the WeakHashMap that runs on it.
 * (README sections "The reference-strength ladder" and "The table to photograph".)
 *
 * The claim: "holding a reference" is not one thing but FOUR contracts with the
 * collector — forever (strong), until the heap hurts (soft), one GC pass (weak), and
 * "notify me at the funeral" (phantom, whose get() is ALWAYS null). The ReferenceQueue
 * is the notification channel; WeakHashMap is the standard consumer of it.
 *
 * Every GC-dependent line goes through GcProbe.collectUntil (bounded retry, 5 s
 * deadline) — System.gc() is a HINT that default HotSpot honors, never a guarantee.
 */
public class ReferenceTypesDemo {

    public static void main(String[] args) {
        banner("1. The matrix: four strengths, four fates, one GC pass");

        // STRONG: an ordinary variable. A GC root path reaches the object, so the
        // collector must keep it — this is the only "reference" Java had until 1.2.
        Account strong = new Account("A-101", "Dana", new BigDecimal("1500.00"));

        // SOFT: cache material — kept until the collector, under real memory pressure,
        // decides it needs the space (policy: everything soft goes shortly before OOM).
        SoftReference<Account> soft = new SoftReference<>(
                new Account("A-202", "Ana", new BigDecimal("250.75")));

        // WEAK: survives only until the NEXT collection — "keep it if keeping is free".
        WeakReference<Account> weak = new WeakReference<>(
                new Account("A-303", "Bo", new BigDecimal("10.00")));

        // PHANTOM: get() is ALWAYS null, even while the referent lives. A phantom exists
        // only to be ENQUEUED after collection — cleanup scheduling, not access. This is
        // the machinery under DirectByteBuffer's Cleaner: native memory freed post-mortem.
        ReferenceQueue<Account> queue = new ReferenceQueue<>();
        PhantomReference<Account> phantom = new PhantomReference<>(
                new Account("A-404", "Cy", new BigDecimal("0.00")), queue);

        System.out.println("step 1: STRONG  get() = " + strong.id() + " — the collector NEVER takes a strongly reachable object");
        System.out.println("step 2: SOFT    get() = " + describe(soft.get()) + " — alive so far; falls only under memory pressure");
        System.out.println("step 3: WEAK    get() = " + describe(weak.get()) + " — alive so far; the next pass takes it");
        System.out.println("step 4: PHANTOM get() = " + phantom.get() + " — ALWAYS null by contract; it only ever ENQUEUES");

        banner("2. One bounded-retry GC storm — watch who survives");
        boolean weakCleared = GcProbe.collectUntil(() -> weak.get() == null, GcProbe.DEADLINE_MS);
        AtomicReference<Reference<? extends Account>> enqueued = new AtomicReference<>();
        boolean phantomEnqueued = GcProbe.collectUntil(() -> {
            Reference<? extends Account> polled = queue.poll();
            if (polled != null) {
                enqueued.set(polled);
            }
            return polled != null;
        }, GcProbe.DEADLINE_MS);

        System.out.println("step 1: STRONG  still here = " + (strong != null) + " — 'strong' is a live local in THIS frame: a root");
        System.out.println("step 2: SOFT    survived full GC = " + (soft.get() != null) + " — soft refs need real pressure, not a polite request (printed, not asserted)");
        System.out.println("step 3: WEAK    cleared within deadline = " + weakCleared);
        System.out.println("step 4: PHANTOM arrived on the ReferenceQueue = " + phantomEnqueued
                + (phantomEnqueued ? " — same reference object we registered: " + (enqueued.get() == phantom) : ""));
        System.out.println("verdict: strength is a CONTRACT with the collector — forever, until it hurts, one pass, notify me.");

        banner("3. WeakHashMap: the self-cleaning registry (the ladder at work)");
        // WeakHashMap holds KEYS through weak references and an internal ReferenceQueue:
        // a key nobody else references is cleared at the next GC and the entry expunged
        // on the next map access. The classic pitfall decides our key choice: an INTERNED
        // string literal is strongly reachable from the string pool FOREVER (core-java 03),
        // so a literal key would never be evicted. Hence new String(...).
        Map<String, String> sessions = new WeakHashMap<>();
        String retainedKey = new String("SESSION-RETAINED");   // held strongly below -> entry must survive
        sessions.put(retainedKey, "open");
        List<String> droppableKeys = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            String key = new String("SESSION-" + i);   // fresh heap String: NOT interned, no other strong ref
            droppableKeys.add(key);                    // held strongly FOR NOW, so the starting size is exact
            sessions.put(key, "open");
        }
        int before = sessions.size();
        System.out.println("step 1: put 1001 entries (keys weakly held); size = " + before);
        droppableKeys.clear();                          // drop 1000 roots AT ONCE — deterministic starting gun
        boolean evicted = GcProbe.collectUntil(() -> sessions.size() <= 1, GcProbe.DEADLINE_MS);
        System.out.println("step 2: after bounded-retry GC, size = " + sessions.size()
                + " (only the retained key survives) = " + evicted);
        System.out.println("WHY: the dropped keys became weakly reachable, the GC cleared them, and the map's");
        System.out.println("     internal ReferenceQueue told WeakHashMap WHICH entries to expunge.");
        System.out.println("verdict: a cache that shrinks ITSELF — but a VALUE that strongly references its own key");
        System.out.println("         pins the entry forever (trap 5: the map strongly holds every value).");

        banner("4. In one sentence");
        System.out.println("Reference strength is how loudly you tell the collector 'keep this':");
        System.out.println("strong = forever, soft = until the heap hurts, weak = one pass, phantom = notify me at the funeral.");
    }

    private static String describe(Account account) {
        return account == null ? "null (already cleared)" : account.id();
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
