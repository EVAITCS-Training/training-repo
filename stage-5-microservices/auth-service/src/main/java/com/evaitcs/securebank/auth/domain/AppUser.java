package com.evaitcs.securebank.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Login identity — the stage-2 AppUser entity, MIGRATED into its own context.
 *
 * THE MICROSERVICE DIFFERENCE: stage-2 had `@ManyToOne Customer customer`.
 * Here that's a plain `Long customerId` — the Customer entity lives in
 * ACCOUNT-service's database now, and cross-service object graphs don't
 * exist. Referencing another context's data BY ID is the rule; needing the
 * name/email too means either an API call or a denormalized copy — the
 * classic distributed-data trade-off (Week 7: eventual consistency).
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** BCrypt hash only — never plaintext (same rule as stage 2). */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Cross-context reference BY ID (account-service owns customers).
     *  Null for staff. No FK — different database! */
    @Column(name = "customer_id")
    private Long customerId;

    protected AppUser() {
    }

    public AppUser(String username, String passwordHash, Role role, Long customerId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.customerId = customerId;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Long getCustomerId() {
        return customerId;
    }
}
