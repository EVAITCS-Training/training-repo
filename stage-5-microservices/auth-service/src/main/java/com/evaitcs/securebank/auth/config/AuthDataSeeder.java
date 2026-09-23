package com.evaitcs.securebank.auth.config;

import com.evaitcs.securebank.auth.domain.AppUser;
import com.evaitcs.securebank.auth.domain.Role;
import com.evaitcs.securebank.auth.repository.AppUserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the same demo identities as stage-1/stage-2 — alice, bob (customers
 * 1 and 2 IN ACCOUNT-SERVICE's database; here only their ids are stored) and
 * the teller/admin staff users. Password: password123 (BCrypt-hashed with the
 * same encoder the login flow verifies against).
 *
 * CROSS-CONTEXT ID CONTRACT: customerIds 1/2 must match account-service's
 * seeded customers. Two services' seeders agreeing on ids is a demo
 * convenience; real systems propagate such facts via events or an identity
 * service. (Worth pointing out in class — this is distributed coupling
 * sneaking back in through the back door.)
 */
@Component
public class AuthDataSeeder implements ApplicationRunner {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthDataSeeder(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        String hash = passwordEncoder.encode("password123");
        users.save(new AppUser("alice", hash, Role.CUSTOMER, 1L));
        users.save(new AppUser("bob", hash, Role.CUSTOMER, 2L));
        users.save(new AppUser("teller", hash, Role.TELLER, null));
        users.save(new AppUser("admin", hash, Role.ADMIN, null));
    }
}
