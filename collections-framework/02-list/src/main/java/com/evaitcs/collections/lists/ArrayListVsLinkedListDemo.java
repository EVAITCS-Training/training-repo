package com.evaitcs.collections.lists;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Demo 1 — TWO ENGINES BEHIND ONE INTERFACE: ArrayList (a growable array) vs LinkedList
 * (a chain of nodes).
 *
 * This module never reflects into java.util internals — modern JDKs refuse that
 * (InaccessibleObjectException), and honestly the source is the wrong teacher anyway. Instead:
 * the documented mechanics in words and diagrams (the README's mermaid pictures), then
 * OBSERVABLE BEHAVIOR that only makes sense if those mechanics are true.
 *
 * ArrayList = ONE contiguous Object[] ("elementData") + a size counter:
 *   - get(i) IS elementData[i] — O(1).
 *   - add(e) at the end is amortized O(1): when the array is full it is replaced by a ~1.5x
 *     larger one (10 -> 15 -> 22 -> ...) and everything is copied across — the occasional
 *     expensive copy pays for many cheap adds.
 *   - middle insert/remove must SHIFT every element right of the position — O(n).
 *
 * LinkedList = doubly-linked nodes (prev | element | next):
 *   - add/remove at the two ends is O(1) pointer re-linking.
 *   - every positional operation first WALKS the chain from the nearer end — O(n) hops.
 *   - each element pays for a Node object (~40 bytes on a 64-bit JVM vs ~8 per element in an
 *     ArrayList), and the nodes are scattered over the heap — every hop can be a cache miss.
 *
 * Hence the punchline, measured in banner 5: as a List, ArrayList wins almost always — even
 * where the Big-O says "tie". LinkedList's real calling is the Deque (module 06, where
 * ArrayDeque usually beats it anyway).
 */
public class ArrayListVsLinkedListDemo {

    private static final int N = 20_000;             // rows per engine — big enough for the shape, small enough for class
    private static final int GETS = 2_000;           // scattered reads
    private static final int FRONT_INSERTS = 5_000;  // add(0, e) operations — the shifting tax

    /** Running total so the JIT cannot optimize the reads away; also proves both engines did identical work. */
    private static long checksum;

    public static void main(String[] args) {
        banner("1. One interface, two engines — the code above the `new` is identical");

        List<Transaction> arrayEngine = Transaction.sampleStatement();                       // returns a fresh ArrayList
        List<Transaction> nodeEngine = new LinkedList<Transaction>(Transaction.sampleStatement());
        System.out.println("ArrayList  : " + arrayEngine);
        System.out.println("LinkedList : " + nodeEngine);
        System.out.println("arrayEngine.equals(nodeEngine) = " + arrayEngine.equals(nodeEngine)
                + "   <- List.equals compares elements+order, never the engine");
        System.out.println("Module 01's promise cashed: the declaration hides the engine; swapping it is the ONE line that says `new`.");

        banner("2. ArrayList inside — one array, two numbers (no reflection; the documented mechanics)");

        System.out.println("new ArrayList<Transaction>() starts EMPTY (Java 8+); the FIRST add() allocates capacity 10.");
        System.out.println("After 3 adds:");
        System.out.println("  elementData : [TXN-1][TXN-2][TXN-3][null][null][null][null][null][null][null]");
        System.out.println("  size = 3       <- what add()/get()/remove() consult");
        System.out.println("  capacity = 10  <- elementData.length; 7 slots paid for, unused");
        System.out.println();
        System.out.println("add() when size == capacity -> GROW: newCapacity = oldCapacity + (oldCapacity >> 1), i.e. ~1.5x:");
        System.out.println("  10 -> 15 -> 22 -> 33 -> 49 -> 73 -> 109 -> 163 -> 244 -> 366 -> 549 -> 823 -> 1234");
        System.out.println("1000 adds = 12 grow-and-copy events, ~2,456 slots copied in total -> fewer than 3 moves per add.");
        System.out.println("THAT is 'amortized O(1)': the occasional expensive growth pays for many cheap adds.");
        System.out.println("Know the size up front? new ArrayList<Transaction>(100000) skips the ladder — RemovePitfallsDemo banner 3.");

        banner("3. LinkedList inside — nodes and pointers");

        System.out.println("size = 3; two handles into the chain: first, last");
        System.out.println("  null <- [prev|TXN-1|next] <-> [prev|TXN-2|next] <-> [prev|TXN-3|next] -> null");
        System.out.println("          ^ first                                    ^ last");
        System.out.println();
        System.out.println("get(i)  : walk min(i, size-1-i) hops from the NEARER end — O(n), one pointer dereference per hop.");
        System.out.println("ends    : add(0,e) / add(e) / remove(0) / remove(size-1) = O(1) re-linking — the chain's one gift.");
        System.out.println("memory  : every element sits in a Node = header + prev + next + element ~= 40 bytes;");
        System.out.println("          ArrayList charges ~8 bytes per element (one reference), plus spare capacity.");
        System.out.println("locality: the array is ONE contiguous block — a cache line fetches ~8 neighbors for free;");
        System.out.println("          nodes are scattered over the heap — every hop can be a cache miss.");

        banner("4. The same middle surgery, two physics");

        List<String> shifting = new ArrayList<String>(Arrays.asList("TXN-1", "TXN-2", "TXN-3", "TXN-4", "TXN-5"));
        System.out.println("ArrayList.remove(2)  before : " + shifting);
        // The mechanics: System.arraycopy slides TXN-4,TXN-5 one slot LEFT; the freed tail slot is
        // set to null so the GC can reclaim the reference (forgetting that nulling is a classic leak);
        // size--. The whole procedure is what module 01 did BY HAND to an array.
        shifting.remove(2);
        System.out.println("ArrayList.remove(2)  after  : " + shifting + "   <- the tail SHIFTED left — O(n)");

        List<String> relinking = new LinkedList<String>(Arrays.asList("TXN-1", "TXN-2", "TXN-3", "TXN-4", "TXN-5"));
        System.out.println("LinkedList.remove(2) before : " + relinking);
        // The mechanics: walk 1 hop from the nearer end to the TXN-3 node, then unlink it:
        // TXN-2.next = TXN-4; TXN-4.prev = TXN-2. Nothing slides — there is no array.
        relinking.remove(2);
        System.out.println("LinkedList.remove(2) after  : " + relinking + "   <- two pointers REWIRED — but the walk to find them was O(n)");

        banner("5. Same operations, two engines — measured");

        final List<Transaction> array = new ArrayList<Transaction>();
        final List<Transaction> nodes = new LinkedList<Transaction>();

        // TIMING CAVEAT: absolute milliseconds depend on the machine, JIT warm-up and the GC —
        // which is why ListBehaviorTest NEVER asserts timings, only structure. What is stable
        // across runs is the SHAPE: which engine wins each row, and by roughly how much.
        // This is a classroom illustration, not a JMH benchmark.
        long addEndAl = timeMillis(new Runnable() {
            @Override
            public void run() {
                fill(array, N);
            }
        });
        long addEndLl = timeMillis(new Runnable() {
            @Override
            public void run() {
                fill(nodes, N);
            }
        });

        checksum = 0L;
        long getAl = timeMillis(new Runnable() {
            @Override
            public void run() {
                scatteredGets(array);
            }
        });
        long checksumAl = checksum;
        checksum = 0L;
        long getLl = timeMillis(new Runnable() {
            @Override
            public void run() {
                scatteredGets(nodes);
            }
        });
        System.out.println("both engines read the same slots: checksum " + checksumAl + " == " + checksum
                + " -> " + (checksumAl == checksum) + "  (identical work, identical result)");

        long frontAl = timeMillis(new Runnable() {
            @Override
            public void run() {
                frontInserts(array);
            }
        });
        long frontLl = timeMillis(new Runnable() {
            @Override
            public void run() {
                frontInserts(nodes);
            }
        });

        System.out.printf("%-36s %14s %14s%n", "operation", "ArrayList ms", "LinkedList ms");
        System.out.printf("%-36s %14d %14d%n", "add(e) x " + N + "  (end)", Long.valueOf(addEndAl), Long.valueOf(addEndLl));
        System.out.printf("%-36s %14d %14d%n", "get(i) x " + GETS + "  (scattered)", Long.valueOf(getAl), Long.valueOf(getLl));
        System.out.printf("%-36s %14d %14d%n", "add(0, e) x " + FRONT_INSERTS + "  (front)", Long.valueOf(frontAl), Long.valueOf(frontLl));
        System.out.println("(your machine prints its own milliseconds — the SHAPE is the lesson, and no test ever asserts these numbers)");

        banner("6. The verdict — and LinkedList's real job");

        System.out.println("Sizes after the measured phase: ArrayList " + array.size() + ", LinkedList " + nodes.size()
                + " — interchangeable results, incomparable costs.");
        System.out.println("Ends-only workload? LinkedList's O(1) re-linking still loses to cache misses in practice —");
        System.out.println("and 'insert/remove at the ends only' is Deque semantics: module 06, where ArrayDeque usually wins anyway.");
        System.out.println("Everything else — reads, middle surgery, memory — ArrayList wins on paper AND on the stopwatch.");
        System.out.println("Rule for the rest of the course: new ArrayList<T>() is the default List; deviate with evidence.");
    }

    /** Appends `rows` fixed transactions — identical data for both engines, so the comparison is honest. */
    static void fill(List<Transaction> list, int rows) {
        for (int i = 0; i < rows; i++) {
            list.add(new Transaction("TXN-" + i, "Bulk posting", new BigDecimal("10.00"), 1_700_000_000_000L + i));
        }
    }

    /** Deterministic pseudo-random reads; accumulates into checksum so the work cannot be optimized away. */
    static void scatteredGets(List<Transaction> list) {
        for (int i = 0; i < GETS; i++) {
            int index = (i * 7919) % list.size();   // 7919 is prime -> the walk covers the whole list
            checksum += list.get(index).getTimestampMillis();
        }
    }

    /** The operation ArrayList dreads: every add(0, e) shifts the ENTIRE array one slot right. */
    static void frontInserts(List<Transaction> list) {
        for (int i = 0; i < FRONT_INSERTS; i++) {
            list.add(0, new Transaction("TXN-HEAD-" + i, "Front posting", new BigDecimal("1.00"), 1_700_000_000_000L));
        }
    }

    /**
     * Passing behavior as a value, the pre-lambda way (the anonymous-inner-class idiom from
     * core-java) — java8-features/01-lambdas turns every call site above into a one-liner.
     */
    static long timeMillis(Runnable task) {
        long start = System.nanoTime();
        task.run();
        return (System.nanoTime() - start) / 1_000_000L;
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
