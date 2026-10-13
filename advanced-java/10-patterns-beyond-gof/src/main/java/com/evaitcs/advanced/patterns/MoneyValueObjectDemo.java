package com.evaitcs.advanced.patterns;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Demo C1 — VALUE OBJECT: an object defined by its VALUE, not its identity.
 * (README section "Value Object: Money with a bouncer at the door".)
 *
 * Problem: two "2.50 EUR" amounts must be interchangeable, must never be negative,
 * must never drift in scale (1.5 vs 1.50), and must be safe to share between threads.
 * Solution: an immutable record whose COMPACT CONSTRUCTOR rejects nonsense before
 * any field is ever assigned — a bouncer at the only door in.
 *
 * Course ties: money is BigDecimal scale 2 everywhere in this curriculum; records
 * are module 01; "immutable objects need no locks" is module 04's happiest fact.
 *
 * The three rejections below are asserted in BeyondGofTest with assertThrows — each
 * one deterministically, on chosen values, every run.
 */
public class MoneyValueObjectDemo {

    /**
     * The Value Object. The compact constructor (no parameter list — it runs BEFORE
     * the field assignment, on the incoming values) is the whole trick: an invalid
     * Money cannot exist, because every path into one passes the bouncer.
     */
    public record Money(BigDecimal amount) {

        public Money {
            Objects.requireNonNull(amount, "amount must not be null");
            if (amount.scale() != 2) {
                throw new IllegalArgumentException("money scale must be exactly 2, was "
                        + amount.scale() + ": " + amount.toPlainString());
            }
            if (amount.signum() < 0) {
                throw new IllegalArgumentException("money must not be negative: " + amount.toPlainString());
            }
        }

        /** Static factory (the JDK's favorite idiom — see IdiomsDemo banner 2). */
        public static Money of(String text) {
            return new Money(new BigDecimal(text));
        }

        /** Operations return NEW instances — the old one is never touched. */
        public Money add(Money other) {
            return new Money(amount.add(other.amount));
        }

        /** Subtraction that would go negative is rejected by the bouncer, not clamped. */
        public Money subtract(Money other) {
            return new Money(amount.subtract(other.amount));
        }
    }

    public static void main(String[] args) {
        banner("1. Value, not identity: two 2.50s ARE each other");

        Money a = Money.of("2.50");
        Money b = Money.of("2.50");
        System.out.println("step 1: a = " + a + ", b = " + b + "  (two separate objects)");
        System.out.println("step 2: a.equals(b) = " + a.equals(b) + ", hashCodes equal = " + (a.hashCode() == b.hashCode()));
        System.out.println("step 3: contrast an Account — two accounts with the same balance are DIFFERENT");
        System.out.println("        accounts (identity matters); two 2.50 amounts are the SAME value (identity irrelevant).");
        System.out.println("verdict: records give equals/hashCode for free — but only SOUNDLY because of banner 2.");

        banner("2. Why scale 2 is PINNED: BigDecimal.equals is scale-sensitive");

        System.out.println("new BigDecimal(\"1.5\").equals(new BigDecimal(\"1.50\")) = "
                + new BigDecimal("1.5").equals(new BigDecimal("1.50")));
        System.out.println("Surprise? BigDecimal.equals compares scale AND value. If Money allowed mixed");
        System.out.println("scales, Money(1.5) != Money(1.50) would break every Set and Map key.");
        System.out.println("So the compact constructor pins scale to exactly 2 — then value equality IS equality.");

        banner("3. The bouncer: three rejections, each with its exact exception");

        try {
            new Money(null);
        } catch (NullPointerException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("why: requireNonNull — a null amount is a bug, not a value.");
        }
        try {
            Money.of("2.5");
        } catch (IllegalArgumentException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("why: scale 1 rejected — pinning scale is what makes banner 2's equality sound.");
        }
        try {
            Money.of("-5.00");
        } catch (IllegalArgumentException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("why: negative money rejected at the door — no object exists to fix later.");
        }

        banner("4. Negative zero cannot sneak in");

        System.out.println("step 1: double HAS a negative zero: Double.toString(-0.0) = " + Double.toString(-0.0)
                + ", and 1.0 / -0.0 = " + (1.0 / -0.0) + "  <- one more reason money is never double");
        System.out.println("step 2: BigDecimal normalizes it: new BigDecimal(\"-0.00\").toString() = "
                + new BigDecimal("-0.00") + " (signum " + new BigDecimal("-0.00").signum() + ")");
        System.out.println("step 3: so Money.of(\"-0.00\").equals(Money.of(\"0.00\")) = "
                + Money.of("-0.00").equals(Money.of("0.00")) + " — no haunted twin of zero");
        System.out.println("step 4: and BigDecimal.valueOf(-0.0) has scale " + BigDecimal.valueOf(-0.0).scale()
                + " — the scale rule rejects it anyway.");

        banner("5. Operations return NEW money — immutability is free thread safety");

        Money start = Money.of("10.00");
        Money sum = start.add(Money.of("5.00"));
        System.out.println("step 1: start = Money(10.00); sum = start.add(Money(5.00))");
        System.out.println("step 2: sum = " + sum + ", and start is STILL " + start + " (untouched)");
        try {
            start.subtract(sum);
        } catch (IllegalArgumentException e) {
            System.out.println("step 3: start.subtract(sum) -> CAUGHT " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
        }
        System.out.println("verdict: module 04's rule — an immutable object cannot be corrupted, so it needs");
        System.out.println("         no lock, no volatile, no defensive copy. Share it freely across threads.");

        banner("6. Trap: when NOT to use this shape");

        System.out.println("Money here has NO currency component: add(EUR, USD) would silently succeed.");
        System.out.println("Single-currency apps are fine; the moment a second currency appears, currency");
        System.out.println("joins the record (Money(BigDecimal amount, Currency ccy)) and add() checks it.");
        System.out.println("A value object must validate EVERY invariant its equality promises.");

        banner("7. In one sentence");
        System.out.println("A Value Object trades identity for equality — and pays for that trade with a");
        System.out.println("constructor that makes invalid states unrepresentable.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
