package com.evaitcs.java8.time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Why java.time exists: a runnable tour of the OLD API's sins.
 *
 * Everything here is code you will still meet in legacy banking systems — the point is
 * not "Date is stupid", it is "here is the exact SHAPE of the bug each design decision
 * produces", so you recognize it in a code review instead of in a post-mortem.
 */
public class OldApiProblemsDemo {

    public static void main(String[] args) {
        mutableDate();
        calendarMonths();
        year1900Constructor();
        sqlDateLie();
        lenientSimpleDateFormat();

        System.out.println();
        System.out.println("=> Every trap above disappears in java.time: immutable values, 1-based");
        System.out.println("   months, no mystery offsets, strict parsing. The rest of the module proves it.");
    }

    static void mutableDate() {
        System.out.println("=== Sin 1: java.util.Date is MUTABLE ===");
        // One Date object shared between the account service and "some other team's" code.
        // Fixed millis => identical printed output on every run (demos must be deterministic).
        Date accountOpened = new Date(1_774_123_456_789L);
        System.out.println("account service sees : " + accountOpened);

        // Hand the SAME object to a routine that 'just normalizes the timestamp'.
        truncateToStartOfDay(accountOpened);

        System.out.println("after the audit call : " + accountOpened);
        System.out.println("=> The caller's object was rewritten from the inside. With Date you must");
        System.out.println("   defensively copy at every hand-off. java.time values CANNOT be changed at all.");
    }

    /** Looks harmless in a code review — and quietly mutates a SHARED object. */
    static void truncateToStartOfDay(Date d) {
        Calendar c = new GregorianCalendar();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        d.setTime(c.getTimeInMillis()); // <-- the mutation the caller never asked for
    }

    static void calendarMonths() {
        System.out.println();
        System.out.println("=== Sin 2: Calendar months are 0-BASED ===");
        Calendar christmas = new GregorianCalendar(2026, Calendar.DECEMBER, 25);
        System.out.println("Calendar.DECEMBER constant = " + Calendar.DECEMBER + "   <-- eleven means December!");
        System.out.println("christmas.getTime()        = " + christmas.getTime());

        // The classic accident: someone reads a 1-BASED month from a config file or a
        // spreadsheet and hands it straight to Calendar. Calendar is LENIENT by default,
        // so it does not complain — it silently rolls the overflow into the next year.
        Calendar oops = new GregorianCalendar(2026, 13, 5);
        System.out.println("new GregorianCalendar(2026, 13, 5) = " + oops.getTime());
        System.out.println("=> Month 13 became FEBRUARY 2027. java.time rejects month 13 with a");
        System.out.println("   DateTimeException at the point of the mistake, not three systems later.");
    }

    @SuppressWarnings("deprecation") // we are DEMONSTRATING the deprecated trap on purpose
    static void year1900Constructor() {
        System.out.println();
        System.out.println("=== Sin 3: new Date(year, month, day) -- two offsets in ONE constructor ===");
        // year means "year MINUS 1900", and month is 0-based AGAIN.
        Date idesOfMarch = new Date(126, 2, 15); // 126 = 2026-1900; 2 = MARCH, not February
        System.out.println("new Date(126, 2, 15) = " + idesOfMarch);
        System.out.println("=> Write new Date(2026, ...) 'because that is the year' and you get the year 3926.");
        System.out.println("   It is deprecated for a reason -- but it still runs, and it still ships.");
    }

    static void sqlDateLie() {
        System.out.println();
        System.out.println("=== Sin 4: java.sql.Date PRETENDS to be date-only ===");
        java.sql.Date opening = java.sql.Date.valueOf("2026-03-15");
        System.out.println("opening                 = " + opening + "   (toString shows only a date -- looks clean)");
        System.out.println("opening.getTime()       = " + opening.getTime() + "  <-- really a util.Date holding MILLIS");
        System.out.println("instanceof util.Date?   = " + (opening instanceof java.util.Date));
        try {
            opening.getHours();
        } catch (IllegalArgumentException e) {
            System.out.println("opening.getHours()      -> " + e.getClass().getSimpleName());
        }
        System.out.println("=> A type that extends Date, HIDES the time part, and THROWS if you ask for it.");
        System.out.println("   LocalDate is genuinely date-only: nothing hidden, nothing thrown.");
    }

    static void lenientSimpleDateFormat() {
        System.out.println();
        System.out.println("=== Sin 5: SimpleDateFormat -- lenient by default, and NOT thread-safe ===");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        System.out.println("sdf.isLenient() = " + sdf.isLenient());
        try {
            Date parsed = sdf.parse("2026-02-30"); // there is no 30th of February
            System.out.println("parse(\"2026-02-30\") silently became : " + sdf.format(parsed));
        } catch (ParseException e) {
            throw new IllegalStateException("lenient SDF should never throw here", e);
        }
        System.out.println("=> February 2026 has 28 days; the extra 2 days rolled into MARCH. A due-date");
        System.out.println("   typo just moved real money two days. DateTimeFormatter is strict by default.");
        System.out.println();
        System.out.println("Thread-safety (explained, not raced -- a flaky demo teaches nothing):");
        System.out.println("  SimpleDateFormat keeps ONE mutable Calendar inside. Two threads calling");
        System.out.println("  format() on a SHARED instance interleave writes to that Calendar and emit");
        System.out.println("  wrong strings (or ArrayIndexOutOfBoundsException) at random, under load,");
        System.out.println("  in production only. The classic bug is a shared `static final SimpleDateFormat`.");
        System.out.println("  The fix is DateTimeFormatter: immutable, thread-safe, MEANT to be a constant.");
    }
}
