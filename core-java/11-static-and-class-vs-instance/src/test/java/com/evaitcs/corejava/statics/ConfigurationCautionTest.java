package com.evaitcs.corejava.statics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Proof for StaticStateCautionDemo: Configuration's static map is ONE global, so
 * "unrelated" callers interfere through it - and every test in the JVM inherits
 * whatever the previous test left behind unless someone resets.
 *
 * @AfterEach reset() below is the standard apology ritual for global state. Notice
 * how much ceremony a single static HashMap creates - that ceremony IS the argument
 * for preferring static final immutable data.
 */
class ConfigurationCautionTest {

    @AfterEach
    void cleanTheGlobal() {
        Configuration.reset(); // without this, THIS test becomes the leak the demo warns about
    }

    @Test
    @DisplayName("one caller's write is instantly visible to a completely unrelated caller")
    void globalStateLeaksBetweenUnrelatedCallers() {
        assertNull(Configuration.get("audit.mode"), "start clean (the @AfterEach promise)");

        checkoutWrites();                      // "feature A" writes the global...
        String seenByReport = reportReads();   // ..."feature B" reads it: no parameter,
                                               // no return value, no signature hint
        System.out.println("checkout wrote audit.mode=verbose; report (unrelated) read: " + seenByReport);
        assertEquals("verbose", seenByReport, "the static map is ONE object for the whole JVM");
    }

    @Test
    @DisplayName("order dependence: the same calls in a different order behave differently")
    void sameCallsDifferentOrderDifferentBehavior() {
        // Report BEFORE checkout this time - the mirror image of the test above.
        String firstRead = reportReads();
        checkoutWrites();
        String secondRead = reportReads();

        System.out.println("report before checkout -> " + firstRead);
        System.out.println("report after  checkout -> " + secondRead);
        assertNull(firstRead, "before the write, the global is empty");
        assertEquals("verbose", secondRead, "after the write, the same read returns something else");
        // THIS is the "passes alone, fails together" mechanism: whichever test runs
        // first decides what the next one sees. JUnit's method order is deliberately
        // not obvious - global state turns that into a lottery.
    }

    private static void checkoutWrites() {
        Configuration.set("audit.mode", "verbose");
    }

    private static String reportReads() {
        return Configuration.get("audit.mode");
    }
}
