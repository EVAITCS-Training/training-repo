package com.evaitcs.collections.concurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Demo 5 — the four flavors of "queue full" and "queue empty".
 *
 * Module 06 taught the non-blocking pair: offer(e) answers false when full, poll() answers
 * null when empty — neither ever waits. BlockingQueue adds the blocking pair — put(e) parks
 * until a slot opens, take() parks until an element arrives — plus the timed middle ground
 * offer(e, timeout) / poll(timeout): wait a while, then give up with false/null.
 *
 * Four flavors, one decision: WHO should wait — nobody (offer/poll), the caller forever
 * (put/take), or the caller with a deadline (the timed pair)?
 *
 * DETERMINISM: banners 1–2 are single-threaded, so every answer is exact. Banner 3 must
 * show BLOCKING, which is inherently about time — so it follows the module's timing rule:
 * a real consumer thread parks in take(); main guarantees (with sleep(300)) that at least
 * ~300ms of parking happens before the waking offer; the printed verdict asserts a 150ms
 * FLOOR — a 2x margin that absorbs clock granularity and slow machines. The milliseconds
 * printed vary run to run; the verdict "take() really blocked" never does. Join before
 * reading anything the consumer wrote.
 */
public class BlockingFlavorsDemo {

    public static void main(String[] args) throws InterruptedException {
        banner("1. offer vs put — the queue-full side (capacity-2 ArrayBlockingQueue)");

        BlockingQueue<String> queue = new ArrayBlockingQueue<String>(2);   // BOUNDED at construction
        System.out.println("offer(TX-0001) -> " + queue.offer("TX-0001") + "   (room: accepted instantly)");
        System.out.println("offer(TX-0002) -> " + queue.offer("TX-0002") + "   (queue now FULL — capacity 2)");
        System.out.println("offer(TX-0003) -> " + queue.offer("TX-0003") + "  <- offer NEVER waits: full means false, right now");
        System.out.println("put(TX-0003)   -> would PARK this thread until a slot opens. Not called here —");
        System.out.println("                  demo 4's producer lives on exactly that parking, and banner 3");
        System.out.println("                  proves the take()-side parking with a real thread.");
        System.out.println("capacity held  : size = " + queue.size() + " — the constructor promise is never broken.");

        banner("2. poll vs take — the queue-empty side");

        System.out.println("poll() -> " + queue.poll() + "   (FIFO: the oldest element)");
        System.out.println("poll() -> " + queue.poll());
        System.out.println("poll() -> " + queue.poll() + "  <- empty: poll answers null IMMEDIATELY, never waits");
        String timed = queue.poll(100, TimeUnit.MILLISECONDS);             // the middle ground
        System.out.println("poll(100, MILLISECONDS) on the empty queue -> " + timed
                + "  (waited UP TO 100ms, then gave up — the deadline flavor)");
        System.out.println("take() on this empty queue would park INDEFINITELY until an element arrives.");
        System.out.println("That is a strong claim — banner 3 proves it with a parked thread and a stopwatch.");

        banner("3. Proof that take() BLOCKS — a parked consumer and a 300ms-late offer");

        final BlockingQueue<String> handoff = new ArrayBlockingQueue<String>(2);
        final CountDownLatch aboutToTake = new CountDownLatch(1);
        final long[] waitedMillis = new long[1];       // written by the consumer; main reads AFTER join
        final String[] received = new String[1];

        Thread consumer = new Thread(new Runnable() {  // classic idiom: anonymous Runnable
            @Override
            public void run() {
                try {
                    aboutToTake.countDown();           // "I am at take()'s doorstep"...
                    long t0 = System.currentTimeMillis();
                    received[0] = handoff.take();      // ...and now I PARK: the queue is empty
                    waitedMillis[0] = System.currentTimeMillis() - t0;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "blocked-consumer");

        consumer.start();
        aboutToTake.await();                           // do not start the clock before the consumer is at take()
        Thread.sleep(300);                             // generous: the consumer is certainly parked by now
        handoff.offer("ACC-PAYMENT");                  // the wakeup — take() returns with this value
        consumer.join();                               // DETERMINISM: judge only after join (happens-before)

        System.out.println("consumer received : " + received[0]);
        System.out.println("parked inside take(): " + waitedMillis[0] + "ms");
        System.out.println("verdict           : blocked >= 150ms -> " + (waitedMillis[0] >= 150));
        System.out.println("(the margin, explained: sleep(300) guarantees AT LEAST ~300ms of parking, and the");
        System.out.println("verdict's floor is 150ms — a 2x margin that absorbs clock granularity and slow");
        System.out.println("machines. The milliseconds printed vary run to run; the VERDICT never does.)");

        banner("4. The family, one line each");

        System.out.println("ArrayBlockingQueue(capacity [, fair]) — ONE array, ONE lock, two conditions (notFull/");
        System.out.println("    notEmpty); optional FAIR mode serves the longest-waiting thread first — and costs");
        System.out.println("    throughput, so it is opt-in.");
        System.out.println("LinkedBlockingQueue([capacity])       — linked nodes, TWO locks (producers and consumers");
        System.out.println("    do not contend at all); default capacity Integer.MAX_VALUE — an UNBOUNDED queue is");
        System.out.println("    an OutOfMemoryError with a delay: in production, ALWAYS pass a capacity.");
        System.out.println("PriorityBlockingQueue                 — heap order instead of FIFO: poll() returns the");
        System.out.println("    minimum (demo 6 banner 4). Unbounded; no nulls.");
        System.out.println("SynchronousQueue                      — capacity ZERO: every put parks until some take");
        System.out.println("    receives it. A hand-off, not a storage. (Handy, exotic — know the name.)");
        System.out.println("ConcurrentLinkedQueue                 — the NON-blocking sibling: lock-free CAS links,");
        System.out.println("    never parks anyone (demo 6 banner 1).");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
