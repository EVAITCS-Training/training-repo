package com.evaitcs.java8.interfaces;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * STATIC interface methods: helpers that belong to the CONTRACT, not to instances.
 *
 * Before Java 8, utility methods that logically belonged to an interface had to live in
 * a separate class (Collections for Collection, that old companion-class smell). Java 8
 * lets the interface host them itself — the famous example is Comparator.comparing(...).
 * SecureBank uses them as policy FACTORIES: static methods that PRODUCE behavior.
 *
 * The one rule to write on the board (exam question, interview question):
 *   A static interface method is NOT inherited — not by implementing classes, not by
 *   sub-interfaces. The ONLY way to call it is InterfaceName.method().
 * Contrast it with interface CONSTANTS, which ARE inherited — students mix these up.
 */
public class StaticInterfaceMethodDemo {

    /** A money policy. Still a functional interface: exactly ONE abstract method (module 01). */
    @FunctionalInterface
    interface MoneyRules {

        /** Interface constants are public static final — and those ARE inherited, as always. */
        BigDecimal TRANSFER_LIMIT = new BigDecimal("500.00");

        boolean allows(BigDecimal amount);

        /** static factory #1 — like Comparator.comparing(...): it PRODUCES behavior. */
        static MoneyRules standard() {
            return amount -> amount.compareTo(TRANSFER_LIMIT) <= 0;
        }

        /** static factory #2 — same interface, stricter policy, no new class name required. */
        static MoneyRules strict() {
            return amount -> amount.compareTo(new BigDecimal("50.00")) <= 0;
        }
    }

    /** An ordinary implementor. Watch closely what it CAN and CANNOT reach. */
    static class TellerTerminal implements MoneyRules {
        @Override
        public boolean allows(BigDecimal amount) {
            // TRANSFER_LIMIT is usable unqualified — point at this line: constants are inherited.
            return amount.compareTo(TRANSFER_LIMIT) <= 0;

            // standard() is NOT usable unqualified — statics are not inherited. Exact javac error:
            //
            //     error: cannot find symbol
            //       symbol:   method standard()
            //       location: class TellerTerminal
        }
    }

    /** Students often ask: "can a sub-interface inherit the static?" No. Uncomment to see: */
    interface StrictRules extends MoneyRules { }
    //     StrictRules.standard();
    //     error: cannot find symbol
    //       symbol:   method standard()
    //       location: interface StrictRules

    public static void main(String[] args) {
        BigDecimal small = new BigDecimal("100.00");
        BigDecimal big = new BigDecimal("1000.00");

        System.out.println("=== The only legal call form: InterfaceName.method() ===");
        System.out.println("   MoneyRules.standard().allows(" + small + ")  = " + MoneyRules.standard().allows(small));
        System.out.println("   MoneyRules.standard().allows(" + big + ") = " + MoneyRules.standard().allows(big));
        System.out.println("   MoneyRules.strict().allows(" + small + ")    = " + MoneyRules.strict().allows(small));

        System.out.println();
        System.out.println("=== Not inherited — the reflection evidence ===");
        System.out.println("   MoneyRules    declares: " + declaredMethodNames(MoneyRules.class));
        System.out.println("   TellerTerminal declares: " + declaredMethodNames(TellerTerminal.class));
        System.out.println("   StrictRules   declares: " + declaredMethodNames(StrictRules.class));
        System.out.println("   The factories live ONLY on MoneyRules. The constant, on the other hand,");
        System.out.println("   IS inherited: TellerTerminal.TRANSFER_LIMIT = " + TellerTerminal.TRANSFER_LIMIT);

        System.out.println();
        System.out.println("=== An implementor is still just an implementor ===");
        MoneyRules terminal = new TellerTerminal();
        System.out.println("   terminal.allows(" + big + ") = " + terminal.allows(big));
        System.out.println("   And terminal.standard() would not compile either — same cannot-find-symbol");
        System.out.println("   error. A static method is reached through the TYPE that declares it. Period.");
    }

    private static List<String> declaredMethodNames(Class<?> c) {
        List<String> names = new ArrayList<>();
        for (Method m : c.getDeclaredMethods()) {
            names.add(m.getName());
        }
        return names;
    }
}
