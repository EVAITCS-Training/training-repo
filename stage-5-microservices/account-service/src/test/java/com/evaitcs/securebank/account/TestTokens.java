package com.evaitcs.securebank.account;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * TEST UTILITY — mints valid JWTs so API tests can authenticate WITHOUT
 * running auth-service. The secret matches src/test/resources/application.yml;
 * the claims match what auth-service produces (sub/role/cid), because the
 * TOKEN IS THE CONTRACT between services — a test that forges one is testing
 * the real interface, not a mock of it.
 *
 * (Copied into each service whose API tests need tokens — same duplication
 * trade-off as the security kit itself.)
 */
public final class TestTokens {

    public static final String SECRET = "test-secret-test-secret-test-secret-32bytes!";

    private TestTokens() {
    }

    public static String customer(String username, long customerId) {
        return sign(username, "CUSTOMER", customerId);
    }

    public static String staff(String username, String role) {
        return sign(username, role, null);
    }

    private static String sign(String subject, String role, Long customerId) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer("securebank-microservices")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                    .claim("role", role);
            if (customerId != null) {
                claims.claim("cid", customerId);
            }
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
                    claims.build());
            jwt.sign(new MACSigner(new SecretKeySpec(
                    SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
