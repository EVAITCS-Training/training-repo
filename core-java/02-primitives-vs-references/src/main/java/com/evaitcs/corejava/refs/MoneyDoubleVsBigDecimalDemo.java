package com.evaitcs.corejava.refs;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money: why double loses cents and BigDecimal does not.
 *
 * THE ONE SENTENCE: double is BINARY floating point — most decimal fractions (0.1, 0.2, ...)
 * have no exact binary representation, so they are stored as the nearest representable value
 * and the tiny errors accumulate. This is the single reason every money field in this whole
 * repo is BigDecimal.
 */
public class MoneyDoubleVsBigDecimalDemo {

    public static void main(String[] args) {
        banner("1. The famous one: 0.1 + 0.2 in double is NOT 0.3");
        double sum = 0.1 + 0.2;
        System.out.println("0.1 + 0.2                = " + sum);
        System.out.println("the real bits, expanded  = " + new BigDecimal(sum)); // new BigDecimal(double) reveals the truth
        System.out.println("(0.1 + 0.2 == 0.3)       = " + (sum == 0.3));

        banner("2. A bank balance chain that LOSES cents with double");
        double balance = 0.0;
        for (int i = 0; i < 10; i++) {
            balance += 0.10; // ten monthly service fees of 10 cents each
        }
        System.out.println("double: 0.10 added 10x   = " + balance + "   (a bank would print 1.00)");
        System.out.println("double: balance == 1.0   = " + (balance == 1.0));

        banner("3. The SAME chain with BigDecimal (scale 2) is EXACT");
        BigDecimal exact = new BigDecimal("0.00"); // build from a STRING, never from a double
        BigDecimal fee = new BigDecimal("0.10");
        for (int i = 0; i < 10; i++) {
            exact = exact.add(fee); // BigDecimal is immutable: add RETURNS a new value
        }
        exact = exact.setScale(2, RoundingMode.HALF_UP);
        System.out.println("BigDecimal: 0.10 added 10x = " + exact.toPlainString() + "   (exactly 1.00)");
        System.out.println("BigDecimal: compareTo(1.00) == 0 : " + (exact.compareTo(new BigDecimal("1.00")) == 0));
        System.out.println("=> THIS is why money is BigDecimal everywhere in SecureBank.");

        banner("4. BigDecimal comparison trap: equals checks SCALE, compareTo checks VALUE");
        BigDecimal twoOh = new BigDecimal("2.0");   // scale 1
        BigDecimal twoOhOh = new BigDecimal("2.00"); // scale 2
        System.out.println("2.0.equals(2.00)          = " + twoOh.equals(twoOhOh) + "   (different scale -> not 'equal')");
        System.out.println("2.0.compareTo(2.00) == 0  = " + (twoOh.compareTo(twoOhOh) == 0) + "    (same value -> use compareTo for money)");
        System.out.println();
        System.out.println("Also: ALWAYS build BigDecimal from a String. new BigDecimal(0.1) = " + new BigDecimal(0.1));
        System.out.println("      (that's the double 0.1 dragged in with all its error — never what you meant.)");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
