package com.evaitcs.java8.lambdas;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Every syntactic shape a lambda can take — point at each one while explaining.
 *
 * THE ONE SENTENCE: a lambda is an anonymous function whose parameter types the compiler
 * infers from the "target type" — the functional interface it is being used as.
 */
public class LambdaSyntaxDemo {

    public static void main(String[] args) {

        banner("1. The shapes");

        // Shape A: expression body, inferred parameter type, no parentheses needed for ONE param.
        // Target type is Predicate<Transaction>: "give me a Transaction, I give you a boolean".
        Predicate<Transaction> isDeposit = t -> t.isDeposit();
        System.out.println("expression body, one param, no parens : t -> t.isDeposit()");

        // Shape B: same thing, block body with braces. Now you MUST write `return`.
        // Rule to state out loud: expression body => value is returned implicitly;
        //                           block body    => behave like a method, return explicitly.
        Predicate<Transaction> isDepositBlock = t -> {
            boolean deposit = t.isDeposit();
            return deposit;
        };
        System.out.println("block body needs braces AND return    : t -> { ...; return ...; }");

        // Shape C: two parameters => parentheses are MANDATORY (arity is ambiguous otherwise).
        Comparator<Transaction> byAmount = (t1, t2) -> t1.getAmount().compareTo(t2.getAmount());
        System.out.println("two params => parentheses required    : (t1, t2) -> ...");

        // Shape D: zero parameters => empty parentheses.
        Supplier<BigDecimal> zero = () -> BigDecimal.ZERO;
        System.out.println("zero params => empty parens           : () -> BigDecimal.ZERO");

        // Shape E: explicit parameter types. Legal, but almost never written —
        // the compiler already knows them from the target type. Show it once so
        // students recognize it in the wild, then tell them to drop the types.
        Function<Transaction, BigDecimal> amountOf = (Transaction t) -> t.getAmount();
        System.out.println("explicit types are allowed but noisy  : (Transaction t) -> ...");

        banner("2. Same lambda text, DIFFERENT meaning (target type decides)");

        // `x -> x * 2` is just text until the compiler sees WHAT it must become.
        // Here the identical-looking lambda is typed three different ways:
        Function<Integer, Integer> doubleInt   = x -> x * 2;   // Integer -> Integer
        Function<BigDecimal, BigDecimal> dblBd = x -> x.multiply(new BigDecimal(2)); // BigDecimal version
        System.out.println("as Function<Integer,Integer> : 21 -> " + doubleInt.apply(21));
        System.out.println("BigDecimal needs a different body: 10.50 -> " + dblBd.apply(new BigDecimal("10.50")));
        System.out.println("=> the lambda's TYPES come from the functional interface, not from the arrow.");

        banner("3. Using them");

        Transaction payroll = Transaction.sampleStatement().get(0);
        System.out.println("isDeposit.test(payroll)          = " + isDeposit.test(payroll));
        System.out.println("isDepositBlock.test(payroll)     = " + isDepositBlock.test(payroll));
        System.out.println("amountOf.apply(payroll)          = " + amountOf.apply(payroll));
        System.out.println("byAmount.compare(payroll, rent)  = "
                + byAmount.compare(payroll, Transaction.sampleStatement().get(1))
                + "  (positive => payroll is bigger)");
        System.out.println("zero.get()                       = " + zero.get());
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
