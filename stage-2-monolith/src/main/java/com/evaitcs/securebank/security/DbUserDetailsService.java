package com.evaitcs.securebank.security;

import com.evaitcs.securebank.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security's standard "load a user by username" port.
 *
 * CONNECTION MAP (login flow, start to finish):
 *
 *   POST /api/auth/login {username, password}
 *        │
 *        ▼
 *   AuthController ──> AuthService.login(...)
 *                          │
 *                          ├─> DbUserDetailsService.loadUserByUsername(username)
 *                          │        └─> AppUserRepository (SELECT ... FROM app_user)
 *                          ├─> PasswordEncoder.matches(raw, hash)   [BCrypt check]
 *                          └─> JwtService.issue(user)               [signed token]
 *        ▼
 *   200 { token: "eyJhbGci..." }   → the frontend stores it and sends it as
 *                                     "Authorization: Bearer <token>" on every
 *                                     later request (HTTP is stateless!).
 *
 * Later requests never touch THIS class: the resource-server filter chain
 * validates the JWT signature instead (SecurityConfig). That asymmetry —
 * password once, token forever (until expiry) — is the essence of JWT auth.
 */
@Service
public class DbUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public DbUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.findByUsername(username)
                .map(SecureBankUser::new)
                // Deliberately vague message: "user not found" and "wrong
                // password" must be INDISTINGUISHABLE to the caller, or we
                // hand attackers a username-enumeration oracle.
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}
