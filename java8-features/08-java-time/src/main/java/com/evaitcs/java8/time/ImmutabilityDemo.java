package com.evaitcs.java8.time;

import java.time.LocalDate;

/**
 * Every java.time type is IMMUTABLE: withX/plusX/minusX never touch the original —
 * they RETURN A NEW OBJECT.
 *
 * Why the class should care: this is the exact property that makes the types thread-safe
 * (contrast Sin 1 in OldApiProblemsDemo, where a shared Date was rewritten from inside),
 * and it introduces the single most common java.time bug: calling plusDays(1) and
 * throwing the result away.
 */
public class ImmutabilityDemo {

    // Immutable + thread-safe => sharing ONE instance everywhere is safe and idiomatic.
    // As a java.util.Date this constant would be a loaded gun: any caller could setTime() it.
    static final LocalDate FISCAL_YEAR_START = LocalDate.of(2026, 4, 1);

    public static void main(String[] args) {
        everyOperationReturnsANewObject();
        theClassicBug();
        whyThisMeansThreadSafe();
    }

    static void everyOperationReturnsANewObject() {
        System.out.println("=== 1. The original NEVER changes ===");
        LocalDate accountOpened = LocalDate.of(2026, 1, 31);

        LocalDate nextDay = accountOpened.plusDays(1);
        LocalDate nextMonth = accountOpened.plusMonths(1);
        LocalDate nextYear = accountOpened.withYear(2027);
        LocalDate startOfMonth = accountOpened.withDayOfMonth(1);

        System.out.println("accountOpened           = " + accountOpened + "   <-- unchanged after ALL FOUR calls");
        System.out.println("  .plusDays(1)          = " + nextDay);
        System.out.println("  .plusMonths(1)        = " + nextMonth + "   (31 Jan + 1 month clamps to 28 Feb -- see DurationVsPeriodDemo)");
        System.out.println("  .withYear(2027)       = " + nextYear);
        System.out.println("  .withDayOfMonth(1)    = " + startOfMonth);
        System.out.println("same object?            = " + (accountOpened == nextDay) + "   (every call built a NEW LocalDate)");
    }

    static void theClassicBug() {
        System.out.println();
        System.out.println("=== 2. The classic bug: ignoring the return value ===");
        LocalDate dueDate = LocalDate.of(2026, 6, 30);

        dueDate.plusDays(1); // <-- the new object is created... and falls on the floor

        System.out.println("dueDate.plusDays(1);    // result discarded");
        System.out.println("dueDate                 = " + dueDate + "   <-- STILL 2026-06-30.");
        System.out.println("=> No exception, no warning -- just a late fee that never fired.");
        System.out.println("   There is no setter to call; the ONLY way to 'change' a date is to KEEP the new one:");

        dueDate = dueDate.plusDays(1);
        System.out.println("dueDate = dueDate.plusDays(1);  ->  " + dueDate);
    }

    static void whyThisMeansThreadSafe() {
        System.out.println();
        System.out.println("=== 3. Why immutability == thread-safety ===");
        // Two 'threads' (here: two calls) deriving different things from the SAME shared constant.
        LocalDate q2Report = FISCAL_YEAR_START.plusMonths(3);
        LocalDate q3Report = FISCAL_YEAR_START.plusMonths(6);

        System.out.println("shared constant         = " + FISCAL_YEAR_START);
        System.out.println("thread A derived        = " + q2Report);
        System.out.println("thread B derived        = " + q3Report);
        System.out.println("constant afterwards     = " + FISCAL_YEAR_START + "   <-- cannot be corrupted, ever");
        System.out.println("=> Each caller gets its OWN new object; the shared one has nothing mutable to race on.");
        System.out.println("   That is why static final LocalDate constants are fine, and static final Date is a bug.");
    }
}
