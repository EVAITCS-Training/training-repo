package com.evaitcs.securebank.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Typed binding for app.jwt.* — the SAME record shape as stage-2, but now
 * the VALUES come from the Config Server (config-repo/application.yml):
 * one secret shared by the issuer (here) and all verifiers (other services).
 * Centralized config means rotating the secret is a one-file change.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,   // HMAC-SHA256 key, >= 32 bytes (checked at startup)
        String issuer,
        Duration ttl
) {
}
