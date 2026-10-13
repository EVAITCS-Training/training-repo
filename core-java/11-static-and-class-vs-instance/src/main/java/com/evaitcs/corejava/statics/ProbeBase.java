package com.evaitcs.corejava.statics;

/**
 * The superclass half of InitOrderProbe - exists ONLY so the timeline can show the
 * two "superclass first" rules:
 *   - at CLASS LOAD: superclass STATICS run before the subclass's own statics;
 *   - at every NEW:  the superclass's whole instance half (fields, blocks, ctor body)
 *     completes before the subclass's instance half begins.
 *
 * DETERMINISM CONTRACT: nothing in this module touches ProbeBase or InitOrderProbe
 * except InitOrderDemo and InitOrderTest, and each runs in its own JVM invocation -
 * static init happens exactly once per class per JVM, so the recorded sequence is
 * pristine for whoever loads the class first. Keep it that way.
 */
class ProbeBase {

    static {
        InitOrderLog.record("static block: ProbeBase (superclass statics come FIRST, once per class load)");
    }

    private String baseNote =
            InitOrderLog.note("instance field initializer: ProbeBase.baseNote (superclass instance state comes first)");

    ProbeBase() {
        InitOrderLog.record("constructor body: ProbeBase() (super() always completes before the subclass's own init)");
    }

    String getBaseNote() {
        return baseNote;
    }
}
