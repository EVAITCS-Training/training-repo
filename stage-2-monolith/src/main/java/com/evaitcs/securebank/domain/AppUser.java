package com.evaitcs.securebank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * LOGIN IDENTITY — deliberately a SEPARATE table from Customer.
 *
 * WHY? "Someone who can log in" and "someone who owns accounts" are
 * different concepts that merely overlap:
 *   - a customer logs in AND owns accounts  (customer_id set)
 *   - a teller logs in but owns nothing     (customer_id NULL)
 * Merging them would mean nullable half-columns everywhere — the
 * normalization lesson from Week 3 applied to security.
 *
 * Table is named `app_user` because USER is a RESERVED WORD in PostgreSQL.
 * Hitting reserved words (user, order, group...) is a rite of passage;
 * the fix is a different table name or quoted identifiers (avoid those).
 *
 * CONNECTIONS:
 *  - security/DbUserDetailsService loads rows from here to authenticate logins.
 *  - security/JwtService copies id/username/role/customer.id into JWT claims.
 *  - Controllers read those claims back to know WHO is calling and enforce
 *    ownership ("you may only see YOUR accounts" — the OWASP "broken access
 *    control" lesson from Week 9).
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * SECURITY RULE: store ONLY a BCrypt hash, never the plaintext password.
     * BCrypt is salted + deliberately slow, so leaked hashes resist cracking.
     * The hash is produced by the PasswordEncoder bean in SecurityConfig and
     * verified by it during login. A 60-char column fits the BCrypt format.
     */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Null for staff users (teller/admin); set for customers. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    protected AppUser() {
    }

    public AppUser(String username, String passwordHash, Role role, Customer customer) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.customer = customer;
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

    public Customer getCustomer() {
        return customer;
    }
}
