package com.evaitcs.corejava.statics;

/**
 * Demo 8 - the closing summary: the whole module as one printed table.
 *
 * Point at it and ask the room the three questions that decide everything:
 *   1. Does this belong to the CLASS or to EACH OBJECT?   (one copy vs one per new)
 *   2. Does it need `this`?                              (statics never have one)
 *   3. When does it run?                                 (class load vs every new)
 */
public class ClassVsInstanceSummaryDemo {

    public static void main(String[] args) {

        banner("Class vs instance - the whole module on one screen");
        row("MEMBER KIND", "WHERE IT LIVES", "HOW MANY COPIES", "ACCESSED VIA", "EXAMPLE");
        row("-----------", "--------------", "---------------", "------------", "-------");
        row("static field", "class area, per class", "ONE, shared by all", "ClassName.FIELD",
                "BankAccount.BANK_NAME");
        row("instance field", "each object on the heap", "one PER object", "reference.field",
                "acct.getBalance()");
        row("static method", "class area; no `this`", "ONE behavior", "ClassName.method()",
                "BankUtils.formatMoney(..)");
        row("instance method", "class area; runs via `this`", "ONE behavior, per object", "reference.method()",
                "acct.deposit(..)");
        row("static nested class", "its own class file", "needs no outer object", "new Outer.Nested()",
                "new Bank.Vault(\"V-01\")");
        row("inner (non-static) class", "own object + hidden this$0", "one per OUTER object", "outer.new Inner()",
                "bank.new Teller(\"Ann\")");

        System.out.println();
        System.out.println("Bonus rows the table cannot fit:");
        System.out.println("  static init  - runs ONCE per class load (superclass first, textual order).");
        System.out.println("  instance init - runs per `new` (superclass first, textual order, ctor last).");
        System.out.println("  static methods are HIDDEN, never overridden (compile-time binding).");
        System.out.println("  static mutable state is GLOBAL state - prefer static final immutable.");
        System.out.println();
        System.out.println("One-sentence takeaway: `static` means PER CLASS - one copy, no object,");
        System.out.println("no `this`; everything else in a class body is PER OBJECT.");
    }

    private static void row(String kind, String where, String copies, String via, String example) {
        System.out.printf("%-24s| %-26s| %-22s| %-21s| %s%n", kind, where, copies, via, example);
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
