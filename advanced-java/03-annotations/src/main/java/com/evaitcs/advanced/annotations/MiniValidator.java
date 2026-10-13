package com.evaitcs.advanced.annotations;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The READER that gives the validation stickers meaning. (README section "Build a mini
 * framework".)
 *
 * On its own, {@code @NotNull} does nothing — it is inert metadata. MiniValidator is what
 * turns it into behavior: it walks an object's fields with reflection (module 02), reads each
 * field's annotations, checks the value against them, and collects violations.
 *
 * This is Jakarta Validation (Bean Validation) and every Spring {@code @Valid} check, reduced
 * to ~40 lines. Same three moves: reflect over fields, read annotations, enforce rules.
 */
public final class MiniValidator {

    /** One broken rule: which field, which sticker, and the offending actual value. */
    public record Violation(String field, String rule, String actual) {
    }

    /**
     * Validates one object. Returns an empty list when every sticker is satisfied.
     *
     * The result is SORTED by field then rule, because {@code getDeclaredFields()} does not
     * guarantee an order (JLS/reflection spec) — sorting makes the output deterministic, which
     * the repo's determinism rule requires and the test asserts.
     */
    public static List<Violation> validate(Object target) {
        List<Violation> violations = new ArrayList<>();

        for (Field field : target.getClass().getDeclaredFields()) {
            field.setAccessible(true);   // Account's fields are private; this is classpath code, so it works
            Object value;
            try {
                value = field.get(target);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("reflection could not read " + field.getName(), e);
            }

            // Rule 1 — @NotNull: the value must be present.
            if (field.isAnnotationPresent(NotNull.class) && value == null) {
                violations.add(new Violation(field.getName(), "@NotNull", "null"));
            }

            // Rule 2 — @Min(n): a BigDecimal must be >= n. Pattern matching keeps the cast safe.
            Min min = field.getAnnotation(Min.class);
            if (min != null && value instanceof BigDecimal decimal
                    && decimal.compareTo(BigDecimal.valueOf(min.value())) < 0) {
                violations.add(new Violation(field.getName(), "@Min(" + min.value() + ")", decimal.toPlainString()));
            }

            // Rule 3 — @CurrencyScale(n): a BigDecimal must have exactly n decimal places.
            CurrencyScale scale = field.getAnnotation(CurrencyScale.class);
            if (scale != null && value instanceof BigDecimal decimal && decimal.scale() != scale.value()) {
                violations.add(new Violation(field.getName(),
                        "@CurrencyScale(" + scale.value() + ")", "scale " + decimal.scale()));
            }
        }

        violations.sort(Comparator.comparing(Violation::field).thenComparing(Violation::rule));
        return violations;
    }

    private MiniValidator() {
        // A utility class is never instantiated.
    }
}
