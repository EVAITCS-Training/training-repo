package com.evaitcs.java8.upgrades;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

/**
 * Collection upgrades: removeIf, forEach, and List.sort.
 *
 * WHY removeIf is the headline: EVERYONE has written the for-each + remove bug at least
 * once. We reproduce it live (caught!), explain WHY it explodes (the iterator's modCount
 * check), show the only pre-Java-8 safe way (iterator.remove), then the one-liner.
 */
public class RemoveIfDemo {

    public static void main(String[] args) {
        List<Transaction> statement = new ArrayList<>(Transaction.sampleStatement());

        System.out.println("=== The CLASSIC BUG: list.remove inside a for-each ===");
        try {
            for (Transaction t : statement) {
                if (t.isDeposit()) {
                    statement.remove(t);  // modifying the list WHILE the for-each iterates it
                }
            }
            System.out.println("   survived?! (removing the SECOND-TO-LAST element can silently pass!)");
        } catch (ConcurrentModificationException e) {
            // WHY: for-each is sugar over an Iterator. ArrayList counts modifications in
            // `modCount`; the iterator remembers `expectedModCount`. list.remove() bumps
            // modCount behind the iterator's back, so its next hasNext/next check fails.
            System.out.println("   BOOM -> " + e.getClass().getName());
            System.out.println("   the for-each iterator noticed the list changed underneath it");
        }

        System.out.println("=== The only SAFE pre-Java-8 way: iterator.remove ===");
        List<Transaction> viaIterator = new ArrayList<>(Transaction.sampleStatement());
        Iterator<Transaction> it = viaIterator.iterator();
        while (it.hasNext()) {
            Transaction t = it.next();
            if (t.isDeposit()) {
                it.remove();  // safe: the iterator removes it ITSELF and re-syncs expectedModCount
            }
        }
        System.out.println("   kept (no deposits): " + idsOf(viaIterator));

        System.out.println("=== Java 8: removeIf - one line, same result ===");
        List<Transaction> viaRemoveIf = new ArrayList<>(Transaction.sampleStatement());
        // The argument is a Predicate<Transaction> - straight out of module 01.
        boolean changed = viaRemoveIf.removeIf(Transaction::isDeposit);
        System.out.println("   kept (no deposits): " + idsOf(viaRemoveIf) + "   changed=" + changed);

        System.out.println("=== removeIf with a real business rule ===");
        List<Transaction> audited = new ArrayList<>(Transaction.sampleStatement());
        // "drop every withdrawal below the 100.00 audit threshold" - reads like the requirement:
        audited.removeIf(t -> !t.isDeposit()
                && t.getAmount().compareTo(new BigDecimal("100.00")) < 0);
        audited.forEach(t -> System.out.println("   kept: " + t));   // Iterable.forEach: a Consumer

        System.out.println("=== List.sort vs Collections.sort ===");
        List<Transaction> sorted = new ArrayList<>(audited);
        // Java 7:  Collections.sort(sorted, comparator);   // helper class first - reads backwards
        // Java 8:  sort is a DEFAULT METHOD on List itself (module 02!) - the list sorts ITSELF:
        sorted.sort(Comparator.comparing(Transaction::getAmount));
        System.out.println("   smallest first: " + sorted.get(0));
        System.out.println("   largest last  : " + sorted.get(sorted.size() - 1));
        Collections.sort(sorted, Comparator.comparing(Transaction::getAmount)); // still legal, same result

        // EVERY Iterable also gained spliterator() - a "splittable iterator" that can be
        // partitioned across threads. That is the engine under parallel streams: module 07.
    }

    private static List<String> idsOf(List<Transaction> txns) {
        List<String> ids = new ArrayList<>();
        for (Transaction t : txns) {
            ids.add(t.getId());
        }
        return ids;
    }
}
