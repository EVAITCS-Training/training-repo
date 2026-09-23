package com.evaitcs.securebank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * ENTITY LAYER — "domain" package.
 *
 * An @Entity is a Java class whose instances map to ROWS of a database table.
 * This class maps to the `customer` table you wrote by hand in stage-1
 * (stage-1-database/schema.sql). Compare the two: every column there is a
 * field here. That equivalence — SQL table <-> Java class — is the core idea
 * of an ORM (Object-Relational Mapping), and Hibernate is the ORM Spring
 * Data JPA uses under the hood.
 *
 * CONNECTIONS:
 *  - repository/CustomerRepository queries THIS class (Spring Data generates
 *    the SQL from method names).
 *  - service/AccountService creates Accounts linked to a Customer.
 *  - security/AppUser links a login identity to a Customer (a teller has
 *    login credentials but is NOT a customer — hence the separate table).
 */
@Entity
@Table(name = "customer")
public class Customer {

    /**
     * IDENTITY = "let the database generate the id" (SQL: GENERATED ALWAYS AS
     * IDENTITY in stage-1). The DB, not Java, owns uniqueness — that keeps
     * concurrent inserts safe.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** UNIQUE constraint enforced by the DB (see schema.sql) AND worth checking
     *  in the service layer for a friendly error message. */
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /**
     * ONE-TO-MANY: one customer owns many accounts. This is the Java side of
     * the FOREIGN KEY account.customer_id from stage-1.
     *
     * mappedBy = "customer" says: "the Account.customer field owns the
     * relationship (it holds the foreign key); this list is just the reverse
     * view". LAZY (the default for collections) means accounts are only
     * fetched when actually accessed — avoiding accidental extra SELECTs.
     */
    @OneToMany(mappedBy = "customer")
    private List<Account> accounts = new ArrayList<>();

    // JPA REQUIRES a no-arg constructor (Hibernate creates instances via
    // reflection). Protected so application code uses the real constructor.
    protected Customer() {
    }

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Getters only where needed — encapsulation (Week 4 OOP pillar).
    // No setters: entities are mutated through business methods, not by
    // whoever holds a reference. That protects invariants.
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Account> getAccounts() {
        return accounts;
    }
}
