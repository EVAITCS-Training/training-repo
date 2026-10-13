package com.evaitcs.corejava.oop;

import java.util.ArrayList;
import java.util.List;

/**
 * The construction order demo — students LOVE seeing this printed step by step.
 *
 * The first time `new Branch(...)` runs, the JVM does, in this exact order:
 *   1. HeadOffice static initializer   (once per JVM, superclass first — module 01
 *                                       showed WHEN a class is initialized: first active use)
 *   2. Branch static initializer       (once per JVM)
 *   3. HeadOffice constructor          (per object — via super(), the first line of Branch's ctor)
 *   4. Branch constructor body         (per object)
 *
 * Statics are per-CLASS and run once; constructors are per-OBJECT and run every time.
 * Create a second Branch and watch the static blocks stay silent — that contrast is
 * the whole lesson.
 */
public class ConstructionOrderDemo {

    // Shared log so the test can read the recorded order as evidence. Package-visible on purpose.
    static final List<String> LOG = new ArrayList<String>();

    public static void main(String[] args) {

        banner("First Branch ever — statics fire, then constructors, parent before child");
        LOG.clear();
        new Branch("Northside");
        printLog();
        System.out.println("  Read it again: static init HeadOffice -> static init Branch");
        System.out.println("  (per class, ONCE), then constructor HeadOffice -> constructor Branch");
        System.out.println("  (per object). super() is WHY the parent's ctor line comes first.");

        banner("Second Branch — the statics stay silent");
        LOG.clear();
        new Branch("Southside");
        printLog();
        System.out.println("  Only constructor lines this time: the classes are already initialized.");
        System.out.println("  Static = once per class; constructor = once per object. (Module 11 unpacks that.)");
    }

    /** The parent half of the family. */
    static class HeadOffice {
        static {
            record("static init: HeadOffice  (runs ONCE, when the class is first actively used)");
        }

        private final String founded;

        HeadOffice() {
            this.founded = "1899";
            record("constructor: HeadOffice  (Object's own ctor already returned before this line)");
        }
    }

    /** The child half: super() first, own work second. */
    static class Branch extends HeadOffice {
        static {
            record("static init: Branch      (AFTER HeadOffice's static init — superclass always first)");
        }

        private final String name;

        Branch(String name) {
            super();            // written explicitly to be seen; it is the compiler's default anyway.
            this.name = name;   // The parent's half of THIS object is fully built before this line runs.
            record("constructor: Branch(" + this.name + ")  (parent half already built)");
        }
    }

    static void record(String step) {
        LOG.add(step);
        System.out.println("  " + step);
    }

    private static void printLog() {
        for (int i = 0; i < LOG.size(); i++) {
            System.out.println("    step " + (i + 1) + ": " + LOG.get(i));
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
