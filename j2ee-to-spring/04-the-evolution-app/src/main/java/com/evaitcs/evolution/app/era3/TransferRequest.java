package com.evaitcs.evolution.app.era3;

import java.math.BigDecimal;

/**
 * ERA 3 — the request body of POST /api/v3/transfers, as a RECORD.
 *
 * This one line is the era gap made visible: eras 1 and 2 parsed `from`, `to` and
 * `amount` out of form parameters with hand-written parseLong/parseAmount helpers and
 * hand-decided 400s. Here Jackson 3 (tools.jackson — records are natively supported,
 * no annotations needed) deserializes the JSON body into this record, and the
 * argument resolver hands it to the controller method already built. Malformed JSON
 * never reaches the controller: it surfaces as HttpMessageNotReadableException, which
 * BankApiAdvice turns into a 400 problem+json.
 *
 * stage-2 counterpart: com.evaitcs.securebank.web.dto.TransferRequest (plus @Valid —
 * bean validation is a starter there; here the service's own checks are the guard).
 */
public record TransferRequest(long from, long to, BigDecimal amount) {
}
