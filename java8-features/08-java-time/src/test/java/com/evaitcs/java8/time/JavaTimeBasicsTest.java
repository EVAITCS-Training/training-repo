package com.evaitcs.java8.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 08-java-time test
 * Each test proves ONE claim, prints its evidence, and asserts it.
 * All expectations derive from FIXED dates — nothing here depends on 'today'.
 */
class JavaTimeBasicsTest {

    @Test
    @DisplayName("immutability: plusX/withX return a NEW object, the original never changes")
    void everyOperationReturnsANewObject() {
        LocalDate opened = LocalDate.of(2026, 1, 31);
        LocalDate next = opened.plusDays(1);

        assertNotSame(opened, next, "a NEW object must come back");
        assertEquals(LocalDate.of(2026, 1, 31), opened, "original untouched");
        assertEquals(LocalDate.of(2026, 2, 1), next);

        // The classic bug, proven harmless-looking but result-free:
        LocalDate due = LocalDate.of(2026, 6, 30);
        due.plusDays(1); // <-- result discarded on purpose
        assertEquals(LocalDate.of(2026, 6, 30), due, "discarding the result changes NOTHING");

        System.out.println("opened = " + opened + " ; opened.plusDays(1) = " + next);
        System.out.println("after `due.plusDays(1);` (discarded) due is STILL " + due);
        System.out.println("=> immutability is also WHY these types are thread-safe: no shared state to corrupt");
    }

    @Test
    @DisplayName("month numbering is human: 12 = December, and month 13 is REJECTED not rolled")
    void monthNumberingIsHuman() {
        LocalDate christmas = LocalDate.of(2026, 12, 25);
        assertEquals(12, christmas.getMonthValue());
        assertEquals(Month.DECEMBER, christmas.getMonth());

        assertThrows(DateTimeException.class, () -> LocalDate.of(2026, 13, 25),
                "month 13 must explode at the point of the mistake");

        System.out.println("LocalDate.of(2026, 12, 25).getMonthValue() = " + christmas.getMonthValue()
                + "  (Calendar would have called December '11')");
        System.out.println("LocalDate.of(2026, 13, 25) throws DateTimeException -- no silent rollover");
    }

    @Test
    @DisplayName("arithmetic clamps to real calendar dates: Jan 31 + 1 month = Feb 28 (Feb 29 in a leap year)")
    void arithmeticClampsToRealCalendarDates() {
        assertEquals(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 1, 31).plusMonths(1));
        assertEquals(LocalDate.of(2024, 2, 29), LocalDate.of(2024, 1, 31).plusMonths(1), "2024 is a leap year");

        // The LoanScheduleDemo rule: disbursement + N months, snapped to end of month.
        LocalDate maturity = LocalDate.of(2026, 1, 30).plusMonths(6).with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(LocalDate.of(2026, 7, 31), maturity);

        System.out.println("2026-01-31 .plusMonths(1) = " + LocalDate.of(2026, 1, 31).plusMonths(1));
        System.out.println("2024-01-31 .plusMonths(1) = " + LocalDate.of(2024, 1, 31).plusMonths(1) + " (leap)");
        System.out.println("2026-01-30 .plusMonths(6).with(lastDayOfMonth) = " + maturity);
    }

    @Test
    @DisplayName("Duration vs Period: Duration.between(LocalDates) is UNSUPPORTED; Period/ChronoUnit answer")
    void durationCannotSpanLocalDatesButPeriodCan() {
        LocalDate opened = LocalDate.of(2026, 1, 15);
        LocalDate reviewed = LocalDate.of(2026, 3, 20);

        // Duration speaks seconds/nanos; a LocalDate has no time-of-day, so there is no honest answer.
        // The exception name says it all: the type simply does not support that UNIT.
        UnsupportedTemporalTypeException failure =
                assertThrows(UnsupportedTemporalTypeException.class, () -> Duration.between(opened, reviewed));

        Period age = Period.between(opened, reviewed);
        assertEquals(Period.of(0, 2, 5), age, "2 months and 5 days of CALENDAR time");
        assertEquals(64, ChronoUnit.DAYS.between(opened, reviewed), "or a plain count of days");
        assertEquals(2, ChronoUnit.MONTHS.between(opened, reviewed), "COMPLETE months only — it truncates");

        System.out.println("Duration.between(" + opened + ", " + reviewed + ") -> "
                + failure.getClass().getSimpleName() + ": " + failure.getMessage());
        System.out.println("Period.between   = " + age + " ; ChronoUnit.DAYS = "
                + ChronoUnit.DAYS.between(opened, reviewed) + " ; ChronoUnit.MONTHS = "
                + ChronoUnit.MONTHS.between(opened, reviewed));
    }

    @Test
    @DisplayName("Duration IS the right tool for machine time between Instants")
    void durationAnswersMachineQuestions() {
        Instant login = Instant.parse("2026-03-15T09:00:00Z");
        Instant logout = Instant.parse("2026-03-15T10:30:00Z");

        Duration session = Duration.between(login, logout);
        assertEquals(Duration.ofMinutes(90), session);
        assertEquals(90, session.toMinutes());

        System.out.println("Duration.between(" + login + ", " + logout + ") = " + session
                + " = " + session.toMinutes() + " minutes");
    }

    @Test
    @DisplayName("same wall clock, different zone => DIFFERENT instants (whose 5pm?)")
    void zoneDecidesTheInstant() {
        LocalDateTime fivePm = LocalDateTime.of(2026, 3, 15, 17, 0);
        ZonedDateTime lagos = fivePm.atZone(ZoneId.of("Africa/Lagos"));
        ZonedDateTime london = fivePm.atZone(ZoneId.of("Europe/London"));

        // 2026-03-15: Lagos is +01:00 year-round; London is still on GMT (UK DST starts 29 Mar 2026).
        assertEquals(ZoneOffset.ofHours(1), lagos.getOffset());
        assertEquals(ZoneOffset.UTC, london.getOffset());
        assertEquals(Instant.parse("2026-03-15T16:00:00Z"), lagos.toInstant());
        assertEquals(Instant.parse("2026-03-15T17:00:00Z"), london.toInstant());
        assertNotEquals(lagos.toInstant(), london.toInstant(), "same wall clock, DIFFERENT moments");

        System.out.println("Lagos  5pm = " + lagos + " -> " + lagos.toInstant());
        System.out.println("London 5pm = " + london + " -> " + london.toInstant());
        System.out.println("=> the two 'same' 5pms are " + Duration.between(lagos.toInstant(), london.toInstant()) + " apart");
    }

    @Test
    @DisplayName("format/parse round-trip is exact with a pinned pattern")
    void formatParseRoundTrip() {
        DateTimeFormatter uk = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        LocalDateTime appointment = LocalDateTime.of(2026, 3, 15, 17, 30);

        String text = uk.format(appointment);
        assertEquals("15/03/2026 17:30", text);
        assertEquals(appointment, LocalDateTime.parse(text, uk), "parse(format(x)) == x");

        System.out.println("format -> \"" + text + "\" -> parse -> " + LocalDateTime.parse(text, uk));
    }

    @Test
    @DisplayName("custom adjuster: first business day skips Sat/Sun and keeps weekdays")
    void firstBusinessDayAdjusterSkipsWeekends() {
        TemporalAdjuster firstBusinessDay = AdjustersDemo.firstBusinessDayOfMonth();

        LocalDate march = LocalDate.of(2026, 3, 1).with(firstBusinessDay);   // 1 Mar 2026 = Sunday
        LocalDate august = LocalDate.of(2026, 8, 1).with(firstBusinessDay);  // 1 Aug 2026 = Saturday
        LocalDate april = LocalDate.of(2026, 4, 1).with(firstBusinessDay);   // 1 Apr 2026 = Wednesday

        assertEquals(LocalDate.of(2026, 3, 2), march);
        assertEquals(LocalDate.of(2026, 8, 3), august);
        assertEquals(LocalDate.of(2026, 4, 1), april, "a weekday 1st must not move");

        System.out.println("Sun 2026-03-01 -> " + march + " (" + march.getDayOfWeek() + ")");
        System.out.println("Sat 2026-08-01 -> " + august + " (" + august.getDayOfWeek() + ")");
        System.out.println("Wed 2026-04-01 -> " + april + " (" + april.getDayOfWeek() + ")");
    }

    @Test
    @DisplayName("legacy bridges round-trip: Date<->Instant, sql.Date<->LocalDate, Calendar->ZonedDateTime")
    void legacyBridgesRoundTrip() {
        Instant instant = Instant.parse("2026-03-15T09:30:00Z");
        Date legacy = Date.from(instant);
        assertEquals(instant, legacy.toInstant(), "Date <-> Instant is lossless");

        LocalDate opened = LocalDate.of(2026, 3, 15);
        assertEquals(opened, java.sql.Date.valueOf(opened).toLocalDate(), "sql.Date <-> LocalDate is lossless");

        GregorianCalendar cal = new GregorianCalendar(2026, Calendar.MARCH, 15);
        assertEquals(opened, cal.toZonedDateTime().toLocalDate(), "Calendar -> ZonedDateTime keeps the date");

        System.out.println("Instant " + instant + " -> Date -> Instant = " + legacy.toInstant());
        System.out.println("LocalDate " + opened + " -> java.sql.Date -> LocalDate = " + java.sql.Date.valueOf(opened).toLocalDate());
        System.out.println("GregorianCalendar(2026, MARCH, 15).toZonedDateTime() = " + cal.toZonedDateTime());
    }
}
