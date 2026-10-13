package com.evaitcs.corejava.statics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The UTILITY CLASS done right: nothing but static members, so no instance is ever
 * needed - exactly like java.lang.Math, java.util.Collections, java.util.Objects.
 *
 * Two design decisions to point at:
 *   1. `final` class      - a pile of unrelated helpers is not an extension point.
 *   2. PRIVATE constructor - `new BankUtils()` stops being legal code, and the private
 *      ctor ALSO blocks subclassing by itself (a subclass constructor must call
 *      super()). UtilityClassDemo proves via reflection that the ctor exists but is
 *      inaccessible - the escape-hatch story from module 06.
 */
public final class BankUtils {

    /** Constants live at the top: static final, UPPER_SNAKE_CASE, safe to share (both immutable). */
    public static final String CURRENCY_CODE = "USD";
    public static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("-500.00");

    /**
     * The TRAP sibling of the constants above: `static final` does NOT mean immutable.
     * It means the REFERENCE can never be re-assigned - the list's CONTENTS are wide
     * open. UtilityClassDemo adds to this list legally, and shows the compile error
     * re-assignment would produce. Same "frozen slot vs live object" split as
     * effectively-final captures in java8-features/01-lambdas.
     */
    public static final List<String> SUPPORTED_CURRENCIES =
            new ArrayList<String>(Arrays.asList("USD", "EUR"));

    private BankUtils() {
        // Not "forgotten" - DELIBERATE. Every member is static, so an instance would
        // hold no state and add no behavior: pure waste. The guard message turns any
        // reflection accident (module 06) into a loud, explained failure.
        throw new AssertionError("BankUtils is a utility class - no instances, ever");
    }

    public static String formatMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP) + " " + CURRENCY_CODE;
    }

    public static boolean isOverdrawn(BankAccount account) {
        // A static method CAN take instances as PARAMETERS - it just cannot have one
        // of its own. Data comes IN through the argument list, never through `this`.
        return account.getBalance().compareTo(BigDecimal.ZERO) < 0;
    }

    public static BigDecimal totalBalance(List<BankAccount> accounts) {
        BigDecimal total = BigDecimal.ZERO;
        for (BankAccount a : accounts) { // classic loop on purpose - streams arrive in java8-features/04
            total = total.add(a.getBalance());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
