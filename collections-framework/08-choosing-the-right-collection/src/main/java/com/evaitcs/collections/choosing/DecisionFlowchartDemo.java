package com.evaitcs.collections.choosing;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Demo 2 — THE DECISION FLOWCHART, walked: five real SecureBank questions, five picks.
 *
 * Choosing a collection is never "which class do I like" — it is a short interrogation
 * about the GUARANTEE you need, in this order:
 *
 *   1. Key -> value?            yes: Map  (sorted by key? TreeMap : HashMap)
 *   2. Duplicates forbidden?    yes: Set  (which order? sorted: TreeSet, insertion: LinkedHashSet,
 *                                          don't care: HashSet)
 *   3. Sequence with duplicates?       List (indexes? ArrayList; both ends? ArrayDeque;
 *                                          heavy middle churn? LinkedList — rare)
 *   4. "Always take the MIN next"?     PriorityQueue (and accept: iteration order is meaningless)
 *   5. Shared by multiple threads?     STOP. None of the above is safe — modules 09 and 10.
 *
 * Interface first, implementation second (module 01): the questions pick the INTERFACE
 * (the guarantee you owe), the Big-O table picks the IMPLEMENTATION (the access pattern
 * you have). Every scenario below declares with the interface and instantiates once.
 */
public class DecisionFlowchartDemo {

    public static void main(String[] args) {
        banner("1. The flowchart as text (the README draws the same thing in mermaid)");

        System.out.println("need key -> value?         yes -> Map: sorted keys? TreeMap : HashMap   (module 05)");
        System.out.println("duplicates forbidden?      yes -> Set: which order?");
        System.out.println("                                sorted -> TreeSet | insertion -> LinkedHashSet | none -> HashSet  (04)");
        System.out.println("sequence, duplicates OK?   yes -> List: get(i)? ArrayList | both ends? ArrayDeque | middle churn? LinkedList  (02/06)");
        System.out.println("'smallest NEXT', forever?  yes -> PriorityQueue — iteration order is meaningless  (06)");
        System.out.println("shared by threads?         yes -> STOP: modules 09/10 (ConcurrentHashMap, BlockingQueue, ...)");

        banner("2. Scenario 1 — statement lines for one account");

        scenario("Render the statement: chronological, duplicates allowed, row 7 must be get(7).",
                "key->value? NO | unique? NO — the same amount CAN recur | indexes? YES",
                "ArrayList<Transaction>",
                "insertion order IS the chronology; get(i) O(1); add at tail O(1) amortized.");
        List<Transaction> statement = new ArrayList<Transaction>(Transaction.sampleStatement());
        System.out.println("   proof: two identical bus rides (same 2.75 amount) both kept -> size " + statement.size()
                + ", get(1) = " + statement.get(1).getId());

        banner("3. Scenario 2 — unique customer ids from a raw feed, first-seen order");

        scenario("Deduplicate a messy feed of customer ids, but REPORT them in first-seen order.",
                "key->value? NO | unique? YES | order? insertion (first seen), not sorted",
                "LinkedHashSet<String>",
                "Set kills duplicates via equals/hashCode (module 04); Linked keeps YOUR order.");
        Set<String> seen = new LinkedHashSet<String>();
        String[] feed = { "C-07", "C-03", "C-07", "C-11", "C-03" };
        for (String id : feed) {
            seen.add(id);
        }
        System.out.println("   proof: feed " + Arrays.asList(feed) + " -> set " + seen + " (dups gone, order kept)");

        banner("4. Scenario 3 — id -> Account lookup, one hop");

        scenario("Given 'ACC-1011', produce the Account. Millions of lookups per night.",
                "key->value? YES | sorted report needed? NO (that would be TreeMap)",
                "HashMap<String, Account>",
                "get(key) is O(1) — hash the key, one bucket, done (module 05). Scanning a List would be O(n) PER lookup.");
        Map<String, Account> byId = new HashMap<String, Account>((int) (3 / 0.75f) + 1);
        for (Account a : Account.sampleAccounts()) {
            byId.put(a.getId(), a);
        }
        System.out.println("   proof: byId.get(\"ACC-1011\") -> " + byId.get("ACC-1011"));

        banner("5. Scenario 4 — incoming tasks, processed in arrival order");

        scenario("A worker loop takes tasks as they arrive; newest cancellations jump the queue at the head.",
                "key->value? NO | unique? NO | order? arrival (FIFO) — and BOTH ends are used",
                "ArrayDeque<Transaction>",
                "Queue career: offer tail / poll head, O(1) both (module 06). The head-insert need is why not ArrayList: add(0,..) there is an O(n) shift.");
        Queue<Transaction> tasks = new ArrayDeque<Transaction>();
        tasks.offer(new Transaction("T-1001", "Bus", new BigDecimal("2.75"), 1000L));
        tasks.offer(new Transaction("T-1002", "Coffee", new BigDecimal("4.50"), 2000L));
        System.out.println("   proof: FIFO drain -> " + tasks.poll().getId() + ", then " + tasks.poll().getId());

        banner("6. Scenario 5 — charge the cheapest fee first");

        scenario("The fee engine must ALWAYS process the smallest pending fee next.",
                "key->value? NO | unique? NO | order? not arrival — by VALUE, min first, repeatedly",
                "PriorityQueue<Transaction> with a comparator",
                "'smallest next' is the heap's whole job: peek O(1), offer/poll O(log n) (module 06). Iteration order is meaningless — never print it and panic.");
        // Anonymous comparator, classic idiom (core-java 10; no lambdas in main code).
        Queue<Transaction> fees = new PriorityQueue<Transaction>(11, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction a, Transaction b) {
                return a.getAmount().compareTo(b.getAmount());
            }
        });
        fees.offer(new Transaction("T-2001", "ATM fee", new BigDecimal("8.00"), 5000L));
        fees.offer(new Transaction("T-2004", "Paper fee", new BigDecimal("2.00"), 6000L));
        System.out.println("   proof: peek() = " + fees.peek().getDescription() + " " + fees.peek().getAmount().toPlainString() + " (the 2.00 one — the min)");

        banner("7. The question that changes everything — 'will threads share it?'");

        System.out.println("Every pick above assumes ONE thread. The moment a second thread reads while the");
        System.out.println("first writes, all nine rows of the Big-O table are the wrong table: ArrayList's");
        System.out.println("iterator throws ConcurrentModificationException (module 09's anatomy lesson), and");
        System.out.println("HashMap.put during a resize can even lose entries. The answers then live in");
        System.out.println("java.util.concurrent: CopyOnWriteArrayList, ConcurrentHashMap, BlockingQueue — module 10.");
    }

    /** Prints one decision: the question asked, the flowchart answers, the pick, the reason. */
    private static void scenario(String need, String answers, String pick, String why) {
        System.out.println("   need      : " + need);
        System.out.println("   flowchart : " + answers);
        System.out.println("   PICK      : " + pick);
        System.out.println("   because   : " + why);
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
