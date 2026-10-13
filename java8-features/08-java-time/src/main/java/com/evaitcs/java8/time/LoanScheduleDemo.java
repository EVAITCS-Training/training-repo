package com.evaitcs.java8.time;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Closing demo — the whole module on one SecureBank loan.
 *
 * Story: a NGN 500,000 personal loan disbursed 2026-01-30, 6 monthly instalments,
 * daily interest, final deadline at 16:00 Lagos time, customer letter formatted.
 * Watch for: end-of-month CLAMPING (Jan 30 + 1 month), lastDayOfMonth() snapping,
 * ChronoUnit.DAYS for the interest window, BigDecimal money, ZonedDateTime deadline.
 */
public class LoanScheduleDemo {

    static final ZoneId LAGOS = ZoneId.of("Africa/Lagos");
    static final BigDecimal PRINCIPAL = new BigDecimal("500000.00");
    static final BigDecimal DAILY_RATE = new BigDecimal("0.00045"); // 0.045%/day — money is ALWAYS BigDecimal

    public static void main(String[] args) {
        LocalDate disbursed = LocalDate.of(2026, 1, 30);
        int termMonths = 6;

        System.out.println("=== SecureBank personal loan schedule ===");
        System.out.println("principal               = NGN " + PRINCIPAL);
        System.out.println("disbursed               = " + disbursed + " (" + disbursed.getDayOfWeek() + ")");
        System.out.println("term                    = " + termMonths + " monthly instalments, daily rate " + DAILY_RATE);

        // Instalment dates: disbursement + N months, SNAPPED to the last day of that month.
        // Why snap? Jan 30 + 1 month = Feb 28 in 2026 (clamped — February has no 30th), but
        // + 2 months would jump BACK to Mar 30. lastDayOfMonth() makes every date predictable
        // and explainable to a customer: "always the last day of the month".
        List<LocalDate> instalments = new ArrayList<LocalDate>();
        for (int m = 1; m <= termMonths; m++) {
            instalments.add(disbursed.plusMonths(m).with(TemporalAdjusters.lastDayOfMonth()));
        }

        System.out.println();
        System.out.println("Instalment due dates:");
        int n = 1;
        for (LocalDate due : instalments) {
            System.out.println("  " + n++ + ". " + due + " (" + due.getDayOfWeek() + ")");
        }

        LocalDate maturity = instalments.get(instalments.size() - 1); // 2026-07-31
        long interestDays = ChronoUnit.DAYS.between(disbursed, maturity); // the interest WINDOW in days
        BigDecimal interest = PRINCIPAL.multiply(DAILY_RATE).multiply(BigDecimal.valueOf(interestDays))
                .setScale(2, RoundingMode.HALF_UP);

        System.out.println();
        System.out.println("maturity                = " + maturity);
        System.out.println("interest window         = " + interestDays + " days (ChronoUnit.DAYS.between)");
        System.out.println("interest owed           = NGN " + interest + "  (principal x dailyRate x days)");
        System.out.println("total repayment         = NGN " + PRINCIPAL.add(interest));

        // The DEADLINE is a MOMENT, not a date: 16:00 Lagos time on the maturity day.
        // A bare LocalDate would leave 'end of day' ambiguous across zones — a ZonedDateTime does not.
        ZonedDateTime deadline = maturity.atTime(16, 0).atZone(LAGOS);
        System.out.println();
        System.out.println("payment deadline        = " + deadline);
        System.out.println("  as a machine instant  = " + deadline.toInstant());

        // Customer letter — one immutable formatter, PINNED Locale, zone spelled out for humans.
        DateTimeFormatter letter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy 'by' HH:mm z", Locale.UK);
        System.out.println();
        System.out.println("--- customer letter ---");
        System.out.println("Dear Customer, your final payment of NGN " + PRINCIPAL.add(interest)
                + " is due " + letter.format(deadline) + ".");
        System.out.println("=> LocalDate for the calendar, ChronoUnit for the window, BigDecimal for the money,");
        System.out.println("   ZonedDateTime for the deadline, DateTimeFormatter for the letter. Whole module, one loan.");
    }
}
