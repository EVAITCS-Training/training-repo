package com.evaitcs.java8.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Two DIFFERENT ideas of "how much time", and mixing them up is a classic bug.
 *
 *   Duration = MACHINE time. An exact number of seconds/nanos. Lives on the timeline
 *              of Instants. "3600 seconds" always means the same wall-clock span.
 *   Period   = CALENDAR time. Years/months/days. Lives on LocalDate. "1 month" does
 *              NOT mean a fixed number of seconds — February and March differ.
 *
 * The demo also shows why Duration.between(twoLocalDates) fails, and why DST makes
 * "24 hours" and "1 day" different things.
 */
public class DurationVsPeriodDemo {

    static final ZoneId LONDON = ZoneId.of("Europe/London");

    public static void main(String[] args) {
        machineTime();
        humanTime();
        theMixUp();
        dstMakes24HoursLie();
        chronoUnitTour();
    }

    static void machineTime() {
        System.out.println("=== Duration -- MACHINE time between two Instants ===");
        Instant login = Instant.parse("2026-03-15T09:00:00Z");
        Instant logout = Instant.parse("2026-03-15T10:30:00Z");
        Duration session = Duration.between(login, logout);
        System.out.println("login                   = " + login);
        System.out.println("logout                  = " + logout);
        System.out.println("Duration.between(..)    = " + session + "   (ISO-8601: PT1H30M)");
        System.out.println("  .toMinutes()          = " + session.toMinutes());
        System.out.println("=> Exact seconds. Perfect for session lengths, timeouts, SLA clocks.");
    }

    static void humanTime() {
        System.out.println();
        System.out.println("=== Period -- CALENDAR time between two LocalDates ===");
        LocalDate opened = LocalDate.of(2020, 1, 31);
        LocalDate reviewed = LocalDate.of(2026, 3, 15);
        Period age = Period.between(opened, reviewed);
        System.out.println("opened                  = " + opened);
        System.out.println("reviewed                = " + reviewed);
        System.out.println("Period.between(..)      = " + age + "   -> " + age.getYears() + "y " + age.getMonths() + "m " + age.getDays() + "d");
        System.out.println("=> 'How long has this account been open?' is a CALENDAR question -- answer in Period.");
    }

    static void theMixUp() {
        System.out.println();
        System.out.println("=== Why Duration.between(two LocalDates) blows up ===");
        LocalDate opened = LocalDate.of(2026, 1, 15);
        LocalDate reviewed = LocalDate.of(2026, 3, 20);
        try {
            Duration.between(opened, reviewed); // <-- the tempting mistake
        } catch (RuntimeException e) {
            System.out.println("Duration.between(LocalDate,LocalDate) -> " + e.getClass().getSimpleName());
            System.out.println("  reason: " + e.getMessage());
        }
        System.out.println("=> Duration speaks SECONDS/NANOS; a LocalDate has no time-of-day to convert, so there");
        System.out.println("   is no honest answer to 'how many seconds between two calendar days?'. Use the right tool:");
        System.out.println("   Period.between  = " + Period.between(opened, reviewed));
        System.out.println("   ChronoUnit.DAYS = " + ChronoUnit.DAYS.between(opened, reviewed) + " days   (a plain count)");
        System.out.println("   ChronoUnit.MONTHS = " + ChronoUnit.MONTHS.between(opened, reviewed)
                + " months  <-- counts COMPLETE months only; it TRUNCATES, it does not round");
    }

    static void dstMakes24HoursLie() {
        System.out.println();
        System.out.println("=== DST: 'plus 1 day' != 'plus 24 hours' ===");
        // London springs forward on 2026-03-29, so that calendar day is only 23 hours long.
        ZonedDateTime saturday = ZonedDateTime.of(2026, 3, 28, 17, 0, 0, 0, LONDON);
        ZonedDateTime plusOneDay = saturday.plusDays(1);             // calendar: same wall time next day
        ZonedDateTime plus24Hours = saturday.plus(Duration.ofHours(24)); // machine: exactly 24h later

        System.out.println("start                   = " + saturday + " (offset " + saturday.getOffset() + ")");
        System.out.println("  .plusDays(1)          = " + plusOneDay + " (offset " + plusOneDay.getOffset() + ")");
        System.out.println("  .plus(24 hours)       = " + plus24Hours);
        System.out.println("real span of that 'day' = " + Duration.between(saturday.toInstant(), plusOneDay.toInstant()));
        System.out.println("=> Interest/penalty maths must CHOOSE consciously: machine time (Duration) or");
        System.out.println("   calendar time (Period/plusDays). On a DST boundary they disagree by an hour.");
    }

    static void chronoUnitTour() {
        System.out.println();
        System.out.println("=== ChronoUnit -- the enum of human/machine time units ===");
        LocalDate opened = LocalDate.of(2026, 1, 15);
        LocalDate reviewed = LocalDate.of(2026, 3, 20);
        for (ChronoUnit unit : new ChronoUnit[]{ChronoUnit.DAYS, ChronoUnit.WEEKS, ChronoUnit.MONTHS, ChronoUnit.YEARS}) {
            System.out.println("  " + pad(unit.name()) + " between " + opened + " and " + reviewed + " = " + unit.between(opened, reviewed));
        }
        System.out.println("=> One enum, one .between(), every scale. WEEKS/MONTHS/YEARS all TRUNCATE to whole units.");
    }

    private static String pad(String s) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < 7) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
