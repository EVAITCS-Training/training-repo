package com.evaitcs.securebank.security;

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
 * ISSUES signed JWTs (the "write" side of auth). The VERIFY side is the
 * JwtDecoder bean in SecurityConfig — same secret, opposite direction.
 *
 * JWT ANATOMY (what we produce below):
 *   header  {"alg":"HS256","typ":"JWT"}          — how it's signed
 *   payload {"sub":"alice","role":"CUSTOMER",    — the CLAIMS (statements)
 *            "cid":1,"iss":...,"iat":...,"exp":...}
 *   signature HMAC-SHA256(header.payload, secret) — proves WE minted it
 *
 * The three parts are base64url-encoded and joined with dots: eyJ... . eyJ... . sig
 * KEY INSIGHT: the payload is only SIGNED, NOT ENCRYPTED — anyone can read
 * it. That's fine for claims like role/cid, and forbidden for passwords.
 * Tampering is what the signature prevents: change one character of the
 * payload and verification fails.
 *
 * HS256 = symmetric: one shared secret signs AND verifies. Perfect for a
 * monolith (and for Stage 5, where services share the secret via the Config
 * Server). Asymmetric RS256 (private key signs, public key verifies) is what
 * you'd use when verifiers must NOT be able to mint tokens — discussed in
 * the Week 6 OAuth2 lesson.
 */
@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKeySpec secretKey;

    public JwtService(JwtProperties props) {
        this.props = props;
        byte[] keyBytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // Fail at STARTUP, not at first login. A short HMAC key is
            // brute-forceable — better to refuse to boot.
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /** Mint a token for an authenticated user. */
    public String issue(SecureBankUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(props.ttl());

        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(user.getUsername())          // "sub" — who this token identifies
                .issuer(props.issuer())               // "iss" — who minted it
                .issueTime(Date.from(now))            // "iat"
                .expirationTime(Date.from(expiry))    // "exp" — verified automatically on decode
                .jwtID(newRandomId())                 // "jti" — unique id, enables revocation lists
                .claim("role", user.getRoleName());   // custom claim -> ROLE_ authority (SecurityConfig)
        if (user.getCustomerId() != null) {
            claims.claim("cid", user.getCustomerId()); // custom claim -> ownership checks
        }

        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
                    claims.build());
            jwt.sign(new MACSigner(secretKey)); // HMAC-SHA256 signature
            return jwt.serialize();             // the dotted string the client stores
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to sign JWT", e);
        }
    }

    public Instant expiresAfter() {
        return Instant.now().plus(props.ttl());
    }

    private static String newRandomId() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes); // SecureRandom, NOT java.util.Random
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
