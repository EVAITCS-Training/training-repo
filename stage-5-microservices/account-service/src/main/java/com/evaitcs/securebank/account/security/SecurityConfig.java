package com.evaitcs.securebank.account.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 * RESOURCE-SERVER SECURITY — the verifier half of the JWT contract.
 *
 * auth-service SIGNS tokens with app.jwt.secret (HS256); this service VERIFIES
 * with the SAME secret, delivered by the Config Server
 * (config-repo/application.yml). That shared value is why centralized config
 * matters: rotate the secret in one file, every service agrees again on
 * restart. (Production upgrade path: RS256 — private key only in auth-service,
 * public key everywhere else; verifiers then CANNOT mint tokens.)
 *
 * DUPLICATED ACROSS SERVICES: this kit (SecurityConfig + JwtClaims) is copied
 * into account-, payment-, loan- and notification-service almost verbatim.
 * The trade-off is real: a shared "security-commons" jar would DRY it up but
 * couple four deployables to one release train. Most estates start with
 * copies, extract a library once the pattern stabilizes, and keep it TINY.
 * For teaching, copies win: each service reads top-to-bottom with no
 * invisible dependencies.
 *
 * /internal/** IS PERMITTED HERE — on purpose. Authorization for those
 * endpoints is the NETWORK's job: the gateway has no /internal route, and
 * k8s NetworkPolicies restrict callers to payment-service (infra/k8s).
 * See AccountService's class comment for the honest discussion of this
 * tension and the mTLS endgame.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())          // stateless Bearer API (stage-2 comment)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/internal/**").permitAll()  // network-protected, see above
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(new RoleClaimConverter())));
        return http.build();
    }

    /** Verifies HS256 signatures with the shared secret (see class comment). */
    @Bean
    JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Token claim "role" -> ROLE_<role> authority (stage-2 RoleClaimJwtConverter). */
    static class RoleClaimConverter implements Converter<Jwt, AbstractAuthenticationToken> {
        @Override
        public AbstractAuthenticationToken convert(Jwt jwt) {
            String role = jwt.getClaimAsString("role");
            Collection<GrantedAuthority> authorities = role == null
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + role));
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        }
    }
}
