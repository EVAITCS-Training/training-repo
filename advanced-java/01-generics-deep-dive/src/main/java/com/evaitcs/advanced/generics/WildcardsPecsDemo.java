package com.evaitcs.advanced.generics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Demo 2 — WILDCARDS AND PECS. (README section "Wildcards: 'some ONE type, unnamed'".)
 *
 * The problem this solves: generics are INVARIANT. List<Integer> is NOT a List<Number>,
 * even though Integer IS a Number — because a List<Number> must accept add(3.14), and a
 * List<Integer> must not. Arrays got this wrong (covariant, runtime crash —
 * collections-framework 01); generics refuse the unsound assignment at COMPILE time.
 *
 * Wildcards recover flexibility soundly:
 *   List<? extends Number>  "some ONE unknown Number subtype" — you may READ Numbers,
 *                           you may NOT add (except null): the compiler cannot prove
 *                           which subtype the list holds.        -> PRODUCER
 *   List<? super Integer>   "some ONE unknown supertype of Integer" — you may ADD
 *                           Integers, reads come back as Object.  -> CONSUMER
 *
 * PECS = Producer Extends, Consumer Super. Collections.copy(dest, src) in the JDK is
 * literally declared copy(List<? super T> dest, List<? extends T> src) — the standard
 * library teaches the rule in its signature.
 */
public class WildcardsPecsDemo {

    public static void main(String[] args) {
        banner("1. Invariance: the assignment that must NOT compile");

        // Uncomment to see the compiler reject it (kept as a comment so the demo runs):
        //   List<Number> numbers = new ArrayList<Integer>();   // COMPILE ERROR
        // WHY: numbers.add(3.14) would then be legal through the Number view,
        // poisoning a list the Integer view still trusts. The check is at COMPILE time —
        // that is the whole improvement over arrays (ArrayStoreException at runtime).
        System.out.println("step 1: List<Number> n = new ArrayList<Integer>()  -> COMPILE ERROR (invariance)");
        System.out.println("step 2: the reason: n.add(3.14) would be legal for the compiler,");
        System.out.println("        but the underlying list promised to hold only Integers.");
        System.out.println("step 3: arrays allowed the same shape (Object[] o = new Integer[3]) and crash LATER.");
        System.out.println("verdict: generics fail early and loudly — at the compiler, not in production.");

        banner("2. ? extends Number — the PRODUCER wildcard (read OUT of it)");

        List<Integer> counts = List.of(1, 2, 3);
        List<BigDecimal> amounts = List.of(new BigDecimal("10.50"), new BigDecimal("0.25"));
        System.out.println("sum of List<Integer>             = " + sum(counts));
        System.out.println("sum of List<BigDecimal>          = " + sum(amounts));
        System.out.println("ONE method, two element types — ? extends Number accepts both.");
        // counts.add(4) inside sum() would not compile: the list might be a List<BigDecimal>
        // as far as the method knows. Reading is safe (everything is at least a Number);
        // writing is unprovable. Producer = you TAKE from it = extends.

        banner("3. ? super Integer — the CONSUMER wildcard (write INTO it)");

        List<Number> sink = new ArrayList<>();
        fillWithOnes(sink, 3);
        List<Object> widerSink = new ArrayList<>();
        fillWithOnes(widerSink, 2);
        System.out.println("filled a List<Number> : " + sink);
        System.out.println("filled a List<Object> : " + widerSink);
        System.out.println("ONE method, two target types — ? super Integer accepts both.");
        // Inside fillWithOnes, list.add(1) compiles: whatever the unknown supertype is,
        // it can hold an Integer. Reading back yields Object only. Consumer = extends NOT.

        banner("4. The JDK teaches PECS in one signature: Collections.copy");

        List<Integer> src = List.of(7, 8, 9);
        List<Number> dest = new ArrayList<>(Collections.nCopies(3, 0));
        Collections.copy(dest, src);   // copy(List<? super T> dest, List<? extends T> src)
        System.out.println("Collections.copy(dest, src) -> dest = " + dest);
        System.out.println("src is the PRODUCER  -> ? extends T   (copy reads out of it)");
        System.out.println("dest is the CONSUMER -> ? super T     (copy writes into it)");

        banner("5. In one sentence");
        System.out.println("PECS: if the method READS from the collection, declare ? extends T;");
        System.out.println("if it WRITES into it, declare ? super T; if it does both, declare T exactly.");
    }

    /** PRODUCER: we only take numbers OUT. Works for every List of any Number subtype. */
    static BigDecimal sum(List<? extends Number> producer) {
        BigDecimal total = BigDecimal.ZERO;
        for (Number n : producer) {                       // read: always safe
            total = total.add(new BigDecimal(n.toString()));
        }
        return total;
    }

    /** CONSUMER: we only put Integers IN. Works for every List of any Integer supertype. */
    static void fillWithOnes(List<? super Integer> consumer, int howMany) {
        for (int i = 0; i < howMany; i++) {
            consumer.add(Integer.valueOf(1));             // write: always safe
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
