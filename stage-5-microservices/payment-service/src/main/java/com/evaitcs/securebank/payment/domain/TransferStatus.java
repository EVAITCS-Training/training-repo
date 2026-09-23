package com.evaitcs.securebank.payment.domain;

/**
 * The saga's state machine. Every terminal state tells an operator EXACTLY
 * how far the money got — that observability is the whole point of
 * persisting the saga (versus an in-memory orchestration).
 *
 *   PENDING ──debit ok──> (crediting) ──credit ok──> COMPLETED
 *      │                                   │
 *      ├─debit refused/unavailable─> FAILED│       (nothing moved)
 *      │                                   └─credit failed─> compensate debit
 *      │                                        ├─ undo ok ──> COMPENSATED
 *      │                                        └─ undo FAILED ─> COMPENSATION_FAILED
 *      └─ownership invalid─> FAILED
 *
 * COMPENSATION_FAILED is the alarm state: money left the source account and
 * the undo did not land. It must page a human (reconciliation job retries or
 * an operator fixes it). Systems that cannot NAME this state are systems
 * that lose money silently.
 */
public enum TransferStatus {
    PENDING,
    COMPLETED,
    FAILED,
    COMPENSATED,
    COMPENSATION_FAILED
}
