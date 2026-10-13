package com.evaitcs.java8.lambdas;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The four shapes of `::`. A method reference is NOT a new feature — it is shorthand for
 * a lambda whose whole body is one call to an existing method.
 *
 * Show each pair side by side: the lambda, then the same thing as `::`. The compiler
 * accepts `X::y` exactly where the equivalent lambda would fit the target type.
 */
public class MethodReferenceDemo {

    /** A tiny service with an instance method, so shape 2 has a real receiver. */
    static class AuditLog {
        void log(String message) {
            System.out.println("   [audit] " + message);
        }
    }

    static BigDecimal feeOf(Transaction t) {           // a static helper for shape 1
        return t.getAmount().multiply(new BigDecimal("0.015"));
    }

    public static void main(String[] args) {
        List<Transaction> statement = Transaction.sampleStatement();
        AuditLog auditLog = new AuditLog();

        System.out.println("=== Shape 1: STATIC method  Class::staticMethod ===");
        // lambda:   t -> feeOf(t)
        Function<Transaction, BigDecimal> fee2 = x -> feeOf(x);
        Function<Transaction, BigDecimal> fee = MethodReferenceDemo::feeOf;
        System.out.println("   fee of " + statement.get(0).getId() + " = " + fee.apply(statement.get(0)));

        System.out.println("=== Shape 2: instance method of a PARTICULAR object  object::instanceMethod ===");
        // lambda:   msg -> auditLog.log(msg)   — the receiver `auditLog` is CAPTURED, like any local
        Consumer<String> log = auditLog::log;
        log.accept("method reference bound to one specific AuditLog instance");

        System.out.println("=== Shape 3: instance method of ANY object of a type  Class::instanceMethod ===");
        // lambda:   t -> t.getAmount()   — the FIRST parameter becomes the RECEIVER.
        // This is the shape students find weirdest; say it slowly:
        // "Function<Transaction, BigDecimal> takes a Transaction; getAmount() is called ON it."
        Function<Transaction, BigDecimal> amount = Transaction::getAmount;
        Predicate<Transaction> deposit = Transaction::isDeposit;   // works for boolean methods too
        System.out.println("   amount of T-102 = " + amount.apply(statement.get(1)));
        System.out.println("   T-102 is deposit? " + deposit.test(statement.get(1)));

        System.out.println("=== Shape 4: CONSTRUCTOR  Class::new ===");
        // lambda:   () -> new ArrayList<Transaction>()
        Supplier<List<Transaction>> emptyStatement = ArrayList::new;
        List<Transaction> fresh = emptyStatement.get();
        System.out.println("   ArrayList::new produced an empty list of size " + fresh.size());

        System.out.println("=== Where you'll actually meet them: sorting ===");
        List<Transaction> sorted = new ArrayList<>(statement);
        // Read it out loud: "comparing transactions BY getAmount" — shape 3 doing real work.
        sorted.sort(Comparator.comparing(Transaction::getAmount));
        System.out.println("   smallest first : " + sorted.get(0));
        System.out.println("   largest last   : " + sorted.get(sorted.size() - 1));
    }
}
