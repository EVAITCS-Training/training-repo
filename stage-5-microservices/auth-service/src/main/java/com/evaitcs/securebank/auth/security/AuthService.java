package com.evaitcs.securebank.auth.security;

import com.evaitcs.securebank.auth.domain.AppUser;
import com.evaitcs.securebank.auth.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * The login use case — the SAME three steps as stage-2's AuthService
 * (load user, BCrypt-compare, mint token), now as a standalone service.
 *
 * WHY SO SIMILAR? Decomposition moves code; it doesn't rewrite it. The
 * differences are the interesting part:
 *   - this service talks ONLY to authdb (no customer/account entities),
 *   - the token it mints is verified ELSEWHERE (shared secret via config),
 *   - being the single issuer means the secret's blast radius is managed:
 *     resource services hold the secret too (HS256 is symmetric) — a real
 *     estate would move to RS256 so verifiers CANNOT mint (Week 6 discussion).
 */
@Service
public class AuthService {

    public record LoginResult(String token, String username, String role, Long customerId) {
    }

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResult login(String username, String rawPassword) {
        AppUser user = users.findByUsername(username)
                // One generic failure for unknown user AND wrong password —
                // no username-enumeration oracle (same rule as stage 2).
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        return new LoginResult(jwtService.issue(user), user.getUsername(),
                user.getRole().name(), user.getCustomerId());
    }
}
