package com.evaitcs.securebank.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * TYPED CONFIGURATION — the modern alternative to scattering
 * @Value("${app.jwt.secret}") everywhere.
 *
 * Binds everything under `app.jwt:` in application.yml onto an immutable
 * record. Benefits: one source of truth, constructor validation, IDE
 * autocomplete, and in Stage 5 the SAME shape is served by the Config Server
 * to five microservices (centralized configuration, Week 7).
 *
 * Registered via @EnableConfigurationProperties(JwtProperties.class) in
 * SecurityConfig — look there to see the wiring.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        /** HMAC-SHA256 signing secret. MUST be >= 32 bytes (256 bits) or
         *  the JWS library rejects it. Override via JWT_SECRET env var —
         *  NEVER commit a production secret to git (Week 1 .gitignore lesson). */
        String secret,
        /** "iss" claim — who minted the token. */
        String issuer,
        /** Token lifetime, e.g. PT1H = ISO-8601 for 1 hour. Short lifetimes
         *  limit the damage of a stolen token. */
        Duration ttl
) {
}
