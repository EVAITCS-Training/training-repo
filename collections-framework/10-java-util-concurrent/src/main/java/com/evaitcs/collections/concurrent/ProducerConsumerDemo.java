package com.evaitcs.collections.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Demo 4 — BlockingQueue: the hand-off that ends hand-rolled wait/notify.
 *
 * A BlockingQueue adds two BLOCKING operations to module 06's Queue vocabulary:
 * put(e) parks the caller while the queue is FULL (backpressure — the consumer sets the
 * pace); take() parks while it is EMPTY (no busy-wait polling — the producer sets the
 * pace). That is the entire producer/consumer pattern as a data structure: no wait(), no
 * notify(), no lost-wakeup bugs, no hand-rolled locking — module 09's "protect the
 * sequence" advice, pre-built and tested.
 *
 * The shutdown idiom is the POISON PILL: a sentinel value the producer offers LAST and
 * the consumer treats as "no more work" (one pill per consumer thread — this pipeline has
 * exactly one).
 *
 * DETERMINISM, in three decisions:
 *   1. ONE producer, ONE consumer — a BlockingQueue hands each element to exactly one
 *      taker, so the consumer's collection order is forced to be the producer's offer
 *      order: exact FIFO, every run;
 *   2. both threads are join()ed before ANY verdict is read (happens-before: the traces
 *      and the processed list are safely visible to main afterwards);
 *   3. the threads never print while running — console interleaving between two threads is
 *      scheduler-dependent, so each thread RECORDS its steps into its own list and main
 *      replays both traces: same content, stable order, every run.
 */
public class ProducerConsumerDemo {

    static final String PILL = "DONE";                 // the sentinel: "no more work"

    public static void main(String[] args) throws InterruptedException {
        banner("1. The pipeline — one producer, one consumer, one bounded queue between them");

        System.out.println("producer --put--> [ ArrayBlockingQueue, capacity 4 ] --take--> consumer");
        System.out.println("put() parks when the queue is full; take() parks when it is empty.");
        System.out.println("Neither thread polls, spins, or touches a lock by hand.");
        System.out.println();

        final BlockingQueue<String> queue = new ArrayBlockingQueue<String>(4);   // BOUNDED: backpressure ON
        final List<String> processed = new ArrayList<String>();        // touched ONLY by the consumer;
        final List<String> producerTrace = new ArrayList<String>();    // each trace only by its own thread;
        final List<String> consumerTrace = new ArrayList<String>();    // main reads them AFTER join

        Thread producer = new Thread(new Runnable() {                  // classic idiom: anonymous Runnable
            @Override
            public void run() {
                try {
                    for (int i = 1; i <= 10; i++) {
                        // Zero-padded ids — core-java 10's lexicographic trap: "TX-10" would
                        // sort BEFORE "TX-9". Padding keeps String order == numeric order.
                        String id = String.format("TX-%04d", i);
                        queue.put(id);                               // parks whenever the queue is full
                        producerTrace.add("put " + id);
                    }
                    queue.put(PILL);                                 // the pill goes LAST — the shutdown signal
                    producerTrace.add("put the poison pill " + PILL + "  ('no more work')");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "producer");

        Thread consumer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        String id = queue.take();                    // parks whenever the queue is empty
                        if (PILL.equals(id)) {                       // sentinel check BEFORE processing
                            consumerTrace.add("took the pill " + PILL + " — shutting down (the pill is not processed)");
                            break;
                        }
                        consumerTrace.add("took " + id + " -> posted to the ledger");
                        processed.add(id);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();                                             // DETERMINISM: verdicts only after join
        consumer.join();

        banner("2. The traces — recorded by the threads, replayed by main (stable console order)");

        System.out.println("producer:");
        for (String step : producerTrace) {
            System.out.println("  " + step);
        }
        System.out.println("consumer:");
        for (String step : consumerTrace) {
            System.out.println("  " + step);
        }
        System.out.println("(printing from inside the threads would interleave unpredictably on the");
        System.out.println("console — recording per thread and replaying after join keeps the evidence stable.)");

        banner("3. The verdict");

        List<String> expected = new ArrayList<String>();
        for (int i = 1; i <= 10; i++) {
            expected.add(String.format("TX-%04d", i));
        }
        System.out.println("consumer processed : " + processed);
        System.out.println("exact FIFO order   : " + processed.equals(expected)
                + " — with ONE producer and ONE consumer the queue is a strictly ordered pipe");
        System.out.println("queue empty at end : " + queue.isEmpty() + " — the pill was consumed, not processed");
        System.out.println("(Several consumers would still lose and duplicate NOTHING — each element goes to");
        System.out.println("exactly one taker — but the order ACROSS consumers is the scheduler's business.");
        System.out.println("One pill per consumer thread; this pipeline has one of each.)");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
