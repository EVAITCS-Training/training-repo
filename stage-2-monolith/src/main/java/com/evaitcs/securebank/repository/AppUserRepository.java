package com.evaitcs.securebank.repository;

import com.evaitcs.securebank.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Credential lookups. Used at LOGIN time by security/DbUserDetailsService:
 * username -> row -> BCrypt hash comparison -> JWT issued.
 *
 * Note how few methods we need: JpaRepository already provides save/findById;
 * the login flow only needs "by username".
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}
