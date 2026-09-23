package com.evaitcs.securebank.auth.security;

import com.evaitcs.securebank.auth.domain.AppUser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;

/**
 * MINTS the system's JWTs — identical algorithm to stage-2's JwtService
 * (HS256 + sub/role/cid claims), because the TOKEN FORMAT is the contract
 * every other service verifies against. Deep explanation of JWT anatomy
 * lives in stage-2/security/JwtService.java; this copy exists because
 * issuing tokens is auth-service's bounded-context job now.
 */
@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKeySpec secretKey;

    public JwtService(JwtProperties props) {
        this.props = props;
        byte[] keyBytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String issue(AppUser user) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer(props.issuer())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(props.ttl())))
                .jwtID(randomHex())
                .claim("role", user.getRole().name());
        if (user.getCustomerId() != null) {
            claims.claim("cid", user.getCustomerId()); // customers only; staff have none
        }
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
                    claims.build());
            jwt.sign(new MACSigner(secretKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to sign JWT", e);
        }
    }

    public Instant expiresAfter() {
        return Instant.now().plus(props.ttl());
    }

    private static String randomHex() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
