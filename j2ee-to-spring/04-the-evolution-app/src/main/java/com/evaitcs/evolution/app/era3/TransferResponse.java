package com.evaitcs.evolution.app.era3;

import java.math.BigDecimal;

/**
 * ERA 3 — the response body of POST /api/v3/transfers, as a RECORD.
 *
 * The controller RETURNS this object; it never writes a byte. The Jackson-3 message
 * converter that @EnableWebMvc auto-registers (spring-webmvc 7 detects
 * tools.jackson.databind.ObjectMapper on the classpath) serializes it to
 * application/json. Eras 1 and 2 printed text lines with out.println — this record is
 * what "the framework owns the response format" means.
 *
 * The new balances ride along so the client sees the post-commit state in one call —
 * tests assert them exactly (they come from inside the transaction, so they ARE exact).
 */
public record TransferResponse(String reference, BigDecimal newFromBalance, BigDecimal newToBalance) {
}
