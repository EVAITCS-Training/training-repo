package com.evaitcs.advanced.annotations;

import java.math.BigDecimal;
import java.util.List;

/**
 * Demo 3 — THE CENTERPIECE: a mini validation framework. (README section "Build a mini framework".)
 *
 * This is the payoff of modules 02 + 03 together. The stickers on {@link Account} (@NotNull,
 * @Min, @CurrencyScale) are inert. {@link MiniValidator} is the READER: it walks the fields with
 * reflection, reads each annotation, checks the value, and reports violations. Run it on a clean
 * account and a dirty one and watch the SAME annotations produce zero or three complaints.
 *
 * Swap the names and you have Jakarta Validation: @NotNull/@Min are jakarta.validation.constraints,
 * MiniValidator is Hibernate Validator, and Spring calls it when a @Valid @RequestBody lands.
 */
public class MiniValidatorDemo {

    public static void main(String[] args) {
        banner("1. A valid account — every sticker is satisfied");

        Account good = new Account("A-2", "Dana", new BigDecimal("250.00"), new BigDecimal("500.00"));
        List<MiniValidator.Violation> none = MiniValidator.validate(good);
        System.out.println("account: " + good);
        System.out.println("step 1: MiniValidator walked 4 fields and read their annotations.");
        System.out.println("step 2: violations found = " + none.size());
        System.out.println("verdict: clean. The reader enforced every rule and complained about nothing.");

        banner("2. An invalid account — three planted problems");

        // Planted on purpose: owner=null breaks @NotNull; balance=-5.00 breaks @Min(0);
        // withdrawalLimit has scale 3, breaking @CurrencyScale(2). id is fine.
        Account bad = new Account("A-1", null, new BigDecimal("-5.00"), new BigDecimal("100.000"));
        System.out.println("account: " + bad);
        List<MiniValidator.Violation> violations = MiniValidator.validate(bad);
        int step = 1;
        for (MiniValidator.Violation violation : violations) {
            System.out.println("step " + step + ": field '" + violation.field() + "' broke "
                    + violation.rule() + " (actual: " + violation.actual() + ")");
            step++;
        }
        System.out.println("verdict: " + violations.size() + " violations. The annotations did NOTHING by themselves —");
        System.out.println("         MiniValidator READ them (reflection) and turned metadata into behavior.");

        banner("3. This is Jakarta Validation, in ~40 lines");

        System.out.println("Rename @NotNull/@Min to jakarta.validation.constraints.* and MiniValidator to");
        System.out.println("Hibernate Validator, and you have Bean Validation. When Spring MVC sees a");
        System.out.println("@Valid @RequestBody, it runs exactly this loop: reflect the fields, read the");
        System.out.println("annotations, collect violations, and turn them into a 400 ProblemDetail.");

        banner("4. In one sentence");
        System.out.println("An annotation is a sticky note; a reader is the hand that acts on it — and building");
        System.out.println("the reader yourself demystifies every validation and binding framework you will ever use.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
