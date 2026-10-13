package com.evaitcs.advanced.gof;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo 4 — BUILDER: assemble a complex object step by step, validate ONCE at the end.
 * (README section "Builder: the fix for the telescoping constructor".)
 *
 * Problem: a loan application has 3 required and 2 optional fields. A constructor
 * for every combination is the "telescoping constructor" anti-shape nobody can read.
 * Solution: a fluent Builder collects the pieces; build() refuses to hand out a
 * half-made object (IllegalStateException naming the missing fields).
 *
 * You already met builders in the JDK: StringBuilder.append(...).append(...),
 * Stream.Builder, and java.net.http HttpRequest.newBuilder(). The fluent chain is
 * also module 10's "fluent interface" idiom — same shape, named from the other side.
 */
public class LoanBuilderDemo {

    /**
     * The product: an immutable record (module 01). It validates AGAIN in its compact
     * constructor (defense in depth): even code that bypasses the Builder and calls
     * new LoanApplication(null, ...) directly cannot create an invalid object.
     * The Builder's build() remains the friendly door — it names ALL missing fields
     * at once instead of failing on the first.
     */
    public record LoanApplication(String applicantId, BigDecimal amount, int termMonths,
                                  String purpose, BigDecimal annualRate) {

        public LoanApplication {   // compact constructor — runs before any field is assigned
            java.util.Objects.requireNonNull(applicantId, "applicantId");
            java.util.Objects.requireNonNull(amount, "amount");
            java.util.Objects.requireNonNull(purpose, "purpose");
            java.util.Objects.requireNonNull(annualRate, "annualRate");
            if (termMonths <= 0) {
                throw new IllegalArgumentException("termMonths must be positive: " + termMonths);
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        /** The Builder: fluent setters return this, build() enforces the contract. */
        public static final class Builder {
            private String applicantId;                                  // required
            private BigDecimal amount;                                   // required
            private Integer termMonths;                                  // required (boxed = "not set yet")
            private String purpose = "PERSONAL";                         // optional, has a default
            private BigDecimal annualRate = new BigDecimal("6.50");      // optional, has a default

            public Builder applicantId(String applicantId) {
                this.applicantId = applicantId;
                return this;
            }

            public Builder amount(BigDecimal amount) {
                this.amount = amount;
                return this;
            }

            public Builder termMonths(int termMonths) {
                this.termMonths = termMonths;
                return this;
            }

            public Builder purpose(String purpose) {
                this.purpose = purpose;
                return this;
            }

            public Builder annualRate(BigDecimal annualRate) {
                this.annualRate = annualRate;
                return this;
            }

            /**
             * The whole point: validation happens ONCE, here — the object cannot
             * exist in an invalid state, because invalid states never get built.
             */
            public LoanApplication build() {
                List<String> missing = new ArrayList<>();
                if (applicantId == null) {
                    missing.add("applicantId");
                }
                if (amount == null) {
                    missing.add("amount");
                }
                if (termMonths == null) {
                    missing.add("termMonths");
                }
                if (!missing.isEmpty()) {
                    throw new IllegalStateException("missing required fields: " + missing);
                }
                return new LoanApplication(applicantId, amount, termMonths, purpose, annualRate);
            }
        }
    }

    public static void main(String[] args) {
        banner("1. The problem Builder replaces: the telescoping constructor");

        System.out.println("3 required + 2 optional fields means up to 8 constructor overloads,");
        System.out.println("and every call site looks like new LoanApplication(id, amt, 24, null, null) —");
        System.out.println("position-dependent, null-padded, unreadable. Builder replaces positions with NAMES.");

        banner("2. A fluent chain builds a complete application");

        LoanApplication full = LoanApplication.builder()
                .applicantId("CUST-77")
                .amount(new BigDecimal("25000.00"))
                .termMonths(24)
                .purpose("BUSINESS")
                .annualRate(new BigDecimal("5.25"))
                .build();
        System.out.println("step 1: chain applicantId -> amount -> termMonths -> purpose -> annualRate");
        System.out.println("step 2: build() validated and returned: " + full);
        System.out.println("verdict: every piece arrived by NAME; order of the chain does not matter.");

        banner("3. Optional slots keep their defaults");

        LoanApplication defaults = LoanApplication.builder()
                .applicantId("CUST-12")
                .amount(new BigDecimal("5000.00"))
                .termMonths(12)
                .build();
        System.out.println("step 1: only the three REQUIRED fields were set");
        System.out.println("step 2: built = " + defaults);
        System.out.println("verdict: purpose and annualRate came from the Builder's defaults, not from null.");

        banner("4. build() refuses to hand out a half-made object");

        try {
            LoanApplication.builder().purpose("PERSONAL").build();   // no applicantId, no amount, no term
        } catch (IllegalStateException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("why: build() collected every unset required field and named them all at once.");
            System.out.println("verdict: an invalid LoanApplication cannot exist — build() checks the pieces,");
            System.out.println("         and the record's compact constructor double-checks every direct call.");
        }

        banner("5. Where you already met it, and when NOT to use it");

        System.out.println("JDK: StringBuilder.append chains, Stream.Builder, HttpRequest.newBuilder().");
        System.out.println("stage-2 preview: Spring does validation declaratively (Bean Validation on");
        System.out.println("records) — same GOAL (no invalid object), different mechanism.");
        System.out.println("trap: a 3-field all-required object needs NO builder — its constructor is");
        System.out.println("already clear. Builder earns its keep with optional fields or fluent reads.");

        banner("6. In one sentence");
        System.out.println("A Builder separates 'collect the pieces' from 'check and create', so the");
        System.out.println("object the rest of the program meets is never half-made.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
