package com.evaitcs.evolution.app.era2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ERA 2 — a row of stage-1's `customer` table as an ENTITY. Copied from module 02
 * unchanged except its package: the entity shapes are the era's product, and era 2 of
 * the capstone reuses them as-is. (A regular class, not a record: Hibernate needs a
 * no-arg constructor and mutable fields — dirty checking updates MANAGED state in place.)
 */
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // stage-1's GENERATED ALWAYS AS IDENTITY column
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** The uq_customer_email UNIQUE constraint stays in the schema: Hibernate maps the column, the DB enforces the rule. */
    @Column(nullable = false)
    private String email;

    /** TIMESTAMP WITH TIME ZONE -> OffsetDateTime (stage-1 rule: never a bare LocalDateTime). */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /** LAZY: no SELECT runs until code touches this collection (module 02's N+1 lesson). */
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<Account> accounts = new ArrayList<>();

    /** JPA instantiates entities through this constructor; application code uses find()/persist(). */
    public Customer() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Account> getAccounts() {
        return accounts;
    }
}
