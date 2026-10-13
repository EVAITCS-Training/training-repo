package com.evaitcs.java8.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims in OldApiProblemsDemo. Run with:
 *   .\mvnw.cmd -pl 08-java-time test
 * Each test prints the evidence it asserts, so the test run doubles as a demo.
 * All dates are FIXED constants — nothing here depends on 'today'.
 */
class OldApiTrapsTest {

    @Test
    @DisplayName("java.util.Date is MUTABLE: shared code can rewrite your object in place")
    void utilDateIsMutableUnderSharedCode() {
        Date accountOpened = new Date(1_774_123_456_789L);
        long before = accountOpened.getTime();

        // The 'harmless normalizer' from the demo — it never reassigns our variable,
        // yet our object changes. That is the definition of the mutability trap.
        OldApiProblemsDemo.truncateToStartOfDay(accountOpened);

        assertNotEquals(before, accountOpened.getTime(), "the caller's Date was mutated in place");
        System.out.println("millis before = " + before);
        System.out.println("millis after  = " + accountOpened.getTime() + "  <- changed WITHOUT any reassignment");
        System.out.println("=> an immutable type (LocalDate, Instant...) makes this bug impossible to write");
    }

    @Test
    @DisplayName("Calendar months are 0-BASED, and month 13 silently rolls into next year")
    void calendarMonthsAreZeroBasedAndRollOver() {
        assertEquals(11, Calendar.DECEMBER, "the constant itself proves the numbering");

        Calendar christmas = new GregorianCalendar(2026, 11, 25);
        assertEquals(2026, christmas.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, christmas.get(Calendar.MONTH), "month 11 = December");

        // The accident: a 1-BASED month from a config file, handed to a LENIENT Calendar.
        Calendar oops = new GregorianCalendar(2026, 13, 5);
        assertEquals(2027, oops.get(Calendar.YEAR), "month 13 rolled the YEAR forward — no exception");
        assertEquals(Calendar.FEBRUARY, oops.get(Calendar.MONTH), "month 13 = February of 2027");

        System.out.println("Calendar(2026, 11, 25) = December, and Calendar(2026, 13, 5) = "
                + oops.get(Calendar.YEAR) + "-02-05 (rolled over silently)");
        System.out.println("java.time numbering  : LocalDate.of(2026, 12, 25).getMonth() = "
                + LocalDate.of(2026, 12, 25).getMonth());
        assertEquals(Month.DECEMBER, LocalDate.of(2026, 12, 25).getMonth(), "12 = December, like humans write it");
    }

    @Test
    @SuppressWarnings("deprecation") // demonstrating the deprecated trap ON PURPOSE
    @DisplayName("new Date(year, month, day) hides TWO offsets: year-1900 AND 0-based month")
    void deprecatedDateConstructorHidesTwoOffsets() {
        Date ides = new Date(126, 2, 15); // 'means' 2026-03-15

        Calendar c = new GregorianCalendar();
        c.setTime(ides);
        assertEquals(2026, c.get(Calendar.YEAR), "year param 126 means 126 + 1900");
        assertEquals(Calendar.MARCH, c.get(Calendar.MONTH), "month param 2 = MARCH because 0-based");
        assertEquals(15, c.get(Calendar.DAY_OF_MONTH));

        System.out.println("new Date(126, 2, 15) = year " + c.get(Calendar.YEAR) + ", month "
                + (c.get(Calendar.MONTH) + 1) + ", day " + c.get(Calendar.DAY_OF_MONTH));
        System.out.println("=> write new Date(2026, ...) and you get the year 3926");
    }

    @Test
    @DisplayName("SimpleDateFormat is LENIENT: 2026-02-30 silently becomes 2026-03-02")
    void simpleDateFormatIsLenientByDefault() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        assertTrue(sdf.isLenient(), "lenient is the DEFAULT");

        Date parsed = sdf.parse("2026-02-30"); // there is no 30th of February
        assertEquals("2026-03-02", sdf.format(parsed), "the extra days rolled into March");
        System.out.println("lenient parse of 2026-02-30 -> " + sdf.format(parsed));

        sdf.setLenient(false);
        assertThrows(ParseException.class, () -> sdf.parse("2026-02-30"),
                "strict mode rejects it — but you have to KNOW to ask");
        System.out.println("setLenient(false): same input now throws " + ParseException.class.getSimpleName());

        // The java.time side of the same claim: strict WITHOUT being asked.
        assertThrows(DateTimeParseException.class, () -> LocalDate.parse("2026-02-30"));
        System.out.println("LocalDate.parse(\"2026-02-30\") throws DateTimeParseException by DEFAULT");
    }
}
