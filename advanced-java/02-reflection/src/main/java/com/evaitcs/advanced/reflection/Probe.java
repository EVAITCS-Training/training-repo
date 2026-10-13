package com.evaitcs.advanced.reflection;

/**
 * A dedicated tripwire for the "Class.forName LOADS **and INITIALIZES**" proof.
 * (README walkthrough: "what happens when you call Class.forName(name)".)
 *
 * The static block below runs exactly once — the first time this class is INITIALIZED.
 * Class.forName(name) initializes; the Account.class literal and account.getClass() do
 * NOT. So the only thing that should fire this tripwire in the test is forName.
 *
 * WHY the count lives in {@link ProbeLog}, a DIFFERENT class: reading a static field of
 * Probe (say Probe.anyField) would itself initialize Probe (JLS 12.4.1) and ruin the
 * "before" reading. Keeping the counter in a separate class lets the test watch Probe be
 * born without touching it. That subtlety is the whole reason this class is so small.
 */
public class Probe {

    static {
        // The one and only side effect: bump the counter that lives in another class.
        ProbeLog.INITIALIZATIONS = ProbeLog.INITIALIZATIONS + 1;
    }

    public Probe() {
        // Never called by the forName proof — initialization is not instantiation.
    }
}
