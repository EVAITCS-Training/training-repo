package com.evaitcs.corejava.jvm;

import java.util.Arrays;

/**
 * Demo 6 — `public static void main(String[] args)`: the entry point, keyword by keyword.
 *
 * Students type this signature for weeks before anyone explains it. Every word earns its place:
 *
 *   public  — the JVM calls main() from OUTSIDE your class; private main is invisible to it
 *   static  — no object of your class exists yet at startup, so main must belong to the CLASS
 *             (static gets its own module: 11-static-and-class-vs-instance)
 *   void    — nobody receives a result; the program "returns" via its exit code, not a value
 *   String[] args — the words typed after the program name, handed over as strings
 */
public class MainMethodDemo {

    public static void main(String[] args) {

        banner("1. What THIS run received");

        describeArgs(args);
        if (args.length == 0) {
            System.out.println("  Empty — `mvnw ... exec:java` passes no arguments by default.");
            System.out.println("  To pass some: .\\mvnw.cmd -pl 01-jvm-jre-jdk compile exec:java \"-Dexec.args=transfer 250.00\"");
            System.out.println();
            System.out.println("  A launch WITH words would look like this (same handling, real values):");
            describeArgs(new String[] { "transfer", "250.00" });
        }

        banner("2. Why each keyword must be there");

        System.out.println("  public : the JVM starts OUTSIDE your class — it can only call what is visible.");
        System.out.println("           (Fun fact: the JVM spec allows other visibilities; the LAUNCHER requires public.)");
        System.out.println("  static : at startup no instance of MainMethodDemo exists — creating one would need");
        System.out.println("           a constructor call, and something must run FIRST. static breaks the chicken-and-egg.");
        System.out.println("  void   : there is no caller up there to receive a result. Completion is signaled by");
        System.out.println("           returning from main (or System.exit(code) — 0 means success by convention).");
        System.out.println("  String[] args : command-line words arrive as strings; parsing numbers is YOUR job");
        System.out.println("           (Long.parseLong(args[1]) — and handling the crash when it is not a number).");
        System.out.println();
        System.out.println("  The signature is a CONTRACT with the JVM, not Java syntax magic: the launcher looks");
        System.out.println("  for exactly this method. Misspell it (or make it non-static) and you get the classic");
        System.out.println("  'Main method not found in class ...' — the JVM simply did not find the door.");
        System.out.println("  And because main is just a static method, other code CAN call it directly — the");
        System.out.println("  JUnit test in this module looks it up by signature to prove the contract holds.");
    }

    /** What any launch of a bank CLI would do with the words it was handed. */
    private static void describeArgs(String[] args) {
        System.out.println("  args = " + Arrays.toString(args) + "   (length " + args.length + ")");
        if (args.length > 0) {
            System.out.println("    command = " + args[0]);
            if (args.length > 1) {
                System.out.println("    amount  = " + args[1]);
            }
        }
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
