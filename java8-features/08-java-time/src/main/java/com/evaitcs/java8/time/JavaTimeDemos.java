package com.evaitcs.java8.time;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 08-java-time compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 08-java-time compile exec:java "-Dexec.mainClass=com.evaitcs.java8.time.ImmutabilityDemo"
 */
public class JavaTimeDemos {

    public static void main(String[] args) {
        section("OldApiProblemsDemo — the old API's sins, runnable");
        OldApiProblemsDemo.main(args);

        section("CoreTypesTourDemo — Instant, LocalDate/Time/DateTime, ZonedDateTime, OffsetDateTime");
        CoreTypesTourDemo.main(args);

        section("ImmutabilityDemo — withX/plusX return NEW objects");
        ImmutabilityDemo.main(args);

        section("DurationVsPeriodDemo — machine time vs calendar time");
        DurationVsPeriodDemo.main(args);

        section("AdjustersDemo — next(FRIDAY), lastDayOfMonth, first business day");
        AdjustersDemo.main(args);

        section("FormatParseDemo — patterns, Locale, zone output, round-trip");
        FormatParseDemo.main(args);

        section("LegacyConversionDemo — Date/Calendar/sql bridges");
        LegacyConversionDemo.main(args);

        section("LoanScheduleDemo — the whole module on one SecureBank loan");
        LoanScheduleDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
