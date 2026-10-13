package com.evaitcs.corejava.statics;

import java.math.BigDecimal;

/**
 * Demo 1 - THE core picture: static field = ONE slot in the class area shared by
 * every instance; instance field = one slot PER object on the heap.
 *
 * Three accounts get built; each prints its OWN balance (per-object) next to its
 * view of the counter (shared). Then a fourth account mutates the static - and all
 * three "old" objects immediately report the new value, because there was never a
 * per-object copy to go stale.
 */
public class SharedStaticStateDemo {

    public static void main(String[] args) {

        banner("1. Three accounts, ONE counter - every object sees the SAME static slot");
        BankAccount a1 = new BankAccount("Ann", new BigDecimal("100.00"));
        BankAccount a2 = new BankAccount("Bo", new BigDecimal("25.50"));
        BankAccount a3 = BankAccount.createSavings("Cy");
        System.out.println("a1 -> " + a1.describe());
        System.out.println("a2 -> " + a2.describe());
        System.out.println("a3 -> " + a3.describe());
        System.out.println("The balances DIFFER (instance data: one slot per object).");
        System.out.println("The counter is IDENTICAL in all three views - it is ONE slot,");
        System.out.println("living in the class area, not copied into any object.");

        banner("2. Mutate the static - EVERY instance sees it instantly");
        BankAccount a4 = new BankAccount("Dana", new BigDecimal("7.00")); // ticks the shared counter
        System.out.println("built a4; now the OLD objects report:");
        System.out.println("a1 -> " + a1.describe());
        System.out.println("a2 -> " + a2.describe());
        System.out.println("a3 -> " + a3.describe());
        System.out.println("Nothing was 'sent' to a1/a2/a3. They were all reading the same");
        System.out.println("slot the whole time - a4's construction changed THAT slot.");

        banner("3. Style rule: read/write statics through the CLASS NAME");
        int viaClass = BankAccount.getAccountCount();   // says what it IS: class-level
        int viaInstance = a1.getAccountCount();         // compiles, runs identically...
        String nameViaInstance = a1.BANK_NAME;          // ...and so does this field access.
        System.out.println("BankAccount.getAccountCount() = " + viaClass);
        System.out.println("a1.getAccountCount()          = " + viaInstance + "  (same value, of course)");
        System.out.println("a1.BANK_NAME                  = " + nameViaInstance);
        System.out.println("The instance forms LIE about what is happening: nothing about");
        System.out.println("a1 is involved - a1 could even be null and the call would still");
        System.out.println("work! Your IDE flags it ('static member accessed via instance");
        System.out.println("reference'). Write BankAccount.getAccountCount(). Always.");
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
