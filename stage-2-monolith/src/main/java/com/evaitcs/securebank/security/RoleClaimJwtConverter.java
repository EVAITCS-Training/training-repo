package com.evaitcs.securebank.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;

/**
 * CLAIMS -> AUTHORITIES.
 *
 * A verified Jwt is just a bag of claims. Spring Security needs GrantedAuthorities
 * to evaluate hasRole(...)/@PreAuthorize. This converter is that bridge:
 *
 *   token claim  "role": "TELLER"   ->  authority  ROLE_TELLER
 *   token claim  "sub":  "alice"    ->  principal name
 *
 * It is plugged into the resource server in SecurityConfig
 * (.jwt(jwt -> jwt.jwtAuthenticationConverter(...))).
 *
 * Downstream, controllers receive the Jwt via @AuthenticationPrincipal and
 * read the "cid" claim for ownership checks (see web/JwtClaims).
 */
public class RoleClaimJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        String role = jwt.getClaimAsString("role");
        if (role != null && !role.isBlank()) {
            // hasRole('X') looks for ROLE_X — we add the prefix here once so
            // every check elsewhere stays clean.
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
