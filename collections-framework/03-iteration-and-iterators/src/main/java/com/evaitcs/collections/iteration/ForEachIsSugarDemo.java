package com.evaitcs.collections.iteration;

import java.util.Iterator;
import java.util.List;

/**
 * Demo 2 — for-each IS COMPILER SUGAR, and the syrup is Iterator.
 *
 * The language spec rewrites `for (Transaction t : statement) { body }` into exactly:
 *
 *     Iterator&lt;Transaction&gt; it = statement.iterator();
 *     while (it.hasNext()) {
 *         Transaction t = it.next();
 *         body
 *     }
 *
 * One iterator() call, then hasNext()/next() until exhausted — which is why EVERYTHING that
 * implements Iterable can be for-eached (every Collection: Sets in module 04, a Map's keySet
 * in module 05, Queues in module 06). Arrays are the one exception: `for (Transaction t : arr)`
 * desugars to an indexed loop, because arrays have no iterator.
 *
 * The consequence Demo 4 cashes in: the sugar declares the iterator variable FOR you, inside
 * the machine — so the loop body has no name to call remove() on, and a removal through the
 * list is a structural change the iterator is never told about.
 */
public class ForEachIsSugarDemo {

    public static void main(String[] args) {
        banner("1. What you write vs what javac emits");

        System.out.println("you write :");
        System.out.println("    for (Transaction t : statement) {");
        System.out.println("        audit(t);");
        System.out.println("    }");
        System.out.println("javac emits:");
        System.out.println("    Iterator<Transaction> it = statement.iterator();");
        System.out.println("    while (it.hasNext()) {");
        System.out.println("        Transaction t = it.next();");
        System.out.println("        audit(t);");
        System.out.println("    }");

        banner("2. Side by side, same list — identical output");

        List<Transaction> statement = Transaction.sampleStatement();

        StringBuilder viaSugar = new StringBuilder();
        for (Transaction t : statement) {
            viaSugar.append(t.getId()).append(' ');
        }

        StringBuilder viaMachine = new StringBuilder();
        Iterator<Transaction> it = statement.iterator();
        while (it.hasNext()) {
            Transaction t = it.next();
            viaMachine.append(t.getId()).append(' ');
        }

        System.out.println("for-each       : " + viaSugar.toString().trim());
        System.out.println("hand-desugared : " + viaMachine.toString().trim());
        System.out.println("identical? " + viaSugar.toString().equals(viaMachine.toString())
                + "   <- they are the same code; the compiler wrote the second one for you");

        banner("3. Driving the machine by hand — every call the for-each makes");

        Iterator<Transaction> manual = statement.iterator();
        int step = 0;
        while (manual.hasNext()) {                   // hasNext() answers "is there one?" — it does NOT return the element
            Transaction t = manual.next();           // next() returns it AND advances the cursor
            step++;
            System.out.println("  hasNext() -> true; next() -> " + t.getId() + "   (call #" + step + ")");
        }
        System.out.println("  hasNext() -> false; the while ends. Total next() calls: " + step + " = size() = " + statement.size());
        System.out.println("The for-each made exactly these calls, in this order, on this kind of iterator — behind the colon.");

        banner("4. What the sugar hides — and why it matters");

        System.out.println("In the desugared form the variable `it` EXISTS — you could call it.remove() (Demo 3).");
        System.out.println("In the for-each form there is no name for the iterator: the only remove() in reach is the LIST's,");
        System.out.println("and a removal through the list is one the iterator is never told about — Demo 4's explosion.");
        System.out.println("Rule: for-each for READING; spell the Iterator out the moment the walk must MUTATE.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
