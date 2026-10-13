package com.evaitcs.collections.why;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

/**
 * Demo 2 — THE ANSWER: a family of INTERFACES, not a pile of classes.
 *
 * The framework's whole design in one sentence: the interfaces (List, Set, Queue, Map)
 * are the GUARANTEES; the classes (ArrayList, HashSet, ...) are interchangeable
 * IMPLEMENTATIONS of those guarantees. You declare variables and parameters with the
 * interface and pick the implementation at the one line that says `new`.
 *
 * Two structural surprises printed below, because every class asks about them:
 *   - Map is NOT a Collection (key-value pairs don't fit "a bag of elements"),
 *     but it hands you Collections via keySet()/values()/entrySet() — module 05.
 *   - One class can implement SEVERAL interfaces: LinkedList is a List AND a Deque.
 *     The type you declare decides which methods you can even see.
 */
public class HierarchyDemo {

    public static void main(String[] args) {
        banner("1. The family tree (the mermaid diagram from the README, as text)");

        System.out.println("Iterable<E>                         <- 'can be walked with for-each'");
        System.out.println("  +-- Collection<E>                 <- add/remove/contains/size + iterator()");
        System.out.println("       |-- List<E>      ORDERED, INDEXED, DUPLICATES OK   -> ArrayList, LinkedList");
        System.out.println("       |-- Set<E>       NO DUPLICATES                     -> HashSet, LinkedHashSet, TreeSet");
        System.out.println("       +-- Queue<E>     PROCESS-IN-ORDER                  -> PriorityQueue, ArrayDeque");
        System.out.println("Map<K,V>  KEY -> VALUE, not a Collection — but its keySet()/values() ARE  (module 05)");

        banner("2. instanceof proof — one object, several guarantees");

        List<Account> accounts = new ArrayList<Account>();
        System.out.println("new ArrayList<Account>() is a List      ? " + (accounts instanceof List));
        System.out.println("                          is a Collection? " + (accounts instanceof Collection));
        System.out.println("                          is an Iterable ? " + (accounts instanceof Iterable));

        // LinkedList wears TWO hats: it is a List (indexed) AND a Deque/Queue (two-ended).
        // The interface you declare decides which hat is visible at the call site.
        LinkedList<Account> both = new LinkedList<Account>();
        System.out.println("new LinkedList<Account>() is a List     ? " + (both instanceof List));
        System.out.println("                            is a Queue    ? " + (both instanceof Queue));
        System.out.println("Declared as List  -> you see get(i)/add(i,e). Declared as Queue -> offer()/poll().");

        banner("3. Swap the implementation, keep the code — the whole point");

        // Same reference type, three implementations. Nothing below the `new` changes.
        Set<Account> insertionOrdered = new LinkedHashSet<Account>(Account.sampleAccounts());
        Set<Account> sorted = new TreeSet<Account>(new ById());
        // (TreeSet needs an order: Account has no Comparable, so we hand it a Comparator —
        //  core-java module 10. Without it: ClassCastException, exactly as taught there.)
        printOwners(insertionOrdered, "LinkedHashSet keeps INSERTION order");
        printOwners(sorted, "TreeSet    keeps SORTED order    ");
        System.out.println("printOwners did not change. It asked for a Set; both ARE Sets.");

        banner("4. Method parameters: the BROADEST interface that does the job");

        // Collection is enough for walking and counting -> accept List, Set, Queue alike.
        printOwners(new ArrayList<Account>(Account.sampleAccounts()), "an ArrayList passed as Collection");
        printOwners(insertionOrdered, "a Set      passed as Collection");
        System.out.println("Rule of thumb: parameters as broad as possible (Collection),");
        System.out.println("return types / fields as SPECIFIC as the guarantee you owe (List).");
        System.out.println("A method that needs get(0) must declare List — interfaces document what you may do.");
    }

    /** Needs only "walkable + countable" — so it takes the broadest useful interface. */
    static void printOwners(Collection<Account> accounts, String label) {
        StringBuilder owners = new StringBuilder();
        for (Account a : accounts) {              // for-each = Iterable in action (module 03)
            if (owners.length() > 0) {
                owners.append(", ");
            }
            owners.append(a.getOwner());
        }
        System.out.println(label + " -> [" + owners + "]");
    }

    /** Classic idiom on purpose: named Comparator, no lambdas (core-java 10 / java8-features 01). */
    static class ById implements java.util.Comparator<Account> {
        @Override
        public int compare(Account a, Account b) {
            return a.getId().compareTo(b.getId());
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
