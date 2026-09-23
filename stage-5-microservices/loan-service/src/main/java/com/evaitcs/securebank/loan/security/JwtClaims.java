package com.evaitcs.securebank.loan.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

/** Claim helper — per-service copy (docs: account-service JwtClaims). */
public final class JwtClaims {

    private JwtClaims() {
    }

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

    public static Long requireCustomerId(Jwt jwt) {
        Long cid = customerId(jwt);
        if (cid == null) {
            throw new IllegalStateException("Customer token must contain the cid claim");
        }
        return cid;
    }
}
