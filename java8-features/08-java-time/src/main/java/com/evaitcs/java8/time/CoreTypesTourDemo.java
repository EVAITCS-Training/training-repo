package com.evaitcs.java8.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The core types of java.time, each answering ONE question.
 *
 * The teaching thread: pick the type by the QUESTION you are asking —
 * "when, for machines?" (Instant), "what day, for humans?" (LocalDate),
 * "what day+time WHERE?" (ZonedDateTime), and so on. Wrong type = wrong answer,
 * not just ugly code.
 */
public class CoreTypesTourDemo {

    // ZoneId strings come from the IANA tz database — always the region/city form.
    // Three-letter abbreviations like "WAT" or "EST" are ambiguous and REJECTED by of().
    static final ZoneId LAGOS = ZoneId.of("Africa/Lagos");
    static final ZoneId LONDON = ZoneId.of("Europe/London");

    public static void main(String[] args) {
        instant();
        localDate();
        localTimeAndDateTime();
        zonedDateTime();
        offsetDateTime();
    }

    static void instant() {
        System.out.println("=== Instant -- a point on the MACHINE timeline (always UTC) ===");
        // Fixed input => deterministic output. In real code: Instant.now().
        Instant auditStamp = Instant.parse("2026-03-15T09:30:15.500Z");
        System.out.println("auditStamp              = " + auditStamp);
        System.out.println("  .getEpochSecond()     = " + auditStamp.getEpochSecond() + "  (seconds since 1970-01-01T00:00Z)");
        System.out.println("  .getNano()            = " + auditStamp.getNano() + "  (nanos WITHIN that second)");
        System.out.println("=> No zone, no calendar, no ambiguity. Store THIS for audit logs and token expiry.");
    }

    static void localDate() {
        System.out.println();
        System.out.println("=== LocalDate -- a human calendar date, no time, no zone ===");
        LocalDate opened = LocalDate.of(2026, 3, 15); // 3 means MARCH: months are 1-BASED, like humans write them
        System.out.println("account opened          = " + opened);
        System.out.println("  .getMonthValue()      = " + opened.getMonthValue() + "   <-- 3 = March. Calendar's 0-based trap is gone.");
        System.out.println("  .getDayOfWeek()       = " + opened.getDayOfWeek());
        System.out.println("=> Use for birthdays, opening dates, due dates, statement days -- 'which DAY', not 'which moment'.");
    }

    static void localTimeAndDateTime() {
        System.out.println();
        System.out.println("=== LocalTime / LocalDateTime -- wall-clock readings, NO zone ===");
        LocalTime branchOpens = LocalTime.of(9, 30);
        LocalDate opened = LocalDate.of(2026, 3, 15);
        LocalDateTime appointment = opened.atTime(branchOpens); // atTime = date + time
        System.out.println("branch opens at         = " + branchOpens);
        System.out.println("appointment             = " + appointment);
        System.out.println("=> LocalDateTime is NOT an instant! It is the wall-clock reading '15 March, 09:30'");
        System.out.println("   SOMEWHERE. 5pm in Lagos and 5pm in London are different moments -- see next section.");
    }

    static void zonedDateTime() {
        System.out.println();
        System.out.println("=== ZonedDateTime -- LocalDateTime + zone = a REAL instant ===");
        // SecureBank story: Lagos HQ and the London desk both say "we close at 5pm".
        LocalDateTime fivePm = LocalDateTime.of(2026, 3, 15, 17, 0);
        ZonedDateTime lagosClose = fivePm.atZone(LAGOS);   // atZone: "this wall reading, in THIS zone"
        ZonedDateTime londonClose = fivePm.atZone(LONDON);
        System.out.println("Lagos  5pm              = " + lagosClose + "  -> instant " + lagosClose.toInstant());
        System.out.println("London 5pm              = " + londonClose + "  -> instant " + londonClose.toInstant());
        System.out.println("gap between the two 'same' 5pms = " + Duration.between(lagosClose.toInstant(), londonClose.toInstant()));

        // Now the SAME question in August — and the answer changes, because London has DST
        // and Nigeria does not. This is why zone RULES (ZoneRules) must live in the type.
        LocalDateTime fivePmSummer = LocalDateTime.of(2026, 8, 15, 17, 0);
        ZonedDateTime lagosSummer = fivePmSummer.atZone(LAGOS);
        ZonedDateTime londonSummer = fivePmSummer.atZone(LONDON);
        System.out.println("In August: London offset = " + londonSummer.getOffset() + " (BST), Lagos offset = " + lagosSummer.getOffset());
        System.out.println("  gap in August           = " + Duration.between(lagosSummer.toInstant(), londonSummer.toInstant())
                + "  <-- the two 5pms now COINCIDE");
        System.out.println("  London isDaylightSavings(March)? " + LONDON.getRules().isDaylightSavings(londonClose.toInstant()));
        System.out.println("  London isDaylightSavings(August)? " + LONDON.getRules().isDaylightSavings(londonSummer.toInstant()));
        System.out.println("=> 'Whose 5pm?' is a REAL banking question: cutoffs, FX rates and interest all hinge on it.");
    }

    static void offsetDateTime() {
        System.out.println();
        System.out.println("=== OffsetDateTime -- an instant + a FIXED offset (no zone rules) ===");
        ZonedDateTime lagosClose = LocalDateTime.of(2026, 3, 15, 17, 0).atZone(LAGOS);
        OffsetDateTime wire = lagosClose.toOffsetDateTime();
        System.out.println("on the wire             = " + wire);
        System.out.println("=> Same instant, but +01:00 is FROZEN -- it cannot know that London shifts in summer.");
        System.out.println("   Use OffsetDateTime at API boundaries; use ZonedDateTime for business rules.");
    }
}
