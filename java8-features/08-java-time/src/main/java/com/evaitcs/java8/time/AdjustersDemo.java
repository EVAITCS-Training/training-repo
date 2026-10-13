package com.evaitcs.java8.time;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;

/**
 * TemporalAdjusters — "move this date to the next meaningful one".
 *
 * Instead of hand-rolling +1/+2/+7 loops to find "next Friday" or "last day of month",
 * java.time ships named adjusters. You apply one with date.with(adjuster), and — because
 * everything is immutable — you get a NEW date back. We even build a custom business-day
 * adjuster out of a lambda (module 01 paying off).
 */
public class AdjustersDemo {

    public static void main(String[] args) {
        builtInAdjusters();
        firstBusinessDayCustom();
    }

    static void builtInAdjusters() {
        System.out.println("=== Built-in adjusters: date.with(TemporalAdjusters.xxx) ===");
        // Fixed reference date so the printed weekdays are identical every run.
        LocalDate payday = LocalDate.of(2026, 3, 15); // happens to be a Sunday
        System.out.println("reference date          = " + payday + " (" + payday.getDayOfWeek() + ")");
        System.out.println("  next(FRIDAY)          = " + payday.with(TemporalAdjusters.next(DayOfWeek.FRIDAY)));
        System.out.println("  nextOrSame(FRIDAY)    = " + payday.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)));
        System.out.println("  previous(FRIDAY)      = " + payday.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY)));
        System.out.println("  lastDayOfMonth()      = " + payday.with(TemporalAdjusters.lastDayOfMonth()));
        System.out.println("  firstDayOfNextMonth() = " + payday.with(TemporalAdjusters.firstDayOfNextMonth()));
        System.out.println("  dayOfWeekInMonth(3,FRIDAY) = " + payday.with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.FRIDAY))
                + "  <- 3rd Friday (settlement batch day)");
        System.out.println("=> next(FRIDAY) from a Sunday skips ahead; nextOrSame would STAY if it were already Friday.");
    }

    static void firstBusinessDayCustom() {
        System.out.println();
        System.out.println("=== Custom adjuster: 'first BUSINESS day of month' (salary day) ===");
        TemporalAdjuster firstBusinessDay = firstBusinessDayOfMonth();
        // Show it across months whose 1st lands on different weekdays, including a weekend.
        for (int month = 1; month <= 6; month++) {
            LocalDate first = LocalDate.of(2026, month, 1);
            LocalDate salaryDay = first.with(firstBusinessDay);
            System.out.println("  " + first + " (" + first.getDayOfWeek() + ") -> salary day "
                    + salaryDay + " (" + salaryDay.getDayOfWeek() + ")");
        }
        System.out.println("=> Months starting Sat/Sun roll forward to Monday; the rest stay put.");
    }

    /**
     * A custom adjuster is just a TemporalAdjuster — and ofDateAdjuster takes a
     * UnaryOperator&lt;LocalDate&gt;, i.e. a lambda. This is module 01 meeting module 08.
     */
    static TemporalAdjuster firstBusinessDayOfMonth() {
        return TemporalAdjusters.ofDateAdjuster(date -> {
            LocalDate day = date.withDayOfMonth(1); // start at the 1st
            DayOfWeek dow = day.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY) {
                return day.plusDays(2); // Sat -> Mon
            }
            if (dow == DayOfWeek.SUNDAY) {
                return day.plusDays(1); // Sun -> Mon
            }
            return day; // already a weekday
        });
    }
}
