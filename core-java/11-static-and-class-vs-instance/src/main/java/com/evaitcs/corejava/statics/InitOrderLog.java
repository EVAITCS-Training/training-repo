package com.evaitcs.corejava.statics;

import java.util.ArrayList;
import java.util.List;

/**
 * Silent recorder for the initialization-order probe.
 *
 * It is a SEPARATE class so ProbeBase/InitOrderProbe can log from inside their OWN
 * initializers. Touching InitOrderLog.LOG does trigger InitOrderLog's class init
 * (referencing a static field is an "active use" - module 01 ClassLoadingDemo), so
 * its initializer is deliberately EMPTY beyond creating the list: it adds no noise
 * to the recorded timeline.
 */
final class InitOrderLog {

    /** The recorded timeline. Package-visible: the demo prints it, the test asserts it. */
    static final List<String> LOG = new ArrayList<String>();

    private InitOrderLog() {
        // utility class - module habit by now (see BankUtils)
    }

    /** Records the step and RETURNS it, so it can be used inside a field initializer. */
    static String note(String step) {
        LOG.add(step);
        return step;
    }

    /** Same trick for int fields: STATIC_B's initializer logs AND computes. */
    static int noteInt(String step, int value) {
        LOG.add(step);
        return value;
    }

    static void record(String step) {
        LOG.add(step);
    }
}
