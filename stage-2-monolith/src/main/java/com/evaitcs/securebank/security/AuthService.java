package com.evaitcs.securebank.security;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * THE LOGIN USE CASE — one method that ties the security pieces together:
 *
 *   username ──> DbUserDetailsService (DB lookup)
 *   password ──> PasswordEncoder.matches (BCrypt compare — NEVER == on hashes;
 *                BCrypt embeds a random SALT inside each hash, so the same
 *                password hashes differently per user and per re-hash)
 *   success  ──> JwtService.issue (mint the token)
 *
 * WHY A SERVICE AND NOT Spring Security's formLogin()?
 * A stateless JSON API authenticates with username+password -> JWT, with no
 * session cookie anywhere. Handing the flow ourselves (instead of the
 * built-in form-login filter) makes every step visible — which is the point
 * of this stage. Stage 5 lifts this exact flow into a dedicated auth-service.
 */
@Service
public class AuthService {

    /** Returned to the controller; becomes the LoginResponse JSON body. */
    public record LoginResult(String token, String username, String role, Long customerId) {
    }

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserDetailsService userDetailsService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResult login(String username, String rawPassword) {
        SecureBankUser user = (SecureBankUser) userDetailsService.loadUserByUsername(username);
        // One generic failure for both "no such user" and "wrong password" —
        // see the enumeration-oracle note in DbUserDetailsService.
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        return new LoginResult(jwtService.issue(user), user.getUsername(),
                user.getRoleName(), user.getCustomerId());
    }
}
