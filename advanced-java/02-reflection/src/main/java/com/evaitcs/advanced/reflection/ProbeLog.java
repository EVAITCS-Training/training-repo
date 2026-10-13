package com.evaitcs.advanced.reflection;

/**
 * Holds {@link Probe}'s initialization tripwire in a class of its OWN.
 *
 * The whole point is separation: a test can read ProbeLog.INITIALIZATIONS to see whether
 * Probe has been born yet, and reading THIS class never initializes Probe. If the counter
 * lived on Probe, the act of checking "has Probe initialized?" would itself initialize it
 * (touching any static member triggers class initialization — JLS 12.4.1), and the proof
 * would be circular.
 */
public final class ProbeLog {

    /** How many times Probe's static initializer has run. forName should make this 1. */
    public static int INITIALIZATIONS = 0;

    private ProbeLog() {
        // A holder class is never instantiated.
    }
}
