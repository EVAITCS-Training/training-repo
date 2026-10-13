package com.evaitcs.advanced.gof;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Demo 3 — FACTORY METHOD: construction with a decision inside.
 * (README section "Factory Method: List.of is one you call every day".)
 *
 * Problem: the caller knows the payment TYPE as a string ("card", "wire") but must
 * not decide which class to instantiate or how to fill its fields. Solution: one
 * static factory method owns the decision; callers get a sealed Payment back.
 *
 * You already call factories daily: List.of(...) chooses an internal list class you
 * never name; Integer.valueOf(...) decides new-vs-cached (that caching half is the
 * Flyweight pattern). Factories are also where module 01's sealed + switch shines:
 * describe() below EXHAUSTS the family with no default branch.
 */
public class PaymentFactoryDemo {

    /** The product family — sealed, so every switch over it is compiler-checked complete. */
    public sealed interface Payment permits CardPayment, WirePayment, DirectDebitPayment { }

    public record CardPayment(String last4, BigDecimal amount) implements Payment { }
    public record WirePayment(String iban, BigDecimal amount) implements Payment { }
    public record DirectDebitPayment(String mandateId, BigDecimal amount) implements Payment { }

    /** The factory. Static, like every JDK factory you know: valueOf, of, from, getInstance. */
    public static final class PaymentFactory {

        public static Payment from(String type, BigDecimal amount) {
            return switch (type.toLowerCase(Locale.ROOT)) {
                case "card" -> new CardPayment("4242", amount);
                case "wire" -> new WirePayment("NL93-ABNA-0001", amount);
                case "direct-debit" -> new DirectDebitPayment("MND-778", amount);
                // A factory's real job includes failing LOUDLY at the decision point:
                default -> throw new IllegalArgumentException("unknown payment type: " + type);
            };
        }

        /** Exhaustive pattern-matching switch over the sealed family — no default needed. */
        public static String describe(Payment payment) {
            return switch (payment) {
                case CardPayment(var last4, var amount) -> "card ending " + last4 + " for " + amount.toPlainString();
                case WirePayment(var iban, var amount) -> "wire to " + iban + " for " + amount.toPlainString();
                case DirectDebitPayment(var mandate, var amount) -> "direct debit via mandate " + mandate
                        + " for " + amount.toPlainString();
            };
        }
    }

    public static void main(String[] args) {
        banner("1. from(String) picks the subtype — the caller never writes new");

        BigDecimal amount = new BigDecimal("86.40");
        for (String type : new String[] {"card", "wire", "direct-debit"}) {
            Payment payment = PaymentFactory.from(type, amount);
            System.out.println("step: from(\"" + type + "\")  -> class = " + payment.getClass().getSimpleName());
            System.out.println("      describe          -> " + PaymentFactory.describe(payment));
        }
        System.out.println("verdict: three different sealed subtypes, ONE entry point, zero new at the call site.");

        banner("2. Unknown type fails loudly AT THE FACTORY, not three layers later");

        try {
            PaymentFactory.from("crypto", amount);
        } catch (IllegalArgumentException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("why: the factory owns the type decision, so it owns the rejection.");
            System.out.println("verdict: an error at the decision point beats a mystery at the use point.");
        }

        banner("3. The sealed switch is a compiler-enforced to-do list");

        System.out.println("describe() has NO default branch — the sealed family makes it exhaustive.");
        System.out.println("Add a fourth Payment tomorrow and describe() (and every other switch");
        System.out.println("over Payment) STOPS COMPILING until the new case is handled (module 01).");

        banner("4. Where you already met it");

        System.out.println("List.of(a, b, c)  -> decides which internal immutable list class to build.");
        System.out.println("Integer.valueOf(n)-> new object OR a cached one (-128..127: Flyweight bonus).");
        System.out.println("stage-2 preview   -> Spring's BeanFactory is a factory over YOUR classes (module 10).");

        banner("5. Trap: when NOT to use a factory");

        System.out.println("If the caller already KNOWS the concrete type, new WirePayment(...) is");
        System.out.println("clearer than a string-keyed factory. Factories earn their keep only when");
        System.out.println("the choice is real: input-driven, cached, or config-driven construction.");

        banner("6. In one sentence");
        System.out.println("A factory method moves the 'which class, built how?' decision into ONE");
        System.out.println("place, so callers state WHAT they want and never HOW it is made.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
