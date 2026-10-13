package com.evaitcs.java8.lambdas;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * What a lambda may capture — and why the rule exists.
 *
 * RULE: local variables used inside a lambda must be FINAL or EFFECTIVELY FINAL
 * (assigned exactly once). Instance FIELDS are exempt — no rule at all for them.
 *
 * WHY (say this out loud in class): a lambda can outlive the method that created it —
 * you can return it, store it, run it on another thread tomorrow. Locals live on the
 * stack and die with the method, so the lambda actually captures a COPY. If you could
 * reassign the local, the copy and the original would silently disagree. Java removes
 * the ambiguity by refusing to compile. Fields don't have this problem: one copy, on the heap.
 */
public class EffectivelyFinalDemo {

    // An instance field: lambdas may capture `this` and read/write fields freely.
    private BigDecimal feeRate = new BigDecimal("0.015");

    public static void main(String[] args) {
        new EffectivelyFinalDemo().run();
    }

    void run() {
        // ---- 1. Effectively final local: declared, assigned once, never touched again ----
        BigDecimal bonusRate = new BigDecimal("0.05"); // "effectively final" — no `final` keyword needed
        Supplier<BigDecimal> withBonus = () -> feeRate.add(bonusRate);
        //  ^ captures the FIELD feeRate (through `this`) AND the local bonusRate (a copy).
        System.out.println("1. effectively final local captured fine : " + withBonus.get());

        // ---- 2. Reassigning the local AFTER capture = COMPILE ERROR ----
        // Uncomment to show the class the exact error message:
        //
        //   bonusRate = new BigDecimal("0.10");
        //   ^^^^^^^^^ local variables referenced from a lambda expression must be final or effectively final
        //
        // Note the error points at the ASSIGNMENT, not the lambda — the variable became
        // non-effectively-final, which retroactively invalidates the capture.
        System.out.println("2. reassigning a captured local does not compile (see comment in source)");

        // A local that is reassigned but NEVER captured is of course fine:
        BigDecimal scratch = BigDecimal.ONE;
        scratch = scratch.add(BigDecimal.ONE); // legal — no lambda uses it
        System.out.println("   (uncaptured locals can be reassigned freely: scratch = " + scratch + ")");

        // ---- 3. FIELDS can be mutated from inside the lambda — no copy, one shared object ----
        Supplier<BigDecimal> showFee = () -> feeRate;
        System.out.println("3. fee before mutation : " + showFee.get());
        feeRate = new BigDecimal("0.02");          // mutate the field...
        System.out.println("   fee after mutation  : " + showFee.get() + "  <= the lambda SEES the change");
        System.out.println("   => fields are captured via `this`, so there is nothing to go stale.");

        // ---- 4. Capturing a mutable OBJECT through a final reference ----
        // The rule is about the VARIABLE, not the object. The reference can't be re-pointed,
        // but the object it points to can still change. Classic exam question, classic bug source.
        StringBuilder audit = new StringBuilder();
        Runnable logIt = () -> audit.append("fee changed; "); // mutating the object: legal
        // audit = new StringBuilder();                        // re-pointing the variable: NOT legal
        logIt.run();
        logIt.run();
        System.out.println("4. final reference, mutable object     : audit = \"" + audit + "\"");
    }
}
