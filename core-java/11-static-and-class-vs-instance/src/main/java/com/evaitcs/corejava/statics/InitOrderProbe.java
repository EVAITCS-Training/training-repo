package com.evaitcs.corejava.statics;

/**
 * The INITIALIZATION-ORDER probe: the exact sequence students get wrong on exams,
 * recorded step by step. The full rule set this class demonstrates:
 *
 *   AT CLASS LOAD - once per JVM, triggered by the first active use (module 01
 *   ClassLoadingDemo showed WHEN that happens; `new`, a static method call, or even
 *   reading a static field all count):
 *     1. superclass statics (ProbeBase's static block)
 *     2. own static fields + static initializer blocks, IN TEXTUAL ORDER
 *
 *   AT EVERY `new` - per object:
 *     3. superclass instance init: field initializers, instance blocks, ctor body
 *     4. own instance fields + instance initializer blocks, IN TEXTUAL ORDER
 *     5. own constructor body
 *
 * Module 05 ConstructionOrderDemo showed the constructor half of this; the probe
 * adds the static half and the textual-order details. Read the fields top-to-bottom:
 * the SOURCE ORDER of the file IS the execution order.
 */
public class InitOrderProbe extends ProbeBase {

    // ---- static area: runs ONCE per class load, in the order written ----

    static int STATIC_A = InitOrderLog.noteInt("static field initializer: STATIC_A (textual position 1)", 10);

    /**
     * Proof counter: how many times did this class's static init run? Spoiler: 1 -
     * no matter how many objects get built. Mutable static, so camelCase, NOT
     * UPPER_SNAKE (see the naming note in BankAccount).
     */
    public static int staticInitRuns = 0;

    static {
        InitOrderLog.record("static block: InitOrderProbe (textual position 2 - between the static fields)");
        staticInitRuns++;
    }

    // STATIC_B may build on STATIC_A BECAUSE statics run in textual order and
    // STATIC_A is above. (Referencing a LATER static by simple name before it is
    // initialized is the "illegal forward reference" compile error.)
    static int STATIC_B = InitOrderLog.noteInt(
            "static field initializer: STATIC_B = STATIC_A + 1 (textual position 3)", STATIC_A + 1);

    // ---- instance area: runs PER `new`, in the order written ----

    private String label = InitOrderLog.note("instance field initializer: label (textual position 1)");

    {
        // An INSTANCE INITIALIZER BLOCK - rare in real code, beloved by exams.
        // It runs on EVERY construction, at its textual position among the field
        // initializers, after super() has returned.
        InitOrderLog.record("instance initializer block { } (textual position 2 - between two field initializers)");
    }

    private String serial = InitOrderLog.note("instance field initializer: serial (textual position 3)");

    private final String name;

    public InitOrderProbe(String name) {
        // super() runs first whether written or not: ProbeBase's ENTIRE instance half
        // (baseNote initializer + ctor body) is already recorded before this line.
        this.name = name;
        InitOrderLog.record("constructor body: InitOrderProbe(" + name + ") (the LAST step, every new)");
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getSerial() {
        return serial;
    }
}
