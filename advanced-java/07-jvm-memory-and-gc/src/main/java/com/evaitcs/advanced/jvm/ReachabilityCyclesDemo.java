package com.evaitcs.advanced.jvm;

import java.lang.ref.WeakReference;

/**
 * Demo 3 — REFERENCE CYCLES ARE COLLECTED: tracing beats counting.
 * (README section "Reachability: the collector's only question".)
 *
 * The claim: the collector never asks "how many references point at this object?"
 * (that is reference COUNTING, and counting leaks on cycles). It asks ONE question:
 * "can I reach this object from a GC ROOT — a live stack frame, a static field, a JNI
 * reference?" Two objects that reference only EACH OTHER are an island: no root reaches
 * either, so both die together, cycle and all.
 *
 * The proof uses WeakReferences as observers (they do not keep anything alive —
 * ReferenceTypesDemo covers the whole ladder) and GcProbe's bounded retry instead of
 * sleep-and-hope: System.gc() is a HINT that default HotSpot honors.
 */
public class ReachabilityCyclesDemo {

    /**
     * Mutable on purpose: a record CANNOT participate in a cycle (final fields, set once
     * at construction), and the cycle is the whole point of this demo.
     */
    static final class Vault {
        final String id;
        Vault partner;   // assigned after construction — this is what allows A -> B -> A

        Vault(String id) {
            this.id = id;
        }
    }

    /** The observers escape the building method; the strong references do not. */
    private record Observers(WeakReference<Vault> a, WeakReference<Vault> b) { }

    public static void main(String[] args) {
        banner("1. Build the island: two vaults referencing ONLY each other");
        // buildIsolatedCycle() creates A and B inside ITS frame, links them, and returns
        // only weak observers. When it returns, its frame pops: from that instant the
        // pair is unreachable from any root, although each still holds the other.
        Observers observers = buildIsolatedCycle();
        System.out.println("step 1: VAULT-A.partner -> VAULT-B and VAULT-B.partner -> VAULT-A (the classic cycle)");
        System.out.println("step 2: both alive right after construction? A=" + (observers.a().get() != null)
                + " B=" + (observers.b().get() != null) + "  (no GC has run against them yet)");
        System.out.println("step 3: no stack local, no static field, no JNI reference reaches either vault —");
        System.out.println("        the building method's frame already popped, taking the only root paths with it.");

        banner("2. Bounded-retry GC, then read the observers");
        // System.gc() is a HINT; GcProbe retries until the 5 s deadline instead of
        // trusting one call — the shared discipline of every GC-dependent line in this module.
        boolean collected = GcProbe.collectUntil(
                () -> observers.a().get() == null && observers.b().get() == null,
                GcProbe.DEADLINE_MS);
        System.out.println("step 4: both weak observers cleared within the deadline = " + collected);
        System.out.println("verdict: the CYCLE WAS COLLECTED. A reference-counting collector would see count 1");
        System.out.println("         on both objects ('each other!') and leak them forever. The JVM traces from");
        System.out.println("         ROOTS instead: unreachable = garbage, no matter how tight the hug.");
    }

    /** Builds the cycle in a private frame so NO strong reference survives into main(). */
    private static Observers buildIsolatedCycle() {
        Vault a = new Vault("VAULT-A");
        Vault b = new Vault("VAULT-B");
        a.partner = b;
        b.partner = a;
        return new Observers(new WeakReference<>(a), new WeakReference<>(b));
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
