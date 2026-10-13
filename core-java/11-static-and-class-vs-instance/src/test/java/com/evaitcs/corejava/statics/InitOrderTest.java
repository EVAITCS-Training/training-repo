package com.evaitcs.corejava.statics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proof for InitOrderDemo: the EXACT initialization sequence, captured as a printed
 * log list, and the statics-run-ONCE counter.
 *
 * Determinism: this test is the ONLY code in the test JVM that touches InitOrderProbe,
 * and class initialization happens exactly once per JVM - so this test gets the
 * pristine, complete timeline. (One test method on purpose: splitting it in two would
 * make the winner of "who loads the class first" decide which one sees the static
 * steps - the exact flakiness StaticStateCautionDemo warns about.)
 */
class InitOrderTest {

    @Test
    @DisplayName("exact init order: superclass statics, own statics textual, then per-new instance init")
    void exactInitializationSequenceAndStaticsRunOnce() {
        InitOrderLog.LOG.clear(); // defensive: nothing should have touched the probe before this

        // ---- The FIRST (and in this JVM only) class load + first construction ----
        new InitOrderProbe("P-1");
        List<String> firstNew = Arrays.asList(
                "static block: ProbeBase (superclass statics come FIRST, once per class load)",
                "static field initializer: STATIC_A (textual position 1)",
                "static block: InitOrderProbe (textual position 2 - between the static fields)",
                "static field initializer: STATIC_B = STATIC_A + 1 (textual position 3)",
                "instance field initializer: ProbeBase.baseNote (superclass instance state comes first)",
                "constructor body: ProbeBase() (super() always completes before the subclass's own init)",
                "instance field initializer: label (textual position 1)",
                "instance initializer block { } (textual position 2 - between two field initializers)",
                "instance field initializer: serial (textual position 3)",
                "constructor body: InitOrderProbe(P-1) (the LAST step, every new)");
        System.out.println("recorded timeline for the first `new InitOrderProbe(\"P-1\")`:");
        for (int i = 0; i < InitOrderLog.LOG.size(); i++) {
            System.out.println("  step " + (i + 1) + ": " + InitOrderLog.LOG.get(i));
        }
        assertEquals(firstNew, InitOrderLog.LOG, "the timeline must match, in this exact order");

        // ---- Two MORE objects: statics stay silent, instance steps repeat ----
        InitOrderLog.LOG.clear();
        new InitOrderProbe("P-2");
        new InitOrderProbe("P-3");
        System.out.println("timeline after clearing the log and building P-2 and P-3:");
        for (String step : InitOrderLog.LOG) {
            System.out.println("  " + step);
        }
        assertEquals(12, InitOrderLog.LOG.size(), "6 instance steps per object, times 2 objects");
        for (String step : InitOrderLog.LOG) {
            assertFalse(step.startsWith("static"), "no static step may ever run again: " + step);
        }
        assertEquals(1, InitOrderProbe.staticInitRuns,
                "static init ran exactly ONCE for the class, despite 3 instantiations");
        assertTrue(InitOrderLog.LOG.get(0).startsWith("instance field initializer: ProbeBase.baseNote"),
                "each new still starts with the SUPERCLASS instance half");
    }
}
