package com.evaitcs.securebank.payment.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Claim helper — per-service copy (see account-service JwtClaims for docs). */
public final class JwtClaims {

    private JwtClaims() {
    }

    public static Long customerId(Jwt jwt) {
        Number cid = jwt.getClaim("cid");
        return cid == null ? null : cid.longValue();
    }

    public static Long requireCustomerId(Jwt jwt) {
        Long cid = customerId(jwt);
        if (cid == null) {
            throw new IllegalStateException("Customer token must contain the cid claim");
        }
        return cid;
    }
}
