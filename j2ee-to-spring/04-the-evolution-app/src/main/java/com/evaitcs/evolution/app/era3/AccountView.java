package com.evaitcs.evolution.app.era3;

import java.math.BigDecimal;

/**
 * ERA 3 — the response body of GET /api/v3/accounts/{id}, as a RECORD projection.
 *
 * A record, not an entity: the API returns exactly three fields, and nothing else can
 * leak. (Era 2's Hibernate world would return a managed Account; module 02's
 * AccountSummary made the same projection point.) `{id}` in the URL becomes the `long id`
 * method parameter through Spring's path-variable conversion — era 1 parsed the same
 * value with Long.parseLong and a hand-decided 400.
 */
public record AccountView(long id, String type, BigDecimal balance) {
}
