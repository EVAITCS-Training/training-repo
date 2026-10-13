package com.evaitcs.java8.lambdas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 01-lambdas test
 * Each test prints the evidence it asserts, so the test run doubles as a demo.
 */
class LambdaBasicsTest {

    private final List<Transaction> statement = Transaction.sampleStatement();

    @Test
    @DisplayName("a lambda IS a value: store it, pass it around, call it later")
    void lambdaIsAValue() {
        Predicate<Transaction> isDeposit = t -> t.isDeposit();
        List<Predicate<Transaction>> policies = Arrays.asList(isDeposit, isDeposit.negate());

        System.out.println("stored two predicates in a list and ran them on T-101 (a deposit):");
        System.out.println("  policies.get(0).test(..) = " + policies.get(0).test(statement.get(0)));
        System.out.println("  policies.get(1).test(..) = " + policies.get(1).test(statement.get(0)));

        assertTrue(policies.get(0).test(statement.get(0)));
        assertFalse(policies.get(1).test(statement.get(0)));
    }

    @Test
    @DisplayName("expression body and block body are equivalent")
    void expressionVsBlockBody() {
        Predicate<Transaction> expression = t -> t.getAmount().compareTo(new BigDecimal("100")) > 0;
        Predicate<Transaction> block = t -> {
            BigDecimal amount = t.getAmount();
            return amount.compareTo(new BigDecimal("100")) > 0;
        };
        for (Transaction t : statement) {
            assertEquals(expression.test(t), block.test(t), "bodies must agree on " + t.getId());
        }
        System.out.println("expression and block bodies agreed on all " + statement.size() + " transactions");
    }

    @Test
    @DisplayName("all three comparator styles from AnonymousVsLambdaDemo sort identically")
    void allComparatorStylesAgree() {
        Comparator<Transaction> anonymous = new Comparator<Transaction>() {
            @Override
            public int compare(Transaction t1, Transaction t2) {
                return t1.getAmount().compareTo(t2.getAmount());
            }
        };
        Comparator<Transaction> lambda = (t1, t2) -> t1.getAmount().compareTo(t2.getAmount());
        Comparator<Transaction> reference = Comparator.comparing(Transaction::getAmount);

        List<String> anonymousOrder = order(anonymous);
        assertEquals(anonymousOrder, order(lambda));
        assertEquals(anonymousOrder, order(reference));
        System.out.println("anonymous class, lambda and :: all produced: " + anonymousOrder);
    }

    private List<String> order(Comparator<Transaction> c) {
        List<Transaction> copy = new ArrayList<>(statement);
        copy.sort(c);
        List<String> ids = new ArrayList<>();
        for (Transaction t : copy) {
            ids.add(t.getId());
        }
        return ids;
    }

    @Test
    @DisplayName("Supplier::new gives a FRESH object every call")
    void constructorReferenceMakesFreshObjects() {
        Supplier<List<Transaction>> factory = ArrayList::new;
        List<Transaction> a = factory.get();
        List<Transaction> b = factory.get();
        assertNotSame(a, b, "each get() must build a new list");
        a.add(statement.get(0));
        assertEquals(0, b.size(), "mutating one must not affect the other");
        System.out.println("ArrayList::new produced two independent lists (a.size=" + a.size() + ", b.size=" + b.size() + ")");
    }

    @Test
    @DisplayName("Function composition: andThen is left-to-right, compose is right-to-left")
    void andThenVsCompose() {
        Function<Integer, Integer> plusOne = x -> x + 1;
        Function<Integer, Integer> timesTen = x -> x * 10;

        // andThen: plusOne FIRST, then timesTen  => (1+1)*10 = 20
        assertEquals(Integer.valueOf(20), plusOne.andThen(timesTen).apply(1));
        // compose: timesTen FIRST, then plusOne  => 1*10+1 = 11
        assertEquals(Integer.valueOf(11), plusOne.compose(timesTen).apply(1));
        System.out.println("1 plusOne.andThen(timesTen) = 20 ; plusOne.compose(timesTen) = 11");
    }

    @Test
    @DisplayName("captured local is a COPY frozen at creation; fields stay live")
    void captureSemantics() {
        BigDecimal[] captured = { new BigDecimal("10") };

        // array trick to show what the compiler FORBIDS on plain locals:
        // the lambda captured the ARRAY REFERENCE (effectively final), not the slot's value.
        // Mutating the array's CONTENT is allowed — reassigning the variable would not be.
        Supplier<BigDecimal> readsArray = () -> captured[0];

        captured[0] = new BigDecimal("99"); // reassigning `captured` itself would NOT compile
        assertEquals(new BigDecimal("99"), readsArray.get());
        System.out.println("lambda sees content changes through the captured reference: " + readsArray.get());
        System.out.println("(but `captured = other;` after capture is the compile error from EffectivelyFinalDemo)");
    }

    @Test
    @DisplayName("own @FunctionalInterface works exactly like the built-ins")
    void ownFunctionalInterface() {
        OwnFunctionalInterfaceDemo.MoneyPolicy under100 = a -> a.compareTo(new BigDecimal("100")) <= 0;
        List<Transaction> kept = OwnFunctionalInterfaceDemo.keepIf(statement, under100);
        assertEquals(3, kept.size()); // groceries 86.40, interest 3.11, refund 45.99
        for (Transaction t : kept) {
            System.out.println("  kept: " + t);
        }
    }
}
