package com.evaitcs.securebank.notification.store;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory notification log — capped, thread-safe, newest-first.
 *
 * CONCURRENCY NOTE (Week 5 applied): Kafka listener threads add while HTTP
 * threads read, so the store is a ConcurrentLinkedDeque + AtomicLong — no
 * locks, no ConcurrentModificationException. Capacity is capped so a
 * long-running demo can't OOM the service; production would persist and page.
 */
@Component
public class NotificationStore {

    public record Notification(long id, String kind, String message, Instant at) {
    }

    private static final int CAPACITY = 200;

    private final ConcurrentLinkedDeque<Notification> items = new ConcurrentLinkedDeque<>();
    private final AtomicLong ids = new AtomicLong();

    /** Called by the Kafka listener thread. */
    public void add(String kind, String message) {
        items.addFirst(new Notification(ids.incrementAndGet(), kind, message, Instant.now()));
        while (items.size() > CAPACITY) {
            items.removeLast(); // evict oldest beyond the cap
        }
    }

    /** Snapshot for the HTTP read side (immutable copy — safe to serialize). */
    public List<Notification> recent() {
        return List.copyOf(items);
    }
}
