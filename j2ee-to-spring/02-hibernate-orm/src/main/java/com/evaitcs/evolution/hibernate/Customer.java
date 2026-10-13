package com.evaitcs.evolution.hibernate;

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
 * A row of stage-1's `customer` table, as a plain Java class — an ENTITY.
 * (README sections "The ORM bargain" and "Entity lifecycle: the four states".)
 *
 * WHY a regular class and not a record (the README's Java 21 corner): Hibernate builds
 * entities by reflection — it needs a no-arg constructor and mutable fields, because a
 * MANAGED entity's state is updated IN PLACE (that is what dirty checking diffs,
 * DirtyCheckingDemo). Records are final and immutable, so JPA does not accept them as
 * entities. Records still shine one layer up: projections (see AccountSummary).
 *
 * The annotations are jakarta.persistence.* — the JPA standard Hibernate implements.
 * In 2006 (JPA 1.0) they were javax.persistence.*: identical code, renamed package
 * (the Jakarta EE split). Module 01 ran on jakarta.servlet for the same reason.
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

    /** TIMESTAMP WITH TIME ZONE -> OffsetDateTime: the mapping Hibernate does natively since 6 (stage-1 rule: never a bare LocalDateTime). */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /**
     * The lazy promise at the heart of LazyAndNPlusOneDemo: Hibernate puts a
     * proxy-backed collection here and runs NO SELECT until code touches it.
     * mappedBy = "customer" says the FK lives on the ACCOUNT side (customer_id),
     * so Account owns the relationship and this side merely reads it.
     */
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
