package com.evaitcs.securebank.web;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

/**
 * Tiny helper for reading OUR claims out of a verified Jwt.
 *
 * Controllers get the token via @AuthenticationPrincipal Jwt jwt — that is
 * the SAME token JwtService.issue(...) minted at login, now verified by the
 * JwtDecoder. Reading claims through one helper keeps claim names ("cid",
 * "role") from being stringly-typed all over the web layer.
 *
 * IDENTITY FLOWS THROUGH THE SYSTEM:
 *   login: AppUser row -> JWT claims (JwtService)
 *   every request after: JWT claims -> principal (here) -> ownership checks (services)
 * The database is only consulted at login; token verification is pure crypto.
 */
public final class JwtClaims {

    private JwtClaims() {
    }

    /** The customer id claim, or null for staff users (teller/admin). */
    public static Long customerId(Jwt jwt) {
        Number cid = jwt.getClaim("cid");
        return cid == null ? null : cid.longValue();
    }

    public static String role(Jwt jwt) {
        return jwt.getClaimAsString("role");
    }

    public static boolean isStaff(Jwt jwt) {
        return Set.of("TELLER", "ADMIN").contains(role(jwt));
    }

    /**
     * For customer-only endpoints: the verified customer id or a 401-style
     * failure. A CUSTOMER token ALWAYS has cid; if it doesn't, the token was
     * minted wrong — fail loudly rather than pass null downstream.
     */
    public static Long requireCustomerId(Jwt jwt) {
        Long cid = customerId(jwt);
        if (cid == null) {
            throw new IllegalStateException("Token for a customer must contain the cid claim");
        }
        return cid;
    }
}
