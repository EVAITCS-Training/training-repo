package com.evaitcs.java8.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Meeting legacy code: the bridges between the old world and java.time.
 *
 * Strategy to state out loud: CONVERT AT THE EDGE (JDBC rows, old APIs, SOAP payloads),
 * then live entirely in java.time inside your domain. The old types should never travel
 * deeper into your code than the adapter layer.
 */
public class LegacyConversionDemo {

    static final ZoneId LAGOS = ZoneId.of("Africa/Lagos");

    public static void main(String[] args) {
        dateToInstantAndBack();
        calendarToZonedDateTime();
        sqlTypes();
    }

    static void dateToInstantAndBack() {
        System.out.println("=== 1. java.util.Date <-> Instant ===");
        Date legacy = new Date(1_774_000_000_000L); // an old service hands us a Date
        Instant instant = legacy.toInstant();       // Java 8 bridge #1: Date -> Instant
        LocalDateTime inLagos = LocalDateTime.ofInstant(instant, LAGOS); // give the instant a human face
        Date back = Date.from(instant);             // Java 8 bridge #2: Instant -> Date

        System.out.println("legacy Date             = " + legacy);
        System.out.println("  .toInstant()          = " + instant);
        System.out.println("  as Lagos wall time    = " + inLagos);
        System.out.println("  Date.from(instant)    = " + back);
        System.out.println("round-trip kept millis? = " + (legacy.getTime() == back.getTime()));
        System.out.println("=> A Date IS an instant on the machine timeline -- that is the honest bridge.");
    }

    static void calendarToZonedDateTime() {
        System.out.println();
        System.out.println("=== 2. GregorianCalendar <-> ZonedDateTime ===");
        Calendar legacyCal = new GregorianCalendar(2026, Calendar.MARCH, 15, 9, 30);
        // (Look at that Calendar.MARCH — the 0-based month trap from Sin 2, one last time.)
        ZonedDateTime zdt = ((GregorianCalendar) legacyCal).toZonedDateTime(); // Java 8 bridge
        GregorianCalendar back = GregorianCalendar.from(zdt);                  // and back again

        System.out.println("legacy Calendar time    = " + legacyCal.getTime());
        System.out.println("  .toZonedDateTime()    = " + zdt + "   <- zone travels WITH it now");
        System.out.println("  GregorianCalendar.from= " + back.getTime());
        System.out.println("millis equal?           = " + (legacyCal.getTimeInMillis() == back.getTimeInMillis()));
        System.out.println("=> Calendar carried a zone but hid it in getTimeZone(); ZonedDateTime makes it visible.");
    }

    static void sqlTypes() {
        System.out.println();
        System.out.println("=== 3. java.sql.Date / Timestamp <-> LocalDate / LocalDateTime ===");
        LocalDate opened = LocalDate.of(2026, 3, 15);
        java.sql.Date sqlDate = java.sql.Date.valueOf(opened);   // Java 8 bridge
        LocalDate back = sqlDate.toLocalDate();                  // Java 8 bridge

        LocalDateTime appointment = LocalDateTime.of(opened, LocalTime.of(9, 30));
        java.sql.Timestamp ts = java.sql.Timestamp.valueOf(appointment);

        System.out.println("LocalDate " + opened + "  ->  java.sql.Date " + sqlDate + "  ->  back " + back
                + " ; equal? " + opened.equals(back));
        System.out.println("LocalDateTime " + appointment + "  ->  Timestamp " + ts);
        System.out.println("  ts.toLocalDateTime()  = " + ts.toLocalDateTime());
        System.out.println("=> JDBC 4.2+ drivers map LocalDate<->DATE directly; these bridges are for the layers");
        System.out.println("   that still speak java.sql. Convert once at the boundary, then stay in java.time.");
    }
}
