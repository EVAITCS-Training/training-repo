package com.evaitcs.securebank.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SECURITY CONFIGURATION — the wiring diagram of everything in this package.
 *
 * HOW A PROTECTED REQUEST IS PROCESSED (the filter chain):
 *
 *   Request: GET /api/accounts   Authorization: Bearer eyJhbGci...
 *      │
 *      ▼
 *   1. CORS filter ─────── browser preflight/origin checks (frontend on :5173)
 *   2. BearerTokenAuthenticationFilter (added by oauth2ResourceServer below)
 *      │   ├─ extracts the token from the Authorization header
 *      │   ├─ JwtDecoder bean VERIFIES signature + exp/iss claims
 *      │   └─ JwtAuthenticationConverter turns claims into authorities:
 *      │        claim role=CUSTOMER  ->  authority ROLE_CUSTOMER
 *      ▼        claim sub/cid        ->  principal (used by controllers)
 *   3. AuthorizationFilter ── matches the request against authorizeHttpRequests
 *      │                       rules below (+ @PreAuthorize on methods)
 *      ▼
 *   4. Controller runs — or 401 (bad/missing token) / 403 (wrong role) JSON.
 *
 * TWO DECISIONS THAT SHAPE EVERYTHING:
 *   - STATELESS: no HttpSession is ever created. The token IS the session.
 *     (Week 1: HTTP is stateless; Week 6: this is how you honor that.)
 *   - CSRF DISABLED: CSRF attacks ride on cookies the browser auto-sends.
 *     We use no cookies — the frontend must deliberately attach the Bearer
 *     header, which a cross-site page cannot do. Cookie-based apps MUST keep
 *     CSRF protection on.
 */
@Configuration
@EnableWebSecurity
// Turns on @PreAuthorize/@Secured on any bean method (used by LoanController
// for the teller-only approve/reject endpoints).
@EnableMethodSecurity
// Activates the JwtProperties record — binds app.jwt.* from application.yml.
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /**
     * The chain itself. Spring Security 7 requires the LAMBDA DSL — each
     * block configures one concern. Read top to bottom = request lifecycle.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())                       // stateless Bearer API — see class comment
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC: login, API docs, health probe, H2 console (dev only).
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // EVERYTHING else requires a valid token. Default-deny
                        // (anyRequest().authenticated()) is the safe posture:
                        // a NEW endpoint is protected until proven public —
                        // the opposite order leaks endpoints by forgetting.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        // Validates Bearer tokens using the JwtDecoder bean below,
                        // then maps claims -> authorities via our converter.
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new RoleClaimJwtConverter())))
                .headers(headers -> headers
                        // The H2 console renders inside frames; same-origin only.
                        .frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /**
     * TOKEN VERIFICATION (the "read" side; JwtService is the "write" side).
     * NimbusJwtDecoder.withSecretKey + HS256 = symmetric verification with the
     * same secret used for signing. It checks signature, exp (expiry), nbf and
     * iss (when configured) on EVERY request — cheap HMAC math, no DB hit.
     */
    @Bean
    JwtDecoder jwtDecoder(JwtProperties props) {
        SecretKeySpec key = new SecretKeySpec(
                props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * BCrypt: adaptive hash with a built-in random salt and a tunable cost
     * factor. Injected as the INTERFACE (PasswordEncoder) so switching
     * algorithms (Argon2 tomorrow) touches one bean, not every call site —
     * polymorphism doing real work (Week 4).
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS — lets the Stage 3 frontend (Vite dev server, http://localhost:5173)
     * call this API from browser JavaScript. Browsers block cross-origin XHR
     * unless the SERVER opts in via Access-Control-Allow-* headers.
     * allowCredentials stays false because we authenticate with Bearer
     * headers, not cookies.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:4173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
