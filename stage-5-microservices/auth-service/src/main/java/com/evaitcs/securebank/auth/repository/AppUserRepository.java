package com.evaitcs.securebank.auth.repository;

import com.evaitcs.securebank.auth.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Credential lookup — the only query auth-service needs. */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}
