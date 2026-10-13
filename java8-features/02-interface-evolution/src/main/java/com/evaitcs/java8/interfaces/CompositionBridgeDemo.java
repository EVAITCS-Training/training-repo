package com.evaitcs.java8.interfaces;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Closing the loop with MODULE 01. There, ComposingDemo happily wrote
 *
 *     isDeposit.and(isLarge)      largeDeposit.negate()
 *
 * as if and/negate simply existed. Now you can see where they live: they are DEFAULT
 * methods on Predicate. A Java 7 Predicate could never have had them — an interface
 * with exactly one abstract method could have NO methods with bodies at all.
 * Default methods are the reason functional interfaces can do behavior algebra:
 * `test` stays the single abstract method (lambdas keep working), while and/or/negate
 * ride along as defaults (composition keeps working).
 */
public class CompositionBridgeDemo {

    static final List<String> FEED = Arrays.asList(
            "PAYROLL +2500.00",
            "RENT -1200.00",
            "INTEREST +3.11",
            "CASH-WITHDRAWAL -300.00");

    public static void main(String[] args) throws Exception {
        System.out.println("=== Predicate under the microscope (reflection) ===");
        report(Predicate.class.getMethod("test", Object.class));
        report(Predicate.class.getMethod("negate"));
        report(Predicate.class.getMethod("and", Predicate.class));
        report(Predicate.class.getMethod("or", Predicate.class));
        System.out.println("   ONE abstract method keeps it a functional interface (module 01);");
        System.out.println("   the combinators are defaults (this module). Both at once = Java 8.");

        System.out.println();
        System.out.println("=== Using them, module-01 style, on the SecureBank feed ===");
        Predicate<String> isPayroll = entry -> entry.startsWith("PAYROLL");
        Predicate<String> isCredit  = entry -> entry.contains("+");
        Predicate<String> isLarge   = entry -> amountOf(entry).compareTo(new BigDecimal("500.00")) > 0;

        Predicate<String> largeCredit = isLarge.and(isCredit);   // <- a DEFAULT method
        Predicate<String> notPayroll  = isPayroll.negate();      // <- a DEFAULT method

        for (String entry : FEED) {
            System.out.printf("   %-22s largeCredit=%-5s notPayroll=%s%n",
                    entry, largeCredit.test(entry), notPayroll.test(entry));
        }

        System.out.println();
        System.out.println("   Without default methods, and/negate/or could not exist on an");
        System.out.println("   interface — module 01's ComposingDemo would need wrapper classes.");
    }

    /** The amount is the token after the last space. Money is BigDecimal — always. */
    static BigDecimal amountOf(String entry) {
        return new BigDecimal(entry.substring(entry.lastIndexOf(' ') + 1)).abs();
    }

    private static void report(Method m) {
        System.out.printf("   Predicate.%-8s abstract=%-5s default=%s%n",
                m.getName(), Modifier.isAbstract(m.getModifiers()), m.isDefault());
    }
}
