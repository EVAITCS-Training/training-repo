package com.evaitcs.securebank.web.dto;

import java.time.Instant;

/**
 * Login success payload. The frontend stores `token` and attaches it as
 * "Authorization: Bearer <token>" on every subsequent request (Stage 3:
 * src/api/client.ts does exactly this).
 */
public record LoginResponse(
        String token,
        String tokenType,     // always "Bearer" — the Authorization scheme
        Instant expiresAt,    // lets the UI proactively re-login before expiry
        String username,
        String role,          // drives which UI screens the user sees
        Long customerId       // null for staff (teller/admin)
) {
}
