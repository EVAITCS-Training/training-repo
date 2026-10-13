package com.evaitcs.corejava.statics;

/**
 * Demo 4 - INITIALIZATION ORDER, printed step by step from InitOrderProbe.
 *
 * The sequence students get wrong on exams:
 *   class load (ONCE):  superclass statics -> own statics, textual order
 *   every `new`:       superclass instance init -> own fields/blocks, textual order -> ctor body
 *
 * Determinism: InitOrderProbe is a FRESH class - nothing touched it before this
 * demo (or, under `mvnw test`, before InitOrderTest), so its once-per-JVM static
 * steps are pristine. Referencing the class at all (even reading a static field)
 * triggers the load - module 01 ClassLoadingDemo; module 05 ConstructionOrderDemo
 * showed the constructor half of this story.
 */
public class InitOrderDemo {

    public static void main(String[] args) {

        banner("1. The FIRST new InitOrderProbe - class load + instance init, ten steps");
        InitOrderLog.LOG.clear(); // defensive: first touch in this JVM should record everything
        new InitOrderProbe("P-1");
        printLog();
        System.out.println("Read the top four lines: SUPERCLASS statics first, then own statics");
        System.out.println("IN TEXTUAL ORDER (STATIC_A, the block, then STATIC_B = STATIC_A + 1).");
        System.out.println("Then the per-object half: ProbeBase's instance state and ctor, own");
        System.out.println("fields/blocks in textual order, and the ctor body LAST.");

        banner("2. Second and third probes - the static steps stay SILENT");
        InitOrderLog.LOG.clear();
        new InitOrderProbe("P-2");
        new InitOrderProbe("P-3");
        printLog();
        System.out.println("Six instance steps per object, twelve lines total - and NOT ONE");
        System.out.println("static line: the class was loaded long ago (well, microseconds).");

        banner("3. The proof counter");
        System.out.println("InitOrderProbe.staticInitRuns = " + InitOrderProbe.staticInitRuns
                + "   <- after 3 instantiations. Statics ran ONCE.");
        System.out.println("STATIC_A = " + InitOrderProbe.STATIC_A + ", STATIC_B = " + InitOrderProbe.STATIC_B
                + "   <- B built on A at class load: textual order is real, not decoration.");
        System.out.println("(Even THIS println participates: reading a static field is an active");
        System.out.println("use that would trigger class load - module 01. Here the class was");
        System.out.println("already loaded by the first `new`, so nothing new fires.)");
        System.out.println("Static = once per CLASS. Instance = once per OBJECT. That contrast");
        System.out.println("is the whole module in two lines.");
    }

    private static void printLog() {
        for (int i = 0; i < InitOrderLog.LOG.size(); i++) {
            System.out.println("  step " + (i + 1) + ": " + InitOrderLog.LOG.get(i));
        }
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
