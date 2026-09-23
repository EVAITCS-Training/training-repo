package com.evaitcs.securebank.notification.web;

import com.evaitcs.securebank.notification.store.NotificationStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * GET /notifications (publicly: /api/notifications via the gateway) —
 * the demo-visible proof that the event pipeline works: do a transfer in the
 * UI, then watch its receipt appear here WITHOUT any service calling this
 * one synchronously.
 *
 * SIMPLIFICATION, flagged for class: everyone sees ALL notifications here.
 * Production scopes them per customer (filter by the JWT's cid claim — the
 * events would carry customerId) and pages them. Deliberately minimal so the
 * pub/sub lesson stays the star.
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    public record NotificationResponse(long id, String kind, String message, Instant at) {
    }

    private final NotificationStore store;

    public NotificationController(NotificationStore store) {
        this.store = store;
    }

    @GetMapping
    public List<NotificationResponse> recent() {
        return store.recent().stream()
                .map(n -> new NotificationResponse(n.id(), n.kind(), n.message(), n.at()))
                .toList();
    }
}
