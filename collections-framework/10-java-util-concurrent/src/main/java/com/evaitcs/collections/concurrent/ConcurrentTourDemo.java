package com.evaitcs.collections.concurrent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Demo 6 — the rest of the shelf, one banner each, and the decision table to photograph.
 *
 * The tour, in the order the README's table lists them:
 *   - ConcurrentLinkedQueue  — lock-free FIFO: CAS on head/tail links, nobody ever parks;
 *                              and the size() trap: an O(n) walk that can be stale mid-count,
 *                              so isEmpty() is the question to ask;
 *   - ConcurrentSkipListMap  — the concurrent SORTED map: skip list (stacked lanes of linked
 *                              nodes), O(log n) like module 05's TreeMap, firstEntry/headMap
 *                              and the whole NavigableMap contract, no nulls (a null key
 *                              cannot be compared — core-java 10's ordering at work);
 *   - CopyOnWriteArraySet    — demo 1's copy-on-write engine behind the Set interface:
 *                              dedupe through equals (core-java 08), snapshot iterators;
 *   - PriorityBlockingQueue  — module 06's PriorityQueue grown up: binary heap, thread-safe,
 *                              take() blocks when empty, poll() returns the MINIMUM by
 *                              Comparator — FIFO is gone BY CONTRACT, not by accident.
 *
 * Everything here is single-threaded on purpose: these are STRUCTURAL guarantees (order,
 * dedupe, sortedness, min-first), and structure does not need a race to be proven. The
 * blocking behavior was demo 5's job; the atomic compounds were demo 2's.
 */
public class ConcurrentTourDemo {

    /**
     * Named Comparator — the classic idiom (core-java 10: natural order lives INSIDE the
     * class, custom orders OUTSIDE). Transaction's natural order is by id; the bank wants
     * "smallest amount first" here, so the order comes from outside. java8-features 01
     * shortens this whole class to (a, b) -> a.getAmount().compareTo(b.getAmount()).
     */
    static class ByAmount implements Comparator<Transaction> {
        @Override
        public int compare(Transaction a, Transaction b) {
            return a.getAmount().compareTo(b.getAmount());   // BigDecimal.compareTo — never subtract
        }
    }

    public static void main(String[] args) {
        banner("1. ConcurrentLinkedQueue — lock-free FIFO, and a size() you must not lean on");

        ConcurrentLinkedQueue<String> fifo = new ConcurrentLinkedQueue<String>();
        fifo.offer("TX-0001");
        fifo.offer("TX-0002");
        fifo.offer("TX-0003");
        List<String> drained = new ArrayList<String>();
        String polled;
        while ((polled = fifo.poll()) != null) {           // poll never blocks: null means empty
            drained.add(polled);
        }
        System.out.println("drain order : " + drained + " — FIFO");
        System.out.println("isEmpty()   : " + fifo.isEmpty() + " — the O(1) question");
        System.out.println("size()      : " + fifo.size() + " — and the trap: on THIS queue size() is an O(n)");
        System.out.println("              WALK of the chain, and concurrent writers can make the count stale");
        System.out.println("              mid-walk; its own javadoc warns it is not constant-time and the value");
        System.out.println("              may be out of date. Engine: CAS on head/tail links — no lock, ever.");
        System.out.println("              Unbounded. Ask isEmpty(); never gate logic on size().");

        banner("2. ConcurrentSkipListMap — the sorted one (TreeMap's concurrent sibling)");

        ConcurrentSkipListMap<String, Account> byId = new ConcurrentSkipListMap<String, Account>();
        for (Account a : Account.sampleAccounts()) {       // seeded 1027, 1005, 1011 — deliberately scrambled
            byId.put(a.getId(), a);
        }
        System.out.println("keys              : " + byId.keySet() + " — sorted on arrival, always");
        System.out.println("firstEntry()      : " + byId.firstEntry());
        System.out.println("lastEntry()       : " + byId.lastEntry());
        System.out.println("headMap(ACC-1011) : " + byId.headMap("ACC-1011").keySet()
                + " — live navigable views, the whole TreeMap contract (module 05)");
        System.out.println("Engine: a skip list — stacked lanes of linked nodes; a search rides the express");
        System.out.println("lane and drops down, O(log n) like a red-black tree, but a writer touches a few");
        System.out.println("nodes, never the whole structure. Order comes from the keys' natural ordering");
        System.out.println("(Comparable — core-java 10) or a Comparator you pass in. No nulls: a null key");
        System.out.println("cannot be compared, and get()==null must mean one thing (demo 3).");

        banner("3. CopyOnWriteArraySet — demo 1's engine behind the Set interface");

        CopyOnWriteArraySet<Account> watchers = new CopyOnWriteArraySet<Account>();
        boolean first = watchers.add(new Account("ACC-1027", "Dana", new BigDecimal("2450.00")));
        boolean dupe = watchers.add(new Account("ACC-1027", "Dana (re-imported feed row)", BigDecimal.ZERO));
        System.out.println("first add  -> " + first);
        System.out.println("second add -> " + dupe + "   (same id, different object)");
        System.out.println("size       -> " + watchers.size());
        System.out.println("The dedupe ran on equals() — Account's equals-by-id (core-java 08) decided the");
        System.out.println("re-imported row IS the same account. Under the hood: addIfAbsent over a");
        System.out.println("copy-on-write array — snapshot iterators, no CME, expensive writes (demo 1).");
        System.out.println("The natural home for subscriber/watcher lists: written at registration, read on");
        System.out.println("every event, duplicates impossible.");

        banner("4. PriorityBlockingQueue — poll() returns the MIN, not the oldest");

        PriorityBlockingQueue<Transaction> byAmount =
                new PriorityBlockingQueue<Transaction>(11, new ByAmount());   // 11 = initial capacity only
        byAmount.put(new Transaction("T-2001", "Rent", new BigDecimal("1200.00"), 1700000000000L));
        byAmount.put(new Transaction("T-2002", "Coffee", new BigDecimal("4.50"), 1700003600000L));
        byAmount.put(new Transaction("T-2003", "Salary refund", new BigDecimal("3100.00"), 1700007200000L));
        System.out.println("insertion order   : [T-2001 (1200.00), T-2002 (4.50), T-2003 (3100.00)]");
        List<String> order = new ArrayList<String>();
        Transaction next;
        while ((next = byAmount.poll()) != null) {
            order.add(next.getId() + "@" + next.getAmount().toPlainString());
        }
        System.out.println("poll order        : " + order);
        System.out.println("Minimum-first by the ByAmount comparator — FIFO is gone BY CONTRACT. This is");
        System.out.println("module 06's PriorityQueue grown up: same binary heap, thread-safe, and take()");
        System.out.println("blocks when the heap is empty. Rules: unbounded (the capacity argument is only");
        System.out.println("the initial array), no nulls, and keep elements IMMUTABLE — mutating a queued");
        System.out.println("element corrupts the heap order (the comparator contract, core-java 10).");

        banner("5. The decision table — which concurrent collection, when (photograph this)");

        System.out.printf("%-24s %-24s %-30s %-12s %s%n",
                "class", "shape / order", "iterator", "nulls", "choose it when");
        System.out.printf("%-24s %-24s %-30s %-12s %s%n",
                "-----------------------", "-----------------------", "-----------------------------",
                "-----------", "--------------------------------------");
        System.out.printf("%-24s %-24s %-30s %-12s %s%n",
                "CopyOnWriteArrayList", "List, insertion order", "frozen snapshot, never CME", "allowed",
                "rare writes, many reads: listeners");
        System.out.printf("%-24s %-24s %-30s %-12s %s%n",
                "CopyOnWriteArraySet", "Set, dedupe by equals", "frozen snapshot, never CME", "allowed",
                "a deduplicated watcher list");
        System.out.printf("%-24s %-24s %-30s %-12s %s%n",
                "ConcurrentHashMap", "Map, unordered", "weakly consistent, never CME", "NEVER",
                "the default shared map; use its atomic compounds");
        System.out.printf("%-24s %-24s %-30s %-12s %s%n",
                "ConcurrentSkipListMap", "Map, SORTED", "weakly consistent, never CME", "NEVER",
                "shared map + sorted/navigable views");
        System.out.printf("%-24s %-24s %-30s %-12s %s%n",
                "ConcurrentLinkedQueue", "Queue, FIFO", "weakly consistent, never CME", "NEVER",
                "lock-free unbounded FIFO; ask isEmpty()");
        System.out.printf("%-24s %-24s %-30s %-12s %s%n",
                "ArrayBlockingQueue", "Queue, FIFO, BOUNDED", "—", "NEVER",
                "fixed buffer + backpressure; optional fairness");
        System.out.printf("%-24s %-24s %-30s %-12s %s%n",
                "LinkedBlockingQueue", "Queue, FIFO, two locks", "—", "NEVER",
                "high-throughput hand-off; ALWAYS bound it");
        System.out.printf("%-24s %-24s %-30s %-12s %s%n",
                "PriorityBlockingQueue", "Queue, by Comparator", "—", "NEVER",
                "most-urgent-first across threads");
        System.out.println();
        System.out.println("Two laws behind the whole table: concurrent iterators NEVER throw CME but promise");
        System.out.println("nothing about freshness (weakly consistent / snapshot), and nulls are banned");
        System.out.println("wherever get()/poll() must have exactly one meaning. Pick the STRUCTURE first");
        System.out.println("(List/Set/Map/Queue — module 08's habit), then the ACCESS PATTERN.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
