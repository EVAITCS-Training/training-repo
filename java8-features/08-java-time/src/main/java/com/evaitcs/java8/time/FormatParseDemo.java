package com.evaitcs.java8.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * DateTimeFormatter — the thread-safe replacement for SimpleDateFormat.
 *
 * Three things to hammer home:
 *  1. Patterns are the same idea as before ("yyyy-MM-dd", "dd/MM/yyyy HH:mm") but the
 *     formatter is IMMUTABLE: declare it once as a static final constant, share it freely.
 *  2. Formatting is Locale-sensitive — pin the Locale for anything stored or compared,
 *     or the Nigerian server and the dev laptop will disagree about month names.
 *  3. Parsing is STRICT enough to reject impossible dates like 2026-02-30 (Sin 5's fix).
 */
public class FormatParseDemo {

    // Immutable and thread-safe => ONE shared constant is the RECOMMENDED style.
    // The same line with SimpleDateFormat would be the classic production race bug.
    static final DateTimeFormatter UK_STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static void main(String[] args) {
        isoDefaults();
        customPatterns();
        localeMatters();
        zoneInTheOutput();
        roundTripAndStrictness();
    }

    static void isoDefaults() {
        System.out.println("=== 1. ISO-8601 is the default -- and that is a feature ===");
        LocalDate opened = LocalDate.of(2026, 3, 15);
        System.out.println("opened.toString()       = " + opened + "   <- toString() is ALWAYS ISO (yyyy-MM-dd)");
        System.out.println("ISO_LOCAL_DATE.format   = " + DateTimeFormatter.ISO_LOCAL_DATE.format(opened));
        System.out.println("ISO_INSTANT.format      = " + DateTimeFormatter.ISO_INSTANT.format(Instant.parse("2026-03-15T09:30:00Z")));
        System.out.println("=> ISO sorts correctly as plain text: logs and file names stay chronological.");
    }

    static void customPatterns() {
        System.out.println();
        System.out.println("=== 2. Custom patterns: ofPattern ===");
        LocalDateTime appointment = LocalDateTime.of(2026, 3, 15, 17, 30);
        System.out.println("dd/MM/yyyy HH:mm        = " + UK_STAMP.format(appointment));
        System.out.println("yyyy-MM-dd              = " + DateTimeFormatter.ofPattern("yyyy-MM-dd").format(appointment));
        System.out.println("=> Ambiguity trap: '01/02/2026' is 1 February in Lagos-style dd/MM, but 2 January");
        System.out.println("   in US MM/dd. Same string, different money. In STORAGE and APIs, always use ISO.");
    }

    static void localeMatters() {
        System.out.println();
        System.out.println("=== 3. Locale -- pin it or the server picks one for you ===");
        LocalDate opened = LocalDate.of(2026, 3, 15);
        System.out.println("ofPattern(.., Locale.UK)= " + DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK).format(opened));
        System.out.println("ofPattern(.., Locale.FRANCE) = " + DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE).format(opened));
        System.out.println("=> ofPattern WITHOUT a Locale uses the JVM default: fine for console demos,");
        System.out.println("   dangerous for anything persisted or compared across machines.");
    }

    static void zoneInTheOutput() {
        System.out.println();
        System.out.println("=== 4. Formatting a ZonedDateTime -- show the customer WHERE ===");
        ZonedDateTime deadline = ZonedDateTime.of(2026, 3, 16, 16, 0, 0, 0, ZoneId.of("Africa/Lagos"));
        DateTimeFormatter letter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH:mm z (VV)", Locale.UK);
        System.out.println("letter format           = " + letter.format(deadline));
        System.out.println("=> 'z' prints the short zone name (WAT), 'VV' the IANA zone id (Africa/Lagos).");
        System.out.println("   A deadline without its zone is how banks argue about cutoffs.");
    }

    static void roundTripAndStrictness() {
        System.out.println();
        System.out.println("=== 5. Parse round-trip and strictness ===");
        LocalDateTime appointment = LocalDateTime.of(2026, 3, 15, 17, 30);
        String text = UK_STAMP.format(appointment);
        LocalDateTime parsed = LocalDateTime.parse(text, UK_STAMP);
        System.out.println("format -> \"" + text + "\" -> parse = " + parsed + " ; equal to original? " + parsed.equals(appointment));

        try {
            LocalDate.parse("2026-02-30"); // impossible date — SimpleDateFormat swallowed this in Sin 5
        } catch (DateTimeParseException e) {
            System.out.println("LocalDate.parse(\"2026-02-30\") -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        System.out.println("=> Round-trip is exact, and February 30th is REJECTED at the door.");
    }
}
