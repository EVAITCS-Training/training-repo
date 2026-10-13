package com.evaitcs.java8.optional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * THE classic trap, live: orElse(x) takes a VALUE, and Java evaluates every argument
 * BEFORE the call runs. So the default is computed even when the Optional is present —
 * if that default is a database hit or a remote call, you just paid for it for nothing.
 *
 * orElseGet(Supplier) takes a FACTORY: the supplier is invoked only when the value is
 * genuinely needed. Lazy, not eager.
 *
 * Rule of thumb to write on the whiteboard:
 *   orElse    -> the default is a constant or already in hand:  .orElse(BigDecimal.ZERO)
 *   orElseGet -> the default COSTS something (query, new object, remote service)
 */
public class OrElseVsOrElseGetDemo {

    /** Counts how often the "expensive" fallback actually ran, so the trap is visible in output. */
    private static int fallbackCalls = 0;

    public static void main(String[] args) {
        Account alice = Account.seedAccounts().get(0); // credit limit 5000.00 — PRESENT
        Optional<BigDecimal> limit = alice.getCreditLimit();

        // ---------- orElse: the argument is evaluated EVEN THOUGH the value is present ----------
        fallbackCalls = 0;
        BigDecimal eager = limit.orElse(queryCreditBureau("ACC-1001")); // <-- runs NOW, unconditionally
        System.out.println("orElse    -> " + eager.toPlainString() + "   (fallbackCalls = " + fallbackCalls + ")");
        System.out.println("  ^^ We paid for a credit-bureau round-trip we never needed.");
        System.out.println("     Java evaluated the argument BEFORE orElse could look inside the Optional.");

        // ---------- orElseGet: the supplier is never invoked ----------
        fallbackCalls = 0;
        BigDecimal lazy = limit.orElseGet(() -> queryCreditBureau("ACC-1001"));
        System.out.println("orElseGet -> " + lazy.toPlainString() + "   (fallbackCalls = " + fallbackCalls + ")");
        System.out.println("  Same result, zero wasted calls. The lambda defers the work.");

        // On an EMPTY Optional both call the fallback exactly once — orElseGet wins
        // when the value is present, or when even CONSTRUCTING the default is expensive.
        fallbackCalls = 0;
        BigDecimal d = Optional.<BigDecimal>empty().orElseGet(() -> queryCreditBureau("ACC-1002"));
        System.out.println("empty.orElseGet -> " + d.toPlainString() + "   (fallbackCalls = " + fallbackCalls + ")");

        // ---------- isPresent + get: the null check in a nicer hat ----------
        System.out.println();
        BigDecimal oldStyle;
        if (limit.isPresent()) {
            oldStyle = limit.get();
        } else {
            oldStyle = BigDecimal.ZERO;
        }
        System.out.println("isPresent()/get() -> " + oldStyle.toPlainString()
                + "   (this IS act one's `if (a != null)` wearing a nicer hat — orElseGet says it in one line)");

        // ---------- orElseThrow: when absence is an ERROR, not a fact of life ----------
        try {
            Optional.<Account>empty().orElseThrow(
                    () -> new IllegalStateException("account ACC-9999 does not exist"));
        } catch (IllegalStateException e) {
            System.out.println();
            System.out.println("orElseThrow(Supplier) -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("  Compare with a message-less NPE: this exception says WHAT was missing and WHY.");
        }
        // Java 8 has NO no-arg orElseThrow() — that arrives in Java 10. In Java 8 the
        // "just give it to me" escape hatch is .get(), and look what it costs you:
        try {
            Optional.<String>empty().get();
        } catch (NoSuchElementException e) {
            System.out.println("empty.get()           -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("  \"just call get()\" is not a plan — it is an NPE with extra steps. The stack");
            System.out.println("  trace says 'No value present' but NOT which value or which lookup. Prefer");
            System.out.println("  orElseThrow(Supplier) so the failure carries the context a debugger needs.");
        }
    }

    /** Stands in for an expensive default: a remote credit-bureau lookup. Prints when it runs. */
    private static BigDecimal queryCreditBureau(String accountId) {
        fallbackCalls++;
        System.out.println("  !! calling the (expensive) credit bureau for " + accountId + " ...");
        return new BigDecimal("1000.00");
    }
}
